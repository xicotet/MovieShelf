package com.canolabs.movieshelf.testing

import com.canolabs.movieshelf.data.remote.datasource.MovieRemoteDataSource
import com.canolabs.movieshelf.data.remote.datasource.RemoteResult
import com.canolabs.movieshelf.data.remote.dto.MovieDetailsDto
import com.canolabs.movieshelf.data.remote.dto.MovieDto
import com.canolabs.movieshelf.data.remote.dto.SearchMoviesResponseDto
import com.canolabs.movieshelf.domain.model.DataError

class FakeMovieRemoteDataSource : MovieRemoteDataSource {

    /** Page-1 responses by exact query text; unknown queries fail with a network error. */
    val searchResponses = mutableMapOf<String, RemoteResult<List<MovieDto>>>()

    /** TMDB's `total_pages` per query text (default 1). */
    val totalPages = mutableMapOf<String, Int>()

    /** Responses for pages >= 2, by query text and page. */
    val pageResponses = mutableMapOf<Pair<String, Int>, RemoteResult<List<MovieDto>>>()
    val detailResponses = mutableMapOf<Long, RemoteResult<MovieDetailsDto>>()

    /** Runs before a search response is returned, e.g. to suspend or inspect state. */
    var beforeSearchResponse: suspend (String) -> Unit = {}

    val searchCalls = mutableListOf<String>()

    /** Every (query, page) requested, in order. */
    val pageCalls = mutableListOf<Pair<String, Int>>()

    override suspend fun searchMovies(query: String, page: Int): RemoteResult<SearchMoviesResponseDto> {
        if (page == 1) searchCalls += query
        pageCalls += query to page
        beforeSearchResponse(query)
        val response = (if (page == 1) searchResponses[query] else pageResponses[query to page])
            ?: RemoteResult.Failure(DataError.Network)
        return when (response) {
            is RemoteResult.Success -> RemoteResult.Success(
                SearchMoviesResponseDto(page = page, results = response.data, totalPages = totalPages[query] ?: 1),
            )
            is RemoteResult.Failure -> response
        }
    }

    /** Discover responses by page; unknown pages fail with a network error. */
    val discoverResponses = mutableMapOf<Int, RemoteResult<List<MovieDto>>>()
    var discoverTotalPages = 1
    val discoverCalls = mutableListOf<Int>()

    override suspend fun discoverMovies(page: Int): RemoteResult<SearchMoviesResponseDto> {
        discoverCalls += page
        return when (val response = discoverResponses[page] ?: RemoteResult.Failure(DataError.Network)) {
            is RemoteResult.Success -> RemoteResult.Success(
                SearchMoviesResponseDto(page = page, results = response.data, totalPages = discoverTotalPages),
            )
            is RemoteResult.Failure -> response
        }
    }

    override suspend fun movieDetails(movieId: Long): RemoteResult<MovieDetailsDto> =
        detailResponses[movieId] ?: RemoteResult.Failure(DataError.Network)
}
