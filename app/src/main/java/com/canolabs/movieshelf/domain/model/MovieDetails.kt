package com.canolabs.movieshelf.domain.model

/**
 * Everything known locally about a movie.
 *
 * The summary fields come from search results and are always present once a movie is cached.
 * [extras] is only available after the detail endpoint has been fetched successfully at least once,
 * so the UI can tell "not fetched yet / unavailable offline" apart from "the movie has no genres".
 */
data class MovieDetails(
    val movie: Movie,
    val backdropUrl: String?,
    val extras: Extras?,
) {
    data class Extras(
        val runtimeMinutes: Int?,
        val genres: List<String>,
        val tagline: String?,
    )
}
