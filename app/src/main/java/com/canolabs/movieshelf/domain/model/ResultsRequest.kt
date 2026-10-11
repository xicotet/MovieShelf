package com.canolabs.movieshelf.domain.model

/**
 * What a Results screen shows: TMDB results for a typed query, or TMDB's default discovery list.
 * Both share the same cache, pagination and screen; only the remote endpoint and the cache key differ.
 */
sealed interface ResultsRequest {

    /** Identity of the request's cached result set in Room. */
    val cacheKey: String

    data class Search(val query: SearchQuery) : ResultsRequest {
        override val cacheKey: String get() = query.key
    }

    /** Popular movies from TMDB's discover endpoint; not a typed query and never part of search history. */
    data object Discover : ResultsRequest {
        /**
         * Typed query keys are always lowercase ([SearchQuery.key]), so a key with uppercase letters can
         * never collide with a user's search.
         */
        override val cacheKey: String = "DISCOVER:popular"
    }
}
