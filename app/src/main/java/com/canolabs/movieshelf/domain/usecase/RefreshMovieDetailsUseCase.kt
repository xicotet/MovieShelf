package com.canolabs.movieshelf.domain.usecase

import com.canolabs.movieshelf.domain.model.RefreshResult
import com.canolabs.movieshelf.domain.repository.MovieRepository

interface RefreshMovieDetailsUseCase {
    suspend operator fun invoke(movieId: Long): RefreshResult
}

class RefreshMovieDetailsUseCaseImpl(
    private val repository: MovieRepository,
) : RefreshMovieDetailsUseCase {
    override suspend fun invoke(movieId: Long): RefreshResult = repository.refreshDetails(movieId)
}
