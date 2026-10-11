package com.canolabs.movieshelf.testing

import com.canolabs.movieshelf.data.remote.datasource.MovieRemoteDataSource
import com.canolabs.movieshelf.data.remote.datasource.RemoteResult
import com.canolabs.movieshelf.data.remote.dto.MovieDetailsDto
import com.canolabs.movieshelf.data.remote.dto.MovieDto
import com.canolabs.movieshelf.domain.model.DataError

class FakeMovieRemoteDataSource : MovieRemoteDataSource {

    /** Responses by exact query text; unknown queries fail with a network error. */
    val searchResponses = mutableMapOf<String, RemoteResult<List<MovieDto>>>()
    val detailResponses = mutableMapOf<Long, RemoteResult<MovieDetailsDto>>()

    /** Runs before a search response is returned, e.g. to suspend or inspect state. */
    var beforeSearchResponse: suspend (String) -> Unit = {}

    val searchCalls = mutableListOf<String>()

    override suspend fun searchMovies(query: String): RemoteResult<List<MovieDto>> {
        searchCalls += query
        beforeSearchResponse(query)
        return searchResponses[query] ?: RemoteResult.Failure(DataError.Network)
    }

    override suspend fun movieDetails(movieId: Long): RemoteResult<MovieDetailsDto> =
        detailResponses[movieId] ?: RemoteResult.Failure(DataError.Network)
}
