package com.canolabs.movieshelf.testing

import com.canolabs.movieshelf.domain.model.CachedSearch
import com.canolabs.movieshelf.domain.model.Movie
import com.canolabs.movieshelf.domain.model.RecentSearch
import com.canolabs.movieshelf.domain.model.RefreshResult
import com.canolabs.movieshelf.domain.model.ResultsRequest
import com.canolabs.movieshelf.domain.model.SearchQuery
import com.canolabs.movieshelf.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Repository double for ViewModel tests. Cached content and refresh behaviour are scripted per test. */
class FakeSearchRepository : SearchRepository {

    private val cache = MutableStateFlow<Map<String, CachedSearch>>(emptyMap())
    private val recent = MutableStateFlow<List<RecentSearch>>(emptyList())

    val refreshCalls = mutableListOf<ResultsRequest>()
    val recordedSearches = mutableListOf<SearchQuery>()
    val removedRecentSearches = mutableListOf<SearchQuery>()
    private val localMatches = MutableStateFlow<Map<String, List<Movie>>>(emptyMap())

    /** Saved movies matching [query]'s title, as the local catalogue would return them. */
    fun setLocalMatches(query: SearchQuery, movies: List<Movie>) {
        localMatches.update { it + (query.key to movies) }
    }

    /**
     * Default: succeed with an empty result, persisted before returning like the real repository.
     * Tests replace it to script outcomes.
     */
    var onRefresh: suspend (ResultsRequest) -> RefreshResult = { request ->
        setCached(request, emptyList())
        RefreshResult.Success
    }

    fun setCached(
        request: ResultsRequest,
        movies: List<Movie>,
        refreshedAt: Long = 1L,
        loadedPages: Int = 1,
        totalPages: Int? = 1,
    ) {
        cache.update { it + (request.cacheKey to CachedSearch(request, movies, refreshedAt, loadedPages, totalPages)) }
    }

    fun setCached(
        query: SearchQuery,
        movies: List<Movie>,
        refreshedAt: Long = 1L,
        loadedPages: Int = 1,
        totalPages: Int? = 1,
    ) = setCached(ResultsRequest.Search(query), movies, refreshedAt, loadedPages, totalPages)

    val nextPageCalls = mutableListOf<ResultsRequest>()

    /** Default: append nothing and succeed. Tests replace it to script outcomes. */
    var onLoadNextPage: suspend (ResultsRequest) -> RefreshResult = { RefreshResult.Success }

    fun setRecent(vararg searches: RecentSearch) {
        recent.value = searches.toList()
    }

    override fun observeResults(request: ResultsRequest): Flow<CachedSearch> = cache
        .map { it[request.cacheKey] ?: CachedSearch(request, emptyList(), lastRefreshedAt = null) }
        .distinctUntilChanged()

    override suspend fun refresh(request: ResultsRequest): RefreshResult {
        refreshCalls += request
        return onRefresh(request)
    }

    override suspend fun loadNextPage(request: ResultsRequest): RefreshResult {
        nextPageCalls += request
        return onLoadNextPage(request)
    }

    override fun observeRecentSearches(limit: Int): Flow<List<RecentSearch>> = recent.map { it.take(limit) }

    override suspend fun removeRecentSearch(query: SearchQuery) {
        removedRecentSearches += query
        recent.update { list -> list.filterNot { it.query == query } }
    }

    override suspend fun recordSearch(query: SearchQuery) {
        recordedSearches += query
    }

    override fun observeLocalTitleMatches(query: SearchQuery, limit: Int): Flow<List<Movie>> =
        localMatches.map { it[query.key].orEmpty().take(limit) }.distinctUntilChanged()
}
