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
) {
    val hasCachedResult: Boolean get() = lastRefreshedAt != null
}
