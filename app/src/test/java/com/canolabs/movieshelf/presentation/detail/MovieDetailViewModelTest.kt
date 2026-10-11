package com.canolabs.movieshelf.presentation.detail

import androidx.lifecycle.SavedStateHandle
import com.canolabs.movieshelf.domain.model.DataError
import com.canolabs.movieshelf.domain.model.MovieDetails
import com.canolabs.movieshelf.domain.model.RefreshResult
import com.canolabs.movieshelf.domain.repository.MovieRepository
import com.canolabs.movieshelf.domain.usecase.ObserveMovieDetailsUseCaseImpl
import com.canolabs.movieshelf.domain.usecase.RefreshMovieDetailsUseCaseImpl
import com.canolabs.movieshelf.testing.MainDispatcherRule
import com.canolabs.movieshelf.testing.movie
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovieDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeMovieRepository : MovieRepository {
        val movies = MutableStateFlow<Map<Long, MovieDetails>>(emptyMap())
        var refreshCalls = 0
        var onRefresh: suspend (Long) -> RefreshResult = { RefreshResult.Success }

        override fun observeMovie(movieId: Long): Flow<MovieDetails?> = movies.map { it[movieId] }
        override suspend fun refreshDetails(movieId: Long): RefreshResult {
            refreshCalls++
            return onRefresh(movieId)
        }
    }

    private val repository = FakeMovieRepository()
    private val cachedSummary = MovieDetails(movie = movie(1, "Arrival"), backdropUrl = null, extras = null)
    private val extras = MovieDetails.Extras(runtimeMinutes = 116, genres = listOf("Drama"), tagline = null)

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) = MovieDetailViewModel(
        movieId = 1,
        savedStateHandle = savedStateHandle,
        observeMovieDetails = ObserveMovieDetailsUseCaseImpl(repository),
        refreshMovieDetails = RefreshMovieDetailsUseCaseImpl(repository),
    )

    private fun TestScope.subscribe(viewModel: MovieDetailViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    @Test
    fun `initial state is loading and makes no request`() = runTest {
        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(0, repository.refreshCalls)
    }

    @Test
    fun `renders the cached movie without a network request`() = runTest {
        repository.movies.value = mapOf(1L to cachedSummary)
        val viewModel = createViewModel()
        subscribe(viewModel)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Arrival", state.details?.movie?.title)
        assertEquals(0, repository.refreshCalls)
    }

    @Test
    fun `opening fetches details once and shows the persisted extras`() = runTest {
        repository.movies.value = mapOf(1L to cachedSummary)
        repository.onRefresh = {
            repository.movies.update { it + (1L to cachedSummary.copy(extras = extras)) }
            RefreshResult.Success
        }
        val viewModel = createViewModel()
        subscribe(viewModel)

        viewModel.onOpened()
        viewModel.onOpened()

        assertEquals(1, repository.refreshCalls)
        assertEquals(extras, viewModel.uiState.value.details?.extras)
    }

    @Test
    fun `offline keeps cached fields and reports extras as unavailable`() = runTest {
        repository.movies.value = mapOf(1L to cachedSummary)
        repository.onRefresh = { RefreshResult.Failure(DataError.Network) }
        val viewModel = createViewModel()
        subscribe(viewModel)

        viewModel.onOpened()

        val state = viewModel.uiState.value
        assertEquals("Arrival", state.details?.movie?.title)
        assertTrue(state.extrasUnavailable)
        assertFalse(state.isMissing)
    }

    @Test
    fun `offline with previously fetched extras shows them without an error notice`() = runTest {
        repository.movies.value = mapOf(1L to cachedSummary.copy(extras = extras))
        repository.onRefresh = { RefreshResult.Failure(DataError.Network) }
        val viewModel = createViewModel()
        subscribe(viewModel)

        viewModel.onOpened()

        val state = viewModel.uiState.value
        assertEquals(extras, state.details?.extras)
        assertFalse(state.extrasUnavailable)
    }

    @Test
    fun `an uncached movie that cannot be fetched is reported as missing`() = runTest {
        repository.onRefresh = { RefreshResult.Failure(DataError.Network) }
        val viewModel = createViewModel()
        subscribe(viewModel)

        viewModel.onOpened()

        val state = viewModel.uiState.value
        assertTrue(state.isMissing)
        assertNull(state.details)
        assertEquals(DataError.Network, state.refreshError)
    }

    @Test
    fun `retry fetches again after a failure`() = runTest {
        repository.movies.value = mapOf(1L to cachedSummary)
        repository.onRefresh = { RefreshResult.Failure(DataError.Network) }
        val viewModel = createViewModel()
        subscribe(viewModel)
        viewModel.onOpened()

        repository.onRefresh = {
            repository.movies.update { it + (1L to cachedSummary.copy(extras = extras)) }
            RefreshResult.Success
        }
        viewModel.retry()

        assertEquals(2, repository.refreshCalls)
        assertNull(viewModel.uiState.value.refreshError)
        assertEquals(extras, viewModel.uiState.value.details?.extras)
    }
}
