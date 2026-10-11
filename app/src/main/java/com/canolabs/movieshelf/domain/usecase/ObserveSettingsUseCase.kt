package com.canolabs.movieshelf.domain.usecase

import com.canolabs.movieshelf.domain.model.AppSettings
import com.canolabs.movieshelf.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

interface ObserveSettingsUseCase {
    operator fun invoke(): Flow<AppSettings>
}

class ObserveSettingsUseCaseImpl(
    private val repository: SettingsRepository,
) : ObserveSettingsUseCase {
    override fun invoke(): Flow<AppSettings> = repository.settings
}
