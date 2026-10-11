package com.canolabs.movieshelf.data.repository

import android.util.Log
import com.canolabs.movieshelf.data.local.datasource.MovieLocalDataSource
import com.canolabs.movieshelf.data.mapper.toDomain
import com.canolabs.movieshelf.data.mapper.toEntities
import com.canolabs.movieshelf.data.remote.datasource.MovieRemoteDataSource
import com.canolabs.movieshelf.data.remote.datasource.RemoteResult
import com.canolabs.movieshelf.data.remote.dto.SearchMoviesResponseDto
import com.canolabs.movieshelf.domain.model.CachedSearch
import com.canolabs.movieshelf.domain.model.Movie
import com.canolabs.movieshelf.domain.model.RecentSearch
import com.canolabs.movieshelf.domain.model.RefreshResult
import com.canolabs.movieshelf.domain.model.SearchQuery
import com.canolabs.movieshelf.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Offline-first search: Room is the single source of truth for displayed results.
 *
 * The network is only used through [refresh], a one-shot call whose successful response is written
 * to Room in one transaction before the call returns; observers then receive it from the database.
 * A failed refresh never touches the cache.
 */
class SearchRepositoryImpl(
    private val local: MovieLocalDataSource,
    private val remote: MovieRemoteDataSource,
    private val clock: () -> Long = System::currentTimeMillis,
) : SearchRepository {

    override fun observeResults(query: SearchQuery): Flow<CachedSearch> =
        local.observeSearchResults(query.key).map { it.toDomain(query) }

    override suspend fun refresh(query: SearchQuery): RefreshResult {
        local.recordSearch(query.key, query.text, searchedAt = clock())
        return when (val response = remote.searchMovies(query.text, page = 1)) {
            is RemoteResult.Success -> {
                val refreshedAt = clock()
                val totalPages = response.data.cappedTotalPages()
                local.replaceSearchResults(
                    queryKey = query.key,
                    displayQuery = query.text,
                    movies = response.data.results.toEntities(updatedAt = refreshedAt),
                    refreshedAt = refreshedAt,
                    totalPages = totalPages,
                )
                Log.d(TAG, "\"${query.text}\": persisted page 1 of $totalPages (${response.data.results.size} results) to Room")
                RefreshResult.Success
            }
            is RemoteResult.Failure -> RefreshResult.Failure(response.error)
        }
    }

    override suspend fun loadNextPage(query: SearchQuery): RefreshResult {
        // Only TMDB's own metadata says whether another page exists; never inferred from page size.
        val pagination = local.pagination(query.key)
        val totalPages = pagination?.totalPages
        if (pagination == null || pagination.loadedPages < 1 || totalPages == null ||
            pagination.loadedPages >= totalPages
        ) {
            return RefreshResult.Success // Nothing cached to extend, or no further page.
        }
        val page = pagination.loadedPages + 1
        if (page > MAX_TMDB_PAGE) return RefreshResult.Success
        return when (val response = remote.searchMovies(query.text, page)) {
            is RemoteResult.Success -> {
                val applied = local.appendSearchResultsPage(
                    queryKey = query.key,
                    page = page,
                    movies = response.data.results.toEntities(updatedAt = clock()),
                    totalPages = response.data.cappedTotalPages(),
                )
                Log.d(
                    TAG,
                    if (applied) {
                        "\"${query.text}\": appended page $page (${response.data.results.size} results) to Room"
                    } else {
                        "\"${query.text}\": page $page discarded, stored results changed meanwhile"
                    },
                )
                RefreshResult.Success
            }
            is RemoteResult.Failure -> RefreshResult.Failure(response.error)
        }
    }

    /** TMDB's `total_pages`, capped to the highest page TMDB serves; never below the page received. */
    private fun SearchMoviesResponseDto.cappedTotalPages(): Int =
        (totalPages ?: page).coerceAtMost(MAX_TMDB_PAGE).coerceAtLeast(if (results.isEmpty()) 0 else page)

    override fun observeRecentSearches(limit: Int): Flow<List<RecentSearch>> =
        local.observeRecentSearches(limit).map { rows -> rows.mapNotNull { it.toDomain() } }

    override suspend fun recordSearch(query: SearchQuery) {
        local.recordSearch(query.key, query.text, searchedAt = clock())
    }

    /** Read-only: local matches are never written to the query's result cache. */
    override fun observeLocalTitleMatches(query: SearchQuery, limit: Int): Flow<List<Movie>> =
        local.observeTitleMatches(query.text, limit).map { rows -> rows.map { it.toDomain() } }

    companion object {
        private const val TAG = "SearchRepositoryImpl"

        /** TMDB rejects `page` values above 500. */
        private const val MAX_TMDB_PAGE = 500
    }
}
