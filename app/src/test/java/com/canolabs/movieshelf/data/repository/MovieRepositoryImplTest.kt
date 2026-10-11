package com.canolabs.movieshelf.data.repository

import com.canolabs.movieshelf.data.remote.datasource.RemoteResult
import com.canolabs.movieshelf.data.remote.dto.GenreDto
import com.canolabs.movieshelf.data.remote.dto.MovieDetailsDto
import com.canolabs.movieshelf.domain.model.DataError
import com.canolabs.movieshelf.domain.model.MovieDetails
import com.canolabs.movieshelf.domain.model.RefreshResult
import com.canolabs.movieshelf.testing.FakeMovieLocalDataSource
import com.canolabs.movieshelf.testing.FakeMovieRemoteDataSource
import com.canolabs.movieshelf.testing.movieDto
import com.canolabs.movieshelf.testing.search
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MovieRepositoryImplTest {

    private val local = FakeMovieLocalDataSource()
    private val remote = FakeMovieRemoteDataSource()
    private val searchRepository = SearchRepositoryImpl(local, remote, clock = { 1L })
    private val repository = MovieRepositoryImpl(local, remote, clock = { 2L })

    private val details = MovieDetailsDto(
        id = 1,
        title = "Arrival",
        overview = "Linguist meets heptapods.",
        releaseDate = "2016-11-10",
        posterPath = "/arrival.jpg",
        backdropPath = "/arrival-backdrop.jpg",
        voteAverage = 7.6,
        voteCount = 18_000,
        runtime = 116,
        tagline = "Why are they here?",
        genres = listOf(GenreDto(18, "Drama"), GenreDto(878, "Science Fiction")),
    )

    @Test
    fun `cached search result renders without details extras`() = runTest {
        remote.searchResponses["Arrival"] = RemoteResult.Success(listOf(movieDto(1, "Arrival")))
        searchRepository.refresh(search("Arrival"))

        val cached = repository.observeMovie(1).first()

        assertEquals("Arrival", cached?.movie?.title)
        assertNull(cached?.extras)
    }

    @Test
    fun `persists fetched details and exposes them from the cache`() = runTest {
        remote.detailResponses[1] = RemoteResult.Success(details)

        val result = repository.refreshDetails(1)

        assertEquals(RefreshResult.Success, result)
        assertEquals(listOf("saveMovieDetails:1"), local.writes)
        assertEquals(
            MovieDetails.Extras(runtimeMinutes = 116, genres = listOf("Drama", "Science Fiction"), tagline = "Why are they here?"),
            repository.observeMovie(1).first()?.extras,
        )
    }

    @Test
    fun `failed detail refresh keeps the cached movie`() = runTest {
        remote.searchResponses["Arrival"] = RemoteResult.Success(listOf(movieDto(1, "Arrival")))
        searchRepository.refresh(search("Arrival"))
        remote.detailResponses[1] = RemoteResult.Failure(DataError.Network)

        val result = repository.refreshDetails(1)

        assertEquals(RefreshResult.Failure(DataError.Network), result)
        assertEquals("Arrival", repository.observeMovie(1).first()?.movie?.title)
    }

    @Test
    fun `a later search refresh keeps previously fetched details`() = runTest {
        remote.searchResponses["Arrival"] = RemoteResult.Success(listOf(movieDto(1, "Arrival")))
        searchRepository.refresh(search("Arrival"))
        remote.detailResponses[1] = RemoteResult.Success(details)
        repository.refreshDetails(1)

        searchRepository.refresh(search("Arrival"))

        assertEquals(116, repository.observeMovie(1).first()?.extras?.runtimeMinutes)
    }
}
