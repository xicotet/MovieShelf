package com.canolabs.movieshelf.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.canolabs.movieshelf.domain.model.AppSettings
import com.canolabs.movieshelf.domain.model.TextSize
import com.canolabs.movieshelf.domain.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Runs against a real Preferences DataStore file. */
class SettingsRepositoryImplTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var file: File

    /** A DataStore with its own scope, so it can be closed before reopening the same file. */
    private fun <T> withDataStore(block: suspend (DataStore<Preferences>) -> T): T {
        if (!::file.isInitialized) file = File(folder.root, "settings.preferences_pb")
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        return try {
            runBlocking { block(PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })) }
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `defaults to system theme and default text size`() {
        val settings = withDataStore { SettingsRepositoryImpl(it).settings.first() }

        assertEquals(AppSettings(ThemeMode.System, TextSize.Default), settings)
    }

    @Test
    fun `saves changes and restores them from a new instance, as after process death`() {
        withDataStore { dataStore ->
            val repository = SettingsRepositoryImpl(dataStore)
            repository.setThemeMode(ThemeMode.Dark)
            repository.setTextSize(TextSize.Large)
        }

        val restored = withDataStore { SettingsRepositoryImpl(it).settings.first() }

        assertEquals(AppSettings(ThemeMode.Dark, TextSize.Large), restored)
    }

    @Test
    fun `unknown stored values fall back to the defaults`() {
        withDataStore { dataStore ->
            dataStore.edit {
                it[stringPreferencesKey("theme_mode")] = "Sepia"
                it[stringPreferencesKey("text_size")] = "Huge"
            }
        }

        val settings = withDataStore { SettingsRepositoryImpl(it).settings.first() }

        assertEquals(AppSettings(), settings)
    }
}
