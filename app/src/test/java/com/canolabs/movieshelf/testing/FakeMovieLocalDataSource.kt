package com.canolabs.movieshelf.testing

import com.canolabs.movieshelf.data.local.datasource.CachedQueryResults
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
    ) {
        writes += "replaceSearchResults:$queryKey"
        db.update { state ->
            val query = (state.queries[queryKey] ?: SearchQueryEntity(queryKey, displayQuery, refreshedAt, null))
                .copy(lastRefreshedAt = refreshedAt)
            // Keep detail columns of already cached movies, as the DAO's partial update does.
            val mergedMovies = state.movies + movies.associate { incoming ->
                val previous = state.movies[incoming.id]
                incoming.id to (
                    previous?.copy(
                        title = incoming.title,
                        overview = incoming.overview,
                        releaseDate = incoming.releaseDate,
                        posterPath = incoming.posterPath,
                        backdropPath = incoming.backdropPath,
                        voteAverage = incoming.voteAverage,
                        voteCount = incoming.voteCount,
                        updatedAt = incoming.updatedAt,
                    ) ?: incoming
                    )
            }
            val results = state.results + (queryKey to movies.map { it.id })
            val referenced = results.values.flatten().toSet()
            state.copy(
                movies = mergedMovies.filterKeys { it in referenced },
                queries = state.queries + (queryKey to query),
                results = results,
            )
        }
    }

    override suspend fun saveMovieDetails(movie: MovieEntity) {
        writes += "saveMovieDetails:${movie.id}"
        db.update { it.copy(movies = it.movies + (movie.id to movie)) }
    }

    fun movie(id: Long): MovieEntity? = db.value.movies[id]
}
