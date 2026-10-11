package com.canolabs.movieshelf.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Builds the TMDB HTTP client. No logging plugin is installed on purpose: request URLs carry the
 * API key as a query parameter and must never end up in logs.
 */
fun createTmdbHttpClient(engine: HttpClientEngine, json: Json): HttpClient = HttpClient(engine) {
    expectSuccess = true
    install(ContentNegotiation) { json(json) }
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 20_000
        socketTimeoutMillis = 20_000
    }
}

val TmdbJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
}
