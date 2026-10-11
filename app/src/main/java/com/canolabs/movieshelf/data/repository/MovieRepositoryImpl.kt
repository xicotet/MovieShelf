package com.canolabs.movieshelf.data.repository

import com.canolabs.movieshelf.data.local.datasource.MovieLocalDataSource
import com.canolabs.movieshelf.data.mapper.toDomainDetails
import com.canolabs.movieshelf.data.mapper.toEntity
import com.canolabs.movieshelf.data.remote.datasource.MovieRemoteDataSource
import com.canolabs.movieshelf.data.remote.datasource.RemoteResult
import com.canolabs.movieshelf.domain.model.MovieDetails
import com.canolabs.movieshelf.domain.model.RefreshResult
import com.canolabs.movieshelf.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MovieRepositoryImpl(
    private val local: MovieLocalDataSource,
    private val remote: MovieRemoteDataSource,
    private val clock: () -> Long = System::currentTimeMillis,
) : MovieRepository {

    override fun observeMovie(movieId: Long): Flow<MovieDetails?> =
        local.observeMovie(movieId).map { it?.toDomainDetails() }

    override suspend fun refreshDetails(movieId: Long): RefreshResult =
        when (val response = remote.movieDetails(movieId)) {
            is RemoteResult.Success -> {
                local.saveMovieDetails(response.data.toEntity(fetchedAt = clock()))
                RefreshResult.Success
            }
            is RemoteResult.Failure -> RefreshResult.Failure(response.error)
        }
}
