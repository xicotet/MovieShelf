package com.canolabs.movieshelf.domain.usecase

import com.canolabs.movieshelf.domain.model.MovieDetails
import com.canolabs.movieshelf.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow

interface ObserveMovieDetailsUseCase {
    operator fun invoke(movieId: Long): Flow<MovieDetails?>
}

class ObserveMovieDetailsUseCaseImpl(
    private val repository: MovieRepository,
) : ObserveMovieDetailsUseCase {
    override fun invoke(movieId: Long): Flow<MovieDetails?> = repository.observeMovie(movieId)
}
