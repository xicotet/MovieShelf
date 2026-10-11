package com.canolabs.movieshelf.domain.model

/**
 * The locally persisted state of one query's result set.
 *
 * `movies` is ordered as returned by the API. [lastRefreshedAt] is `null` when the query has never been
 * fetched successfully, which distinguishes a cache miss from a cached, genuinely empty result.
 */
data class CachedSearch(
    val query: SearchQuery,
    val movies: List<Movie>,
    val lastRefreshedAt: Long?,
    /** TMDB pages stored for the query. */
    val loadedPages: Int = 0,
    /** TMDB's total page count for the query, or `null` when unknown. */
    val totalPages: Int? = null,
) {
    val hasCachedResult: Boolean get() = lastRefreshedAt != null

    /** Another TMDB page exists according to TMDB's own metadata (never inferred from page size). */
    val canLoadMore: Boolean
        get() = hasCachedResult && loadedPages >= 1 && totalPages != null && loadedPages < totalPages
}
