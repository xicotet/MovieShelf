package com.canolabs.movieshelf.domain.usecase

import com.canolabs.movieshelf.domain.model.TextSize
import com.canolabs.movieshelf.domain.repository.SettingsRepository

interface SetTextSizeUseCase {
    suspend operator fun invoke(size: TextSize)
}

class SetTextSizeUseCaseImpl(
    private val repository: SettingsRepository,
) : SetTextSizeUseCase {
    override suspend fun invoke(size: TextSize) = repository.setTextSize(size)
}
