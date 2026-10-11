package com.canolabs.movieshelf.data.local.datasource

import androidx.room.withTransaction
import com.canolabs.movieshelf.data.local.dao.MovieDao
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
)

interface MovieLocalDataSource {
    fun observeSearchResults(queryKey: String): Flow<CachedQueryResults?>
    fun observeRecentSearches(limit: Int): Flow<List<SearchQueryEntity>>
    fun observeMovie(movieId: Long): Flow<MovieEntity?>

    /** Case-insensitive title substring search over saved movies; empty for blank input. */
    fun observeTitleMatches(text: String, limit: Int): Flow<List<MovieEntity>>

    suspend fun recordSearch(queryKey: String, displayQuery: String, searchedAt: Long)

    /** Atomically replaces [queryKey]'s result set (possibly with an empty one). */
    suspend fun replaceSearchResults(
        queryKey: String,
        displayQuery: String,
        movies: List<MovieEntity>,
        refreshedAt: Long,
    )

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
            )
            // Only movies that no query references anymore are removed.
            movieDao.deleteUnreferenced()
        }
    }

    override suspend fun saveMovieDetails(movie: MovieEntity) {
        movieDao.upsertDetails(movie)
    }
}
