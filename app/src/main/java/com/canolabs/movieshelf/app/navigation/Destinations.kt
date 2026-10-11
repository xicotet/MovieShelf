package com.canolabs.movieshelf.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Back-stack keys. They are serializable so the back stack survives process recreation. */
@Serializable
data object SearchDestination : NavKey

@Serializable
data class SearchEntryDestination(val initialQuery: String = "") : NavKey

/**
 * One submitted search. [query] is the validated query text (results are looked up by its normalized
 * key); [submissionId] is unique per submission, so submitting the same text again creates a new
 * entry — and therefore a new ViewModel that runs the search — instead of reusing the previous one.
 */
@Serializable
data class ResultsDestination(val query: String, val submissionId: String) : NavKey

/**
 * The stable movie ID identifies the destination; Detail reads all data from the local cache.
 * [posterUrl] is only a presentation hint so the shared poster can be drawn on the very first frame
 * of the transition, before Room's first emission arrives.
 */
@Serializable
data class MovieDetailDestination(val movieId: Long, val posterUrl: String? = null) : NavKey
