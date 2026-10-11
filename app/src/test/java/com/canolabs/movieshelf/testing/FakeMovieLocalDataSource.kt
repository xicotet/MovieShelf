package com.canolabs.movieshelf.testing

import com.canolabs.movieshelf.data.local.datasource.CachedQueryResults
import com.canolabs.movieshelf.data.local.dao.QueryPagination
import com.canolabs.movieshelf.data.local.datasource.MovieLocalDataSource
import com.canolabs.movieshelf.data.local.entity.MovieEntity
import com.canolabs.movieshelf.data.local.entity.SearchQueryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory stand-in for the Room-backed local data source, with the same observable contract:
 * per-query result membership with positions, one shared movie table, and atomic replacement.
 */
class FakeMovieLocalDataSource : MovieLocalDataSource {

    private data class Db(
        val movies: Map<Long, MovieEntity> = emptyMap(),
        val queries: Map<String, SearchQueryEntity> = emptyMap(),
        /** queryKey -> ordered movie IDs. */
        val results: Map<String, List<Long>> = emptyMap(),
    )

    private val db = MutableStateFlow(Db())

    /** Ordered log of write operations, used to assert persistence ordering. */
    val writes = mutableListOf<String>()

    override fun observeSearchResults(queryKey: String): Flow<CachedQueryResults?> = db
        .map { state ->
            state.queries[queryKey]?.let { q ->
                CachedQueryResults(
                    lastRefreshedAt = q.lastRefreshedAt,
                    movies = state.results[queryKey].orEmpty().map { state.movies.getValue(it) },
                    loadedPages = q.loadedPages,
                    totalPages = q.totalPages,
                )
            }
        }
        .distinctUntilChanged()

    override fun observeRecentSearches(limit: Int): Flow<List<SearchQueryEntity>> = db
        .map { state -> state.queries.values.sortedByDescending { it.lastSearchedAt }.take(limit) }
        .distinctUntilChanged()

    override fun observeMovie(movieId: Long): Flow<MovieEntity?> =
        db.map { it.movies[movieId] }.distinctUntilChanged()

    /** Mirrors the DAO: case-insensitive substring, most-voted first, then title and ID. */
    override fun observeTitleMatches(text: String, limit: Int): Flow<List<MovieEntity>> = db
        .map { state ->
            val needle = text.trim().lowercase()
            if (needle.isEmpty()) {
                emptyList()
            } else {
                state.movies.values
                    .filter { it.title.lowercase().contains(needle) }
                    .sortedWith(
                        compareByDescending<MovieEntity> { it.voteCount ?: -1 }
                            .thenBy { it.title.lowercase() }
                            .thenBy { it.id },
                    )
                    .take(limit)
            }
        }
        .distinctUntilChanged()

    override suspend fun recordSearch(queryKey: String, displayQuery: String, searchedAt: Long) {
        writes += "recordSearch:$queryKey"
        db.update { state ->
            val existing = state.queries[queryKey]
            val updated = existing?.copy(displayQuery = displayQuery, lastSearchedAt = searchedAt)
                ?: SearchQueryEntity(queryKey, displayQuery, searchedAt, lastRefreshedAt = null)
            state.copy(queries = state.queries + (queryKey to updated))
        }
    }

    override suspend fun replaceSearchResults(
        queryKey: String,
        displayQuery: String,
        movies: List<MovieEntity>,
        refreshedAt: Long,
        totalPages: Int,
    ) {
        writes += "replaceSearchResults:$queryKey"
        db.update { state ->
            val query = (state.queries[queryKey] ?: SearchQueryEntity(queryKey, displayQuery, refreshedAt, null))
                .copy(lastRefreshedAt = refreshedAt, loadedPages = 1, totalPages = totalPages)
            // Movies are never pruned: they stay in the catalogue, as with the real DAO.
            state.copy(
                movies = state.movies.merge(movies),
                queries = state.queries + (queryKey to query),
                results = state.results + (queryKey to movies.map { it.id }.distinct()),
            )
        }
    }

    override suspend fun pagination(queryKey: String): QueryPagination? =
        db.value.queries[queryKey]?.let { QueryPagination(it.loadedPages, it.totalPages) }

    /** Mirrors SearchDao.appendPage: applied only when exactly `page - 1` pages are stored. */
    override suspend fun appendSearchResultsPage(
        queryKey: String,
        page: Int,
        movies: List<MovieEntity>,
        totalPages: Int,
    ): Boolean {
        var applied = false
        db.update { state ->
            val withMovies = state.copy(movies = state.movies.merge(movies))
            val query = state.queries[queryKey]
            if (query == null || page < 2 || query.loadedPages != page - 1) {
                applied = false
                return@update withMovies
            }
            applied = true
            val existing = state.results[queryKey].orEmpty()
            val appended = existing + movies.map { it.id }.distinct().filterNot { it in existing }
            withMovies.copy(
                queries = state.queries + (queryKey to query.copy(loadedPages = page, totalPages = totalPages)),
                results = state.results + (queryKey to appended),
            )
        }
        writes += "appendSearchResultsPage:$queryKey:$page:${if (applied) "applied" else "rejected"}"
        return applied
    }

    /** Keeps detail columns of already cached movies, as the DAO's partial update does. */
    private fun Map<Long, MovieEntity>.merge(incoming: List<MovieEntity>): Map<Long, MovieEntity> =
        this + incoming.associate { movie ->
            val previous = this[movie.id]
            movie.id to (
                previous?.copy(
                    title = movie.title,
                    overview = movie.overview,
                    releaseDate = movie.releaseDate,
                    posterPath = movie.posterPath,
                    backdropPath = movie.backdropPath,
                    voteAverage = movie.voteAverage,
                    voteCount = movie.voteCount,
                    updatedAt = movie.updatedAt,
                ) ?: movie
                )
        }

    override suspend fun saveMovieDetails(movie: MovieEntity) {
        writes += "saveMovieDetails:${movie.id}"
        db.update { it.copy(movies = it.movies + (movie.id to movie)) }
    }

    fun movie(id: Long): MovieEntity? = db.value.movies[id]
}
