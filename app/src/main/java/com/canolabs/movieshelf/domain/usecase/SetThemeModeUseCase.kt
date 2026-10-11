package com.canolabs.movieshelf.domain.usecase

import com.canolabs.movieshelf.domain.model.ThemeMode
import com.canolabs.movieshelf.domain.repository.SettingsRepository

interface SetThemeModeUseCase {
    suspend operator fun invoke(mode: ThemeMode)
}

class SetThemeModeUseCaseImpl(
    private val repository: SettingsRepository,
) : SetThemeModeUseCase {
    override suspend fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}
