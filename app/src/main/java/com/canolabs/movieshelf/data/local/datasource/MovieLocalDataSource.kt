package com.canolabs.movieshelf.data.local.datasource

import androidx.room.withTransaction
import com.canolabs.movieshelf.data.local.dao.MovieDao
import com.canolabs.movieshelf.data.local.dao.QueryPagination
import com.canolabs.movieshelf.data.local.dao.SearchDao
import com.canolabs.movieshelf.data.local.database.MovieShelfDatabase
import com.canolabs.movieshelf.data.local.entity.MovieEntity
import com.canolabs.movieshelf.data.local.entity.SearchQueryEntity
import com.canolabs.movieshelf.data.local.entity.SearchResultCrossRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Escapes LIKE wildcards so input such as `50%` or `a_b` is matched literally (escape char `\`). */
internal fun escapeLikePattern(text: String): String =
    text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

/** Persisted state of one query: `null` when the query has never been recorded. */
data class CachedQueryResults(
    val lastRefreshedAt: Long?,
    /** Ordered by result position. */
    val movies: List<MovieEntity>,
    val loadedPages: Int = 0,
    val totalPages: Int? = null,
)

interface MovieLocalDataSource {
    fun observeSearchResults(queryKey: String): Flow<CachedQueryResults?>
    fun observeRecentSearches(limit: Int): Flow<List<SearchQueryEntity>>
    fun observeMovie(movieId: Long): Flow<MovieEntity?>

    /** Case-insensitive title substring search over saved movies; empty for blank input. */
    fun observeTitleMatches(text: String, limit: Int): Flow<List<MovieEntity>>

    suspend fun recordSearch(queryKey: String, displayQuery: String, searchedAt: Long)

    /**
     * Atomically replaces [queryKey]'s result set with TMDB page 1 (possibly empty) and resets its
     * pagination to one loaded page of [totalPages].
     */
    suspend fun replaceSearchResults(
        queryKey: String,
        displayQuery: String,
        movies: List<MovieEntity>,
        refreshedAt: Long,
        totalPages: Int,
    )

    /** Pagination of [queryKey], or `null` if the query was never recorded. */
    suspend fun pagination(queryKey: String): QueryPagination?

    /**
     * Atomically saves [movies] to the catalogue and appends them as page [page] of [queryKey].
     * Returns `false` (results untouched, movies still saved) unless exactly `page - 1` pages are stored.
     */
    suspend fun appendSearchResultsPage(queryKey: String, page: Int, movies: List<MovieEntity>, totalPages: Int): Boolean

    suspend fun saveMovieDetails(movie: MovieEntity)
}

class MovieLocalDataSourceImpl(
    private val database: MovieShelfDatabase,
    private val searchDao: SearchDao,
    private val movieDao: MovieDao,
) : MovieLocalDataSource {

    override fun observeSearchResults(queryKey: String): Flow<CachedQueryResults?> =
        searchDao.observeResultRows(queryKey)
            .map { rows ->
                if (rows.isEmpty()) {
                    null
                } else {
                    CachedQueryResults(
                        lastRefreshedAt = rows.first().lastRefreshedAt,
                        movies = rows.mapNotNull { it.movie },
                        loadedPages = rows.first().loadedPages,
                        totalPages = rows.first().totalPages,
                    )
                }
            }
            .distinctUntilChanged()

    override fun observeRecentSearches(limit: Int): Flow<List<SearchQueryEntity>> =
        searchDao.observeRecent(limit)

    override fun observeMovie(movieId: Long): Flow<MovieEntity?> =
        movieDao.observeMovie(movieId).distinctUntilChanged()

    override fun observeTitleMatches(text: String, limit: Int): Flow<List<MovieEntity>> {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || limit <= 0) return flowOf(emptyList())
        return movieDao.observeTitleMatches(escapeLikePattern(trimmed), limit).distinctUntilChanged()
    }

    override suspend fun recordSearch(queryKey: String, displayQuery: String, searchedAt: Long) {
        searchDao.recordSearch(queryKey, displayQuery, searchedAt)
    }

    override suspend fun replaceSearchResults(
        queryKey: String,
        displayQuery: String,
        movies: List<MovieEntity>,
        refreshedAt: Long,
        totalPages: Int,
    ) {
        database.withTransaction {
            // The query row normally exists already (recorded on submit). Only create it when missing,
            // without touching its recency, so a late response never reorders recent searches.
            searchDao.insertIfAbsent(SearchQueryEntity(queryKey, displayQuery, refreshedAt, lastRefreshedAt = null))
            movieDao.upsertSummaries(movies)
            searchDao.replaceResults(
                queryKey = queryKey,
                results = movies.mapIndexed { index, movie -> SearchResultCrossRef(queryKey, movie.id, index) },
                refreshedAt = refreshedAt,
                totalPages = totalPages,
            )
            // Movies are never deleted here: they stay in the local catalogue for offline title
            // search even when no cached query references them anymore.
        }
    }

    override suspend fun pagination(queryKey: String): QueryPagination? = searchDao.pagination(queryKey)

    override suspend fun appendSearchResultsPage(
        queryKey: String,
        page: Int,
        movies: List<MovieEntity>,
        totalPages: Int,
    ): Boolean = database.withTransaction {
        // Movies join the catalogue even if the page itself turns out to be stale.
        movieDao.upsertSummaries(movies)
        searchDao.appendPage(queryKey, page, movies.map { it.id }, totalPages)
    }

    override suspend fun saveMovieDetails(movie: MovieEntity) {
        movieDao.upsertDetails(movie)
    }
}
