package com.canolabs.movieshelf.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.canolabs.movieshelf.domain.model.AppSettings
import com.canolabs.movieshelf.domain.model.TextSize
import com.canolabs.movieshelf.domain.model.ThemeMode
import com.canolabs.movieshelf.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Settings stored in a Preferences DataStore; unreadable or unknown values fall back to defaults. */
class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences ->
            AppSettings(
                themeMode = preferences[THEME_MODE].toEnumOrNull<ThemeMode>() ?: ThemeMode.System,
                textSize = preferences[TEXT_SIZE].toEnumOrNull<TextSize>() ?: TextSize.Default,
            )
        }
        .distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    override suspend fun setTextSize(size: TextSize) {
        dataStore.edit { it[TEXT_SIZE] = size.name }
    }

    private inline fun <reified T : Enum<T>> String?.toEnumOrNull(): T? =
        enumValues<T>().firstOrNull { it.name == this }

    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val TEXT_SIZE = stringPreferencesKey("text_size")
    }
}
