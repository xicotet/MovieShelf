package com.canolabs.movieshelf.domain.repository

import com.canolabs.movieshelf.domain.model.MovieDetails
import com.canolabs.movieshelf.domain.model.RefreshResult
import kotlinx.coroutines.flow.Flow

interface MovieRepository {

    /** Emits `null` when the movie is not cached locally. */
    fun observeMovie(movieId: Long): Flow<MovieDetails?>

    /** Fetches full details and persists them before returning. Failures keep cached data. */
    suspend fun refreshDetails(movieId: Long): RefreshResult
}
