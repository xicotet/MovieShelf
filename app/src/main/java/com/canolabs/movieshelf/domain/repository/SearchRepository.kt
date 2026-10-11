package com.canolabs.movieshelf.domain.repository

import com.canolabs.movieshelf.domain.model.CachedSearch
import com.canolabs.movieshelf.domain.model.Movie
import com.canolabs.movieshelf.domain.model.RecentSearch
import com.canolabs.movieshelf.domain.model.RefreshResult
import com.canolabs.movieshelf.domain.model.SearchQuery
import kotlinx.coroutines.flow.Flow

interface SearchRepository {

    /** Observes the persisted results for exactly [query]. Never emits another query's results. */
    fun observeResults(query: SearchQuery): Flow<CachedSearch>

    /**
     * Records [query] as a recent search, fetches it from the API and, on success, replaces the
     * query's cached result set before returning. A failure leaves any existing cache untouched.
     */
    suspend fun refresh(query: SearchQuery): RefreshResult

    /**
     * Fetches the next TMDB page of [query]'s cached results and appends it in one transaction
     * before returning. A failure leaves every stored page untouched. Does nothing (and succeeds)
     * when no further page exists or the stored results changed meanwhile (e.g. a page-1 refresh).
     */
    suspend fun loadNextPage(query: SearchQuery): RefreshResult

    /** Most recently submitted queries first, without duplicates. */
    fun observeRecentSearches(limit: Int): Flow<List<RecentSearch>>

    /** Records [query] as a recent search without contacting the API (e.g. a submission made offline). */
    suspend fun recordSearch(query: SearchQuery)

    /**
     * Movies already stored locally whose title contains [query], case-insensitively. These are local
     * matches only: they are never written to the query's result cache nor presented as TMDB results.
     */
    fun observeLocalTitleMatches(query: SearchQuery, limit: Int): Flow<List<Movie>>
}
