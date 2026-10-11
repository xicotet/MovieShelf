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

    suspend fun movieDetails(movieId: Long): MovieDetailsDto =
        client.get(config.baseUrl + "movie/$movieId") {
            authorize()
        }.body()

    private fun HttpRequestBuilder.authorize() {
        parameter("api_key", config.apiKey)
    }
}
