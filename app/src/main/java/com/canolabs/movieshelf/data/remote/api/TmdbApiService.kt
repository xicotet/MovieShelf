package com.canolabs.movieshelf.data.remote.api

import com.canolabs.movieshelf.data.remote.dto.MovieDetailsDto
import com.canolabs.movieshelf.data.remote.dto.SearchMoviesResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Thin typed wrapper over the TMDB v3 endpoints used by the app.
 * The client is configured with `expectSuccess = true`, so non-2xx responses throw.
 */
class TmdbApiService(
    private val client: HttpClient,
    private val config: TmdbApiConfig,
) {

    suspend fun searchMovies(query: String, page: Int = 1): SearchMoviesResponseDto =
        client.get(config.baseUrl + "search/movie") {
            authorize()
            parameter("query", query)
            parameter("page", page)
            parameter("include_adult", false)
        }.body()

    /** TMDB's default discovery: popular movies, same page format as search. */
    suspend fun discoverMovies(page: Int = 1): SearchMoviesResponseDto =
        client.get(config.baseUrl + "discover/movie") {
            authorize()
            parameter("sort_by", "popularity.desc")
            parameter("page", page)
            parameter("include_adult", false)
            parameter("include_video", false)
        }.body()

    suspend fun movieDetails(movieId: Long): MovieDetailsDto =
        client.get(config.baseUrl + "movie/$movieId") {
            authorize()
        }.body()

    private fun HttpRequestBuilder.authorize() {
        parameter("api_key", config.apiKey)
    }
}
