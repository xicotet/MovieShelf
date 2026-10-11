package com.canolabs.movieshelf.domain.repository

import com.canolabs.movieshelf.domain.model.CachedSearch
import com.canolabs.movieshelf.domain.model.Movie
import com.canolabs.movieshelf.domain.model.RecentSearch
import com.canolabs.movieshelf.domain.model.RefreshResult
import com.canolabs.movieshelf.domain.model.ResultsRequest
import com.canolabs.movieshelf.domain.model.SearchQuery
import kotlinx.coroutines.flow.Flow

interface SearchRepository {

    /** Observes the persisted results for exactly [request]. Never emits another request's results. */
    fun observeResults(request: ResultsRequest): Flow<CachedSearch>

    /**
     * Fetches page 1 of [request] and, on success, replaces its cached result set before returning.
     * A typed search is also recorded as a recent search. A failure leaves any existing cache untouched.
     */
    suspend fun refresh(request: ResultsRequest): RefreshResult

    /**
     * Fetches the next TMDB page of [request]'s cached results and appends it in one transaction
     * before returning. A failure leaves every stored page untouched. Does nothing (and succeeds)
     * when no further page exists or the stored results changed meanwhile (e.g. a page-1 refresh).
     */
    suspend fun loadNextPage(request: ResultsRequest): RefreshResult

    /** Most recently submitted queries first, without duplicates or removed entries. */
    fun observeRecentSearches(limit: Int): Flow<List<RecentSearch>>

    /**
     * Removes [query] from recent searches only: its cached results and the saved movies are kept,
     * and searching for it again brings it back.
     */
    suspend fun removeRecentSearch(query: SearchQuery)

    /** Records [query] as a recent search without contacting the API (e.g. a submission made offline). */
    suspend fun recordSearch(query: SearchQuery)

    /**
     * Movies already stored locally whose title contains [query], case-insensitively. These are local
     * matches only: they are never written to the query's result cache nor presented as TMDB results.
     */
    fun observeLocalTitleMatches(query: SearchQuery, limit: Int): Flow<List<Movie>>
}
