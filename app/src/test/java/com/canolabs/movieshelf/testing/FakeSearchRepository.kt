package com.canolabs.movieshelf.testing

import com.canolabs.movieshelf.domain.model.CachedSearch
import com.canolabs.movieshelf.domain.model.Movie
import com.canolabs.movieshelf.domain.model.RecentSearch
import com.canolabs.movieshelf.domain.model.RefreshResult
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

    val refreshCalls = mutableListOf<SearchQuery>()
    val recordedSearches = mutableListOf<SearchQuery>()
    private val localMatches = MutableStateFlow<Map<String, List<Movie>>>(emptyMap())

    /** Saved movies matching [query]'s title, as the local catalogue would return them. */
    fun setLocalMatches(query: SearchQuery, movies: List<Movie>) {
        localMatches.update { it + (query.key to movies) }
    }

    /**
     * Default: succeed with an empty result, persisted before returning like the real repository.
     * Tests replace it to script outcomes.
     */
    var onRefresh: suspend (SearchQuery) -> RefreshResult = { q ->
        setCached(q, emptyList())
        RefreshResult.Success
    }

    fun setCached(
        query: SearchQuery,
        movies: List<Movie>,
        refreshedAt: Long = 1L,
        loadedPages: Int = 1,
        totalPages: Int? = 1,
    ) {
        cache.update { it + (query.key to CachedSearch(query, movies, refreshedAt, loadedPages, totalPages)) }
    }

    /** Current cached snapshot, for scripting next-page responses. */
    fun cached(query: SearchQuery): CachedSearch? = cache.value[query.key]

    val nextPageCalls = mutableListOf<SearchQuery>()

    /** Default: append nothing and succeed. Tests replace it to script outcomes. */
    var onLoadNextPage: suspend (SearchQuery) -> RefreshResult = { RefreshResult.Success }

    fun setRecent(vararg searches: RecentSearch) {
        recent.value = searches.toList()
    }

    override fun observeResults(query: SearchQuery): Flow<CachedSearch> = cache
        .map { it[query.key] ?: CachedSearch(query, emptyList(), lastRefreshedAt = null) }
        .distinctUntilChanged()

    override suspend fun refresh(query: SearchQuery): RefreshResult {
        refreshCalls += query
        return onRefresh(query)
    }

    override suspend fun loadNextPage(query: SearchQuery): RefreshResult {
        nextPageCalls += query
        return onLoadNextPage(query)
    }

    override fun observeRecentSearches(limit: Int): Flow<List<RecentSearch>> = recent.map { it.take(limit) }

    override suspend fun recordSearch(query: SearchQuery) {
        recordedSearches += query
    }

    override fun observeLocalTitleMatches(query: SearchQuery, limit: Int): Flow<List<Movie>> =
        localMatches.map { it[query.key].orEmpty().take(limit) }.distinctUntilChanged()
}
