package com.canolabs.movieshelf.domain.repository

import com.canolabs.movieshelf.domain.model.AppSettings
import com.canolabs.movieshelf.domain.model.TextSize
import com.canolabs.movieshelf.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    /** Current settings; defaults until the user changes something. */
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setTextSize(size: TextSize)
}
