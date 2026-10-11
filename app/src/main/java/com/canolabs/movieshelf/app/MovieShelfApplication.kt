package com.canolabs.movieshelf.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.canolabs.movieshelf.di.appModule
import com.canolabs.movieshelf.di.dataModule
import com.canolabs.movieshelf.di.domainModule
import com.canolabs.movieshelf.di.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MovieShelfApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MovieShelfApplication)
            modules(networkModule, dataModule, domainModule, appModule)
        }
    }

    /** Coil fetches posters with its own Ktor client (no API key involved) and caches them on disk. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .build()
}
