package com.canolabs.movieshelf.domain.model

/**
 * The locally persisted state of one request's result set.
 *
 * [hasCachedResult] tells a cache miss apart from a cached, genuinely empty result. [canLoadMore] is
 * true when another TMDB page exists according to TMDB's own metadata, never inferred from page size.
 *
 * @property movies Ordered as returned by the API.
 * @property lastRefreshedAt `null` when the request has never been fetched successfully.
 * @property loadedPages TMDB pages stored for the request.
 * @property totalPages TMDB's total page count for the request, or `null` when unknown.
 */
data class CachedSearch(
    val request: ResultsRequest,
    val movies: List<Movie>,
    val lastRefreshedAt: Long?,
    val loadedPages: Int = 0,
    val totalPages: Int? = null,
) {
    val hasCachedResult: Boolean get() = lastRefreshedAt != null

    val canLoadMore: Boolean
        get() = hasCachedResult && loadedPages >= 1 && totalPages != null && loadedPages < totalPages
}
