package com.canolabs.movieshelf.di

import com.canolabs.movieshelf.BuildConfig
import com.canolabs.movieshelf.data.remote.api.TmdbApiConfig
import com.canolabs.movieshelf.data.remote.api.TmdbApiService
import com.canolabs.movieshelf.data.remote.api.TmdbJson
import com.canolabs.movieshelf.data.remote.api.createTmdbHttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.dsl.module

val networkModule = module {
    single { TmdbApiConfig(apiKey = BuildConfig.TMDB_API_KEY) }
    single { TmdbJson }
    single { createTmdbHttpClient(engine = OkHttp.create(), json = get()) }
    single { TmdbApiService(client = get(), config = get()) }
}
