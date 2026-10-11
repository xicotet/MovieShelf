package com.canolabs.movieshelf.di

import android.net.ConnectivityManager
import androidx.room.Room
import com.canolabs.movieshelf.data.connectivity.AndroidNetworkStatusSource
import com.canolabs.movieshelf.data.connectivity.SharedConnectivityMonitor
import com.canolabs.movieshelf.data.local.database.MovieShelfDatabase
import com.canolabs.movieshelf.data.local.datasource.MovieLocalDataSource
import com.canolabs.movieshelf.data.local.datasource.MovieLocalDataSourceImpl
import com.canolabs.movieshelf.data.remote.datasource.MovieRemoteDataSource
import com.canolabs.movieshelf.data.remote.datasource.MovieRemoteDataSourceImpl
import com.canolabs.movieshelf.data.repository.MovieRepositoryImpl
import com.canolabs.movieshelf.data.repository.SearchRepositoryImpl
import com.canolabs.movieshelf.domain.repository.ConnectivityMonitor
import com.canolabs.movieshelf.domain.repository.MovieRepository
import com.canolabs.movieshelf.domain.repository.SearchRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single {
        Room.databaseBuilder(androidContext(), MovieShelfDatabase::class.java, MovieShelfDatabase.NAME)
            .build()
    }
    single { get<MovieShelfDatabase>().searchDao() }
    single { get<MovieShelfDatabase>().movieDao() }

    single<MovieLocalDataSource> { MovieLocalDataSourceImpl(database = get(), searchDao = get(), movieDao = get()) }
    single<MovieRemoteDataSource> { MovieRemoteDataSourceImpl(api = get()) }

    // One platform callback shared app-wide; registered only while someone observes connectivity.
    single<ConnectivityMonitor> {
        SharedConnectivityMonitor(
            source = AndroidNetworkStatusSource(androidContext().getSystemService(ConnectivityManager::class.java)),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )
    }

    single<SearchRepository> { SearchRepositoryImpl(local = get(), remote = get()) }
    single<MovieRepository> { MovieRepositoryImpl(local = get(), remote = get()) }
}
