package com.canolabs.movieshelf.presentation.results

import androidx.lifecycle.SavedStateHandle
import com.canolabs.movieshelf.domain.model.ConnectivityStatus
import com.canolabs.movieshelf.domain.model.DataError
import com.canolabs.movieshelf.domain.model.RefreshResult
import com.canolabs.movieshelf.domain.usecase.GetConnectivityStatusUseCaseImpl
import com.canolabs.movieshelf.domain.usecase.ObserveConnectivityUseCaseImpl
import com.canolabs.movieshelf.domain.usecase.ObserveLocalTitleMatchesUseCaseImpl
import com.canolabs.movieshelf.domain.usecase.ObserveSearchResultsUseCaseImpl
import com.canolabs.movieshelf.domain.usecase.RecordSearchUseCaseImpl
import com.canolabs.movieshelf.domain.usecase.RefreshSearchResultsUseCaseImpl
import com.canolabs.movieshelf.presentation.results.ResultsContract.Content
import com.canolabs.movieshelf.presentation.results.ResultsContract.Notice
import com.canolabs.movieshelf.presentation.results.ResultsContract.UiAction
import com.canolabs.movieshelf.presentation.results.ResultsContract.UiIntent
import com.canolabs.movieshelf.testing.FakeConnectivityMonitor
import com.canolabs.movieshelf.testing.FakeSearchRepository
import com.canolabs.movieshelf.testing.MainDispatcherRule
import com.canolabs.movieshelf.testing.movie
import com.canolabs.movieshelf.testing.query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ResultsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSearchRepository()
    private val connectivity = FakeConnectivityMonitor(ConnectivityStatus.Validated)

    private fun createViewModel(
        rawQuery: String = "Batman",
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ) = ResultsViewModel(
        rawQuery = rawQuery,
        savedStateHandle = savedStateHandle,
        observeSearchResults = ObserveSearchResultsUseCaseImpl(repository),
        refreshSearchResults = RefreshSearchResultsUseCaseImpl(repository),
        observeLocalTitleMatches = ObserveLocalTitleMatchesUseCaseImpl(repository),
        recordSearch = RecordSearchUseCaseImpl(repository),
        observeConnectivity = ObserveConnectivityUseCaseImpl(connectivity),
        getConnectivityStatus = GetConnectivityStatusUseCaseImpl(connectivity),
    )

    /** Keeps the WhileSubscribed state flow active, as a visible screen would. */
    private fun TestScope.subscribe(viewModel: ResultsViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    @Test
    fun `initial state is loading and nothing is requested before the state is observed`() = runTest {
        val viewModel = createViewModel(rawQuery = "  Batman  ")

        val state = viewModel.uiState.value
        assertEquals("Batman", state.query)
        assertEquals(Content.Loading, state.content)
        assertTrue(repository.refreshCalls.isEmpty())
    }

    @Test
    fun `observing the state starts the submission exactly once`() = runTest {
        val viewModel = createViewModel()

        subscribe(viewModel)
        subscribe(viewModel)

        assertEquals(listOf(query("Batman")), repository.refreshCalls)
    }

    @Test
    fun `re-subscribing after the sharing timeout does not resubmit`() = runTest {
        // A failed request leaves nothing cached: the case where a reload would be most tempting.
        repository.onRefresh = { RefreshResult.Failure(DataError.Network) }
        val viewModel = createViewModel()
        val first = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertEquals(1, repository.refreshCalls.size)

        // The screen stops observing (e.g. Detail on top) long enough for WhileSubscribed to stop the
        // upstream; observing again restarts it — and its onStart — but must not reload.
        first.cancel()
        advanceTimeBy(5_001)
        subscribe(viewModel)

        assertEquals(1, repository.refreshCalls.size)
    }

    @Test
    fun `a new submission of an already cached query refreshes it`() = runTest {
        repository.setCached(query("Batman"), listOf(movie(1)))
        repository.onRefresh = { q ->
            repository.setCached(q, listOf(movie(2)))
            RefreshResult.Success
        }

        // Each submission is its own navigation entry, hence its own ViewModel.
        subscribe(createViewModel(rawQuery = "Batman"))
        val resubmitted = createViewModel(rawQuery = "Batman")
        subscribe(resubmitted)

        assertEquals(2, repository.refreshCalls.size)
        assertEquals(Content.Movies(listOf(movie(2))), resubmitted.uiState.value.content)
    }

    @Test
    fun `recreation with a submitted query but no completed result restarts the request`() = runTest {
        val savedState = SavedStateHandle(mapOf("results_submitted" to true))
        repository.onRefresh = { q ->
            repository.setCached(q, listOf(movie(1)))
            RefreshResult.Success
        }
        val viewModel = createViewModel(savedStateHandle = savedState)
        subscribe(viewModel)

        assertEquals(1, repository.refreshCalls.size)
        assertEquals(Content.Movies(listOf(movie(1))), viewModel.uiState.value.content)
    }

    @Test
    fun `recreation with a completed cached result does not resubmit`() = runTest {
        repository.setCached(query("Batman"), listOf(movie(1)))
        val viewModel = createViewModel(savedStateHandle = SavedStateHandle(mapOf("results_submitted" to true)))
        subscribe(viewModel)

        assertTrue(repository.refreshCalls.isEmpty())
        val state = viewModel.uiState.value
        assertEquals(Content.Movies(listOf(movie(1))), state.content)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `shows loading while the request runs and content once it is persisted`() = runTest {
        val response = CompletableDeferred<Unit>()
        repository.onRefresh = { q ->
            response.await()
            repository.setCached(q, listOf(movie(1), movie(2)))
            RefreshResult.Success
        }
        val viewModel = createViewModel()
        subscribe(viewModel)

        assertEquals(Content.Loading, viewModel.uiState.value.content)

        response.complete(Unit)
        val state = viewModel.uiState.value
        assertEquals(Content.Movies(listOf(movie(1), movie(2))), state.content)
        assertFalse(state.isRefreshing)
        assertNull(state.notice)
    }

    @Test
    fun `shows the empty state for a successful response without matches`() = runTest {
        repository.onRefresh = { q ->
            repository.setCached(q, emptyList())
            RefreshResult.Success
        }
        val viewModel = createViewModel()
        subscribe(viewModel)

        assertEquals(Content.Empty, viewModel.uiState.value.content)
    }

    @Test
    fun `shows an error when the request fails and nothing is cached`() = runTest {
        repository.onRefresh = { RefreshResult.Failure(DataError.Network) }
        val viewModel = createViewModel()
        subscribe(viewModel)

        assertEquals(Content.Unavailable(Notice.RefreshFailed(DataError.Network)), viewModel.uiState.value.content)
    }

    @Test
    fun `keeps cached content and flags the failure when the refresh fails`() = runTest {
        repository.setCached(query("Batman"), listOf(movie(1)), refreshedAt = 42L)
        repository.onRefresh = { RefreshResult.Failure(DataError.Network) }
        val viewModel = createViewModel()
        subscribe(viewModel)

        val state = viewModel.uiState.value
        assertEquals(Content.Movies(listOf(movie(1))), state.content)
        assertEquals(Notice.RefreshFailed(DataError.Network), state.notice)
        assertEquals(42L, state.lastUpdatedAt)
    }

    @Test
    fun `shows cached content immediately while refreshing`() = runTest {
        repository.setCached(query("Batman"), listOf(movie(1)))
        val response = CompletableDeferred<RefreshResult>()
        repository.onRefresh = { response.await() }
        val viewModel = createViewModel()
        subscribe(viewModel)

        val state = viewModel.uiState.value
        assertEquals(Content.Movies(listOf(movie(1))), state.content)
        assertTrue(state.isRefreshing)
        response.complete(RefreshResult.Success)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `retry after an error shows the refreshed content`() = runTest {
        repository.onRefresh = { RefreshResult.Failure(DataError.Network) }
        val viewModel = createViewModel()
        subscribe(viewModel)

        repository.onRefresh = { q ->
            repository.setCached(q, listOf(movie(7)))
            RefreshResult.Success
        }
        viewModel.onIntent(UiIntent.Retry)

        assertEquals(Content.Movies(listOf(movie(7))), viewModel.uiState.value.content)
    }

    @Test
    fun `never shows results cached for another query`() = runTest {
        repository.setCached(query("Alien"), listOf(movie(1)))
        repository.onRefresh = { RefreshResult.Failure(DataError.Network) }
        val viewModel = createViewModel(rawQuery = "Batman")
        subscribe(viewModel)

        repository.setCached(query("Alien"), listOf(movie(2)))

        assertEquals(Content.Unavailable(Notice.RefreshFailed(DataError.Network)), viewModel.uiState.value.content)
    }

    @Test
    fun `repeated retries while a request runs do not start or cancel requests`() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.onRefresh = { q ->
            gate.await()
            repository.setCached(q, listOf(movie(1)))
            RefreshResult.Success
        }
        val viewModel = createViewModel()
        subscribe(viewModel)

        viewModel.onIntent(UiIntent.Retry)
        viewModel.onIntent(UiIntent.Retry)
        assertEquals(1, repository.refreshCalls.size)

        // The first request was neither cancelled nor restarted: it completes normally.
        gate.complete(Unit)
        assertEquals(Content.Movies(listOf(movie(1))), viewModel.uiState.value.content)
        assertFalse(viewModel.uiState.value.isRefreshing)

        // Once it has finished, a new retry is allowed.
        viewModel.onIntent(UiIntent.Retry)
        assertEquals(2, repository.refreshCalls.size)
    }

    @Test
    fun `a retry is allowed again after a failure`() = runTest {
        repository.onRefresh = { RefreshResult.Failure(DataError.Network) }
        val viewModel = createViewModel()
        subscribe(viewModel)

        viewModel.onIntent(UiIntent.Retry)

        assertEquals(2, repository.refreshCalls.size)
    }

    @Test
    fun `cached empty result keeps the empty state and exposes the failure notice`() = runTest {
        repository.setCached(query("Batman"), emptyList(), refreshedAt = 7L)
        repository.onRefresh = { RefreshResult.Failure(DataError.Network) }
        val viewModel = createViewModel()
        subscribe(viewModel)

        val state = viewModel.uiState.value
        assertEquals(Content.Empty, state.content)
        assertEquals(Notice.RefreshFailed(DataError.Network), state.notice)
        assertEquals(7L, state.lastUpdatedAt)
    }

    @Test
    fun `retrying over cached results keeps the notice and reports progress`() = runTest {
        repository.setCached(query("Batman"), listOf(movie(1)))
        repository.onRefresh = { RefreshResult.Failure(DataError.Network) }
        val viewModel = createViewModel()
        subscribe(viewModel)
        val retry = CompletableDeferred<RefreshResult>()
        repository.onRefresh = { retry.await() }

        viewModel.onIntent(UiIntent.Retry)

        val retrying = viewModel.uiState.value
        assertEquals(Content.Movies(listOf(movie(1))), retrying.content)
        assertEquals(Notice.RefreshFailed(DataError.Network), retrying.notice)
        assertTrue(retrying.isRefreshing)
        assertFalse(retrying.showPullRefreshIndicator)

        retry.complete(RefreshResult.Failure(DataError.Timeout))
        val failedAgain = viewModel.uiState.value
        assertEquals(Content.Movies(listOf(movie(1))), failedAgain.content)
        assertEquals(Notice.RefreshFailed(DataError.Timeout), failedAgain.notice)
        assertFalse(failedAgain.isRefreshing)
    }

    @Test
    fun `retrying without cache keeps the error instead of showing a skeleton`() = runTest {
        repository.onRefresh = { RefreshResult.Failure(DataError.Network) }
        val viewModel = createViewModel()
        subscribe(viewModel)
        val retry = CompletableDeferred<RefreshResult>()
        repository.onRefresh = { retry.await() }

        viewModel.onIntent(UiIntent.Retry)

        assertEquals(Content.Unavailable(Notice.RefreshFailed(DataError.Network)), viewModel.uiState.value.content)
        assertTrue(viewModel.uiState.value.isRefreshing)

        retry.complete(RefreshResult.Failure(DataError.Network))
        assertEquals(Content.Unavailable(Notice.RefreshFailed(DataError.Network)), viewModel.uiState.value.content)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `a successful retry clears the failure once room exposes the new results`() = runTest {
        repository.setCached(query("Batman"), listOf(movie(1)))
        repository.onRefresh = { RefreshResult.Failure(DataError.Network) }
        val viewModel = createViewModel()
        subscribe(viewModel)
        repository.onRefresh = { q ->
            repository.setCached(q, listOf(movie(2)), refreshedAt = 99L)
            RefreshResult.Success
        }

        viewModel.onIntent(UiIntent.Retry)

        val state = viewModel.uiState.value
        assertEquals(Content.Movies(listOf(movie(2))), state.content)
        assertNull(state.notice)
        assertEquals(99L, state.lastUpdatedAt)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `a cancelled request is not reported as a failure and can be retried`() = runTest {
        repository.onRefresh = { throw CancellationException("destination left") }
        val viewModel = createViewModel()
        subscribe(viewModel)

        val state = viewModel.uiState.value
        assertEquals(Content.NotLoaded, state.content)
        assertFalse(state.isRefreshing)

        repository.onRefresh = { q ->
            repository.setCached(q, listOf(movie(3)))
            RefreshResult.Success
        }
        viewModel.onIntent(UiIntent.Retry)
        assertEquals(Content.Movies(listOf(movie(3))), viewModel.uiState.value.content)
    }

    // region Connectivity and offline behaviour

    @Test
    fun `submitting while offline sends no request and still records the search`() = runTest {
        connectivity.emit(ConnectivityStatus.Unavailable)
        val viewModel = createViewModel()
        subscribe(viewModel)

        assertTrue(repository.refreshCalls.isEmpty())
        assertEquals(listOf(query("Batman")), repository.recordedSearches)
        val state = viewModel.uiState.value
        assertEquals(Content.Unavailable(Notice.Offline), state.content)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `offline submission shows cached results immediately with an offline notice`() = runTest {
        repository.setCached(query("Batman"), listOf(movie(1)))
        connectivity.emit(ConnectivityStatus.Unavailable)
        val viewModel = createViewModel()
        subscribe(viewModel)

        val state = viewModel.uiState.value
        assertEquals(Content.Movies(listOf(movie(1))), state.content)
        assertEquals(Notice.Offline, state.notice)
        assertFalse(state.isRefreshing)
        assertTrue(repository.refreshCalls.isEmpty())
    }

    @Test
    fun `offline submission keeps a cached empty result distinct from a cache miss`() = runTest {
        repository.setCached(query("Batman"), emptyList())
        connectivity.emit(ConnectivityStatus.Unavailable)
        val cachedEmpty = createViewModel(rawQuery = "Batman")
        val miss = createViewModel(rawQuery = "Robin")
        subscribe(cachedEmpty)
        subscribe(miss)

        assertEquals(Content.Empty, cachedEmpty.uiState.value.content)
        assertEquals(Notice.Offline, cachedEmpty.uiState.value.notice)
        assertEquals(Content.Unavailable(Notice.Offline), miss.uiState.value.content)
    }

    @Test
    fun `offline submission without cache falls back to saved title matches without caching them`() = runTest {
        repository.setLocalMatches(query("Batman"), listOf(movie(2), movie(5)))
        connectivity.emit(ConnectivityStatus.Unavailable)
        val viewModel = createViewModel()
        subscribe(viewModel)

        val state = viewModel.uiState.value
        assertEquals(Content.LocalMatches(listOf(movie(2), movie(5))), state.content)
        assertEquals(Notice.Offline, state.notice)
        // Local matches are never turned into a (fake) successful TMDB result for the query.
        assertFalse(repository.observeResults(query("Batman")).first().hasCachedResult)
        assertTrue(repository.refreshCalls.isEmpty())
    }

    @Test
    fun `saved title matches are not shown while an online search is running`() = runTest {
        repository.setLocalMatches(query("Batman"), listOf(movie(2)))
        val response = CompletableDeferred<RefreshResult>()
        repository.onRefresh = { response.await() }
        val viewModel = createViewModel()
        subscribe(viewModel)

        assertEquals(Content.Loading, viewModel.uiState.value.content)
        assertEquals(listOf(query("Batman")), repository.refreshCalls)
    }

    @Test
    fun `losing and regaining connectivity updates the notice without clearing results or searching`() = runTest {
        repository.onRefresh = { q ->
            repository.setCached(q, listOf(movie(1)))
            RefreshResult.Success
        }
        val viewModel = createViewModel()
        subscribe(viewModel)

        connectivity.emit(ConnectivityStatus.Unavailable)
        val offline = viewModel.uiState.value
        assertEquals(Content.Movies(listOf(movie(1))), offline.content)
        assertEquals(Notice.Offline, offline.notice)

        connectivity.emit(ConnectivityStatus.Validated)
        val online = viewModel.uiState.value
        assertEquals(Content.Movies(listOf(movie(1))), online.content)
        assertNull(online.notice)
        assertEquals(1, repository.refreshCalls.size)
    }

    @Test
    fun `connectivity returning after an offline submission does not search automatically`() = runTest {
        connectivity.emit(ConnectivityStatus.Unavailable)
        val viewModel = createViewModel()
        subscribe(viewModel)

        connectivity.emit(ConnectivityStatus.Unvalidated)
        connectivity.emit(ConnectivityStatus.Validated)

        assertTrue(repository.refreshCalls.isEmpty())
        assertEquals(Content.Unavailable(Notice.BackOnline), viewModel.uiState.value.content)
    }

    @Test
    fun `manual retry reaches the network even when the connectivity callback is delayed`() = runTest {
        connectivity.emit(ConnectivityStatus.Unavailable)
        val viewModel = createViewModel()
        subscribe(viewModel)
        // The network is back, but the callback has not been delivered to observers yet.
        connectivity.platformStatus = ConnectivityStatus.Validated
        repository.onRefresh = { q ->
            repository.setCached(q, listOf(movie(9)))
            RefreshResult.Success
        }

        viewModel.onIntent(UiIntent.Retry)

        assertEquals(listOf(query("Batman")), repository.refreshCalls)
        assertEquals(Content.Movies(listOf(movie(9))), viewModel.uiState.value.content)
    }

    @Test
    fun `retry while still offline sends nothing and gives immediate feedback`() = runTest {
        connectivity.emit(ConnectivityStatus.Unavailable)
        val viewModel = createViewModel()
        subscribe(viewModel)

        viewModel.onIntent(UiIntent.Retry)

        assertTrue(repository.refreshCalls.isEmpty())
        assertEquals(UiAction.ShowStillOffline, viewModel.uiActions.first())
        val state = viewModel.uiState.value
        assertEquals(Content.Unavailable(Notice.Offline), state.content)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `retrying after an offline submission keeps local matches visible until the result is persisted`() = runTest {
        repository.setLocalMatches(query("Batman"), listOf(movie(2)))
        connectivity.emit(ConnectivityStatus.Unavailable)
        val viewModel = createViewModel()
        subscribe(viewModel)
        connectivity.emit(ConnectivityStatus.Validated)
        val response = CompletableDeferred<Unit>()
        repository.onRefresh = { q ->
            response.await()
            repository.setCached(q, listOf(movie(7)))
            RefreshResult.Success
        }

        viewModel.onIntent(UiIntent.Retry)

        val retrying = viewModel.uiState.value
        assertEquals(Content.LocalMatches(listOf(movie(2))), retrying.content)
        assertEquals(Notice.BackOnline, retrying.notice)
        assertTrue(retrying.isRefreshing)

        response.complete(Unit)
        val done = viewModel.uiState.value
        assertEquals(Content.Movies(listOf(movie(7))), done.content)
        assertNull(done.notice)
        assertFalse(done.isRefreshing)
    }

    @Test
    fun `recreation while offline with nothing cached sends no request and is never stuck loading`() = runTest {
        repository.setLocalMatches(query("Batman"), listOf(movie(2)))
        connectivity.emit(ConnectivityStatus.Unavailable)
        val viewModel = createViewModel(savedStateHandle = SavedStateHandle(mapOf("results_submitted" to true)))
        subscribe(viewModel)

        assertTrue(repository.refreshCalls.isEmpty())
        assertTrue(repository.recordedSearches.isEmpty())
        assertEquals(Content.LocalMatches(listOf(movie(2))), viewModel.uiState.value.content)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `recreation while offline restores the offline notice over cached results`() = runTest {
        repository.setCached(query("Batman"), listOf(movie(1)))
        connectivity.emit(ConnectivityStatus.Unavailable)
        val viewModel = createViewModel(savedStateHandle = SavedStateHandle(mapOf("results_submitted" to true)))
        subscribe(viewModel)

        val state = viewModel.uiState.value
        assertEquals(Content.Movies(listOf(movie(1))), state.content)
        assertEquals(Notice.Offline, state.notice)
        assertTrue(repository.refreshCalls.isEmpty())
    }

    @Test
    fun `unknown or unvalidated connectivity still sends the request`() = runTest {
        connectivity.emit(ConnectivityStatus.Unknown)
        subscribe(createViewModel(rawQuery = "Batman"))
        connectivity.emit(ConnectivityStatus.Unvalidated)
        subscribe(createViewModel(rawQuery = "Robin"))

        assertEquals(listOf(query("Batman"), query("Robin")), repository.refreshCalls)
    }

    @Test
    fun `a timeout online falls back to saved title matches with a timeout notice`() = runTest {
        repository.setLocalMatches(query("Batman"), listOf(movie(2)))
        repository.onRefresh = { RefreshResult.Failure(DataError.Timeout) }
        val viewModel = createViewModel()
        subscribe(viewModel)

        val state = viewModel.uiState.value
        assertEquals(Content.LocalMatches(listOf(movie(2))), state.content)
        assertEquals(Notice.RefreshFailed(DataError.Timeout), state.notice)
    }

    @Test
    fun `a server error without cache is an error, not an offline state`() = runTest {
        repository.setLocalMatches(query("Batman"), listOf(movie(2)))
        repository.onRefresh = { RefreshResult.Failure(DataError.Server) }
        val viewModel = createViewModel()
        subscribe(viewModel)

        assertEquals(Content.Error(DataError.Server), viewModel.uiState.value.content)
    }

    // endregion

    @Test
    fun `selecting a movie emits an open detail action with its poster`() = runTest {
        val poster = movie(78).copy(posterUrl = "https://image.test/78.jpg")
        repository.setCached(query("Batman"), listOf(poster))
        // Observing starts the submission; keep the cached result unchanged by the refresh.
        repository.onRefresh = { RefreshResult.Success }
        val viewModel = createViewModel()
        subscribe(viewModel)

        viewModel.onIntent(UiIntent.MovieSelected(78))

        assertEquals(UiAction.OpenMovieDetail(78, "https://image.test/78.jpg"), viewModel.uiActions.first())
    }

    @Test
    fun `selecting the search bar reopens search entry with the current query`() = runTest {
        val viewModel = createViewModel(rawQuery = " Blade  Runner ")

        viewModel.onIntent(UiIntent.SearchBarSelected)

        assertEquals(UiAction.OpenSearchEntry("Blade Runner"), viewModel.uiActions.first())
    }
}
