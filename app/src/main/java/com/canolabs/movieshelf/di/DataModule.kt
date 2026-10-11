package com.canolabs.movieshelf.di

import android.net.ConnectivityManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.canolabs.movieshelf.data.connectivity.AndroidNetworkStatusSource
import com.canolabs.movieshelf.data.connectivity.SharedConnectivityMonitor
import com.canolabs.movieshelf.data.local.database.MIGRATION_1_2
import com.canolabs.movieshelf.data.local.database.MIGRATION_2_3
import com.canolabs.movieshelf.data.local.database.MovieShelfDatabase
import com.canolabs.movieshelf.data.local.datasource.MovieLocalDataSource
import com.canolabs.movieshelf.data.local.datasource.MovieLocalDataSourceImpl
import com.canolabs.movieshelf.data.remote.datasource.MovieRemoteDataSource
import com.canolabs.movieshelf.data.remote.datasource.MovieRemoteDataSourceImpl
import com.canolabs.movieshelf.data.repository.MovieRepositoryImpl
import com.canolabs.movieshelf.data.repository.SearchRepositoryImpl
import com.canolabs.movieshelf.data.repository.SettingsRepositoryImpl
import com.canolabs.movieshelf.domain.repository.ConnectivityMonitor
import com.canolabs.movieshelf.domain.repository.MovieRepository
import com.canolabs.movieshelf.domain.repository.SearchRepository
import com.canolabs.movieshelf.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single {
        Room.databaseBuilder(androidContext(), MovieShelfDatabase::class.java, MovieShelfDatabase.NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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

    // One DataStore instance per file for the whole process, as DataStore requires.
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.create(produceFile = { androidContext().preferencesDataStoreFile("settings") })
    }
    single<SettingsRepository> { SettingsRepositoryImpl(dataStore = get()) }

    single<SearchRepository> { SearchRepositoryImpl(local = get(), remote = get()) }
    single<MovieRepository> { MovieRepositoryImpl(local = get(), remote = get()) }
}
