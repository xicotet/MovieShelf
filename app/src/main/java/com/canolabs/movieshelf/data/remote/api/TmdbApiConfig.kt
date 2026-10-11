package com.canolabs.movieshelf.data.remote.api

/** TMDB endpoint configuration. [apiKey] is injected from the local build configuration. */
class TmdbApiConfig(
    val apiKey: String,
    val baseUrl: String = "https://api.themoviedb.org/3/",
) {
    // Keep the key out of logs and crash reports that print this object.
    override fun toString(): String = "TmdbApiConfig(baseUrl=$baseUrl)"

    companion object {
        const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"
        const val POSTER_SIZE = "w500"
        const val BACKDROP_SIZE = "w780"
    }
}
