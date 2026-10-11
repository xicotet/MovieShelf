package com.canolabs.movieshelf.presentation.results

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canolabs.movieshelf.domain.model.RefreshResult
import com.canolabs.movieshelf.domain.model.ResultsRequest
import com.canolabs.movieshelf.domain.model.SearchQuery
import com.canolabs.movieshelf.domain.usecase.GetConnectivityStatusUseCase
import com.canolabs.movieshelf.domain.usecase.LoadNextResultsPageUseCase
import com.canolabs.movieshelf.domain.usecase.ObserveConnectivityUseCase
import com.canolabs.movieshelf.domain.usecase.ObserveLocalTitleMatchesUseCase
import com.canolabs.movieshelf.domain.usecase.ObserveSearchResultsUseCase
import com.canolabs.movieshelf.domain.usecase.RecordSearchUseCase
import com.canolabs.movieshelf.domain.usecase.RefreshSearchResultsUseCase
import com.canolabs.movieshelf.presentation.results.ResultsContract.UiAction
import com.canolabs.movieshelf.presentation.results.ResultsContract.UiIntent
import com.canolabs.movieshelf.presentation.results.ResultsContract.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.TimeSource

/**
 * Owns one submitted [ResultsRequest] (a typed search or Discover) for the lifetime of its Results
 * destination. A new submission creates a new destination (and ViewModel), so this screen can never
 * display another request's results; leaving
 * the destination cancels its in-flight request with [viewModelScope].
 */
class ResultsViewModel(
    private val request: ResultsRequest,
    private val savedStateHandle: SavedStateHandle,
    private val observeSearchResults: ObserveSearchResultsUseCase,
    private val refreshSearchResults: RefreshSearchResultsUseCase,
    private val loadNextResultsPage: LoadNextResultsPageUseCase,
    observeLocalTitleMatches: ObserveLocalTitleMatchesUseCase,
    private val recordSearch: RecordSearchUseCase,
    observeConnectivity: ObserveConnectivityUseCase,
    private val getConnectivityStatus: GetConnectivityStatusUseCase,
) : ViewModel() {

    // Results destinations are only created for validated submissions.
    /** The typed query, or `null` for Discover (no title matching, no search history). */
    private val searchQuery: SearchQuery? = (request as? ResultsRequest.Search)?.query
    private val label: String = searchQuery?.text ?: "Discover"

    private val refreshState = MutableStateFlow(RefreshState())
    private val nextPageState = MutableStateFlow(NextPageState())

    /** Safety net against a superseded request writing its outcome; see [refresh]. */
    private var refreshGeneration = 0

    /** Per-instance guard: re-entering the destination (e.g. back from Detail) must not reload. */
    private var loadHandled = false

    private val actions = Channel<UiAction>(Channel.BUFFERED)
    val uiActions: Flow<UiAction> = actions.receiveAsFlow()

    private val connectivity = observeConnectivity()
    private val isDiscover = request is ResultsRequest.Discover

    // Until Room's first emission the initial value (no cache known yet) is shown. Local matches are
    // only displayed by the reducer when TMDB is unavailable and nothing is cached for the query.
    //
    // The state pipeline owns the initial load: it starts the submission when the screen first
    // observes it. WhileSubscribed restarts the upstream (and so onStart) after every 5 s without
    // subscribers; loadOnce() is idempotent per instance and per entry, so that never reloads.
    val uiState: StateFlow<UiState> =
        combine(
            observeSearchResults(request),
            searchQuery?.let { observeLocalTitleMatches(it) } ?: flowOf(emptyList()),
            connectivity,
            combine(refreshState, nextPageState, ::Pair),
        ) { cache, localMatches, status, (refresh, nextPage) ->
            reduceResultsState(searchQuery?.text.orEmpty(), cache, localMatches, status, refresh, nextPage, isDiscover)
        }.onStart { loadOnce() }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = reduceResultsState(
                query = searchQuery?.text.orEmpty(),
                cache = null,
                localMatches = emptyList(),
                connectivity = connectivity.value,
                refresh = RefreshState(),
                isDiscover = isDiscover,
            ),
        )

    /** Single public entry point for user intents. */
    fun onIntent(intent: UiIntent) {
        when (intent) {
            UiIntent.Retry -> retry()
            UiIntent.LoadMore -> loadMore()
            is UiIntent.MovieSelected -> actions.trySend(UiAction.OpenMovieDetail(intent.movieId, posterUrlOf(intent.movieId)))
            UiIntent.SearchBarSelected -> actions.trySend(UiAction.OpenSearchEntry(searchQuery?.text.orEmpty()))
        }
    }

    /**
     * Runs the submission that opened this destination, once. Called whenever [uiState]'s upstream
     * starts, which happens again after re-subscription; the guards below make that a no-op.
     *
     * - First open: the request is sent once; [KEY_SUBMITTED] records that in [SavedStateHandle].
     *   If the device is confirmed offline, no request is sent: the query is recorded as a recent
     *   search and cached results or saved title matches are shown immediately.
     * - Same ViewModel again (re-subscription, configuration change, returning from another
     *   destination): nothing happens ([loadHandled]).
     * - Recreated after process death: the flag only proves the search was *submitted*, not that it
     *   completed. If Room has no completed result for the query, the submission is resumed with the
     *   same offline rule; otherwise the cached result is shown without a new request.
     *
     * Connectivity changes never trigger requests; only submissions and explicit retries do.
     */
    private fun loadOnce() {
        if (loadHandled) return
        loadHandled = true
        if (savedStateHandle.get<Boolean>(KEY_SUBMITTED) != true) {
            savedStateHandle[KEY_SUBMITTED] = true
            Log.d(TAG, "\"$label\" load: first open")
            submit(recordWhenOffline = true)
            return
        }
        viewModelScope.launch {
            val completed = observeSearchResults(request).first().hasCachedResult
            Log.d(TAG, "\"$label\" load: recreated after process death, cached result ${if (completed) "present" else "missing"}")
            if (completed) refreshState.update { it.noLoadNeeded() } else submit(recordWhenOffline = false)
        }
    }

    /**
     * Explicit user retry. The connectivity decision uses a fresh platform read (not the possibly
     * stale observed value): if the device is still offline, nothing is sent — that would only wait
     * for a timeout — and the user is told immediately.
     */
    private fun retry() {
        val status = getConnectivityStatus()
        if (status.isConfirmedOffline) {
            Log.d(TAG, "\"$label\" retry: still offline ($status), no request sent")
            actions.trySend(UiAction.ShowStillOffline)
            return
        }
        refresh()
    }

    /** Sends the submission unless the device is confirmed offline right now (fresh platform read). */
    private fun submit(recordWhenOffline: Boolean) {
        val status = getConnectivityStatus()
        if (!status.isConfirmedOffline) {
            refresh()
            return
        }
        Log.d(TAG, "\"$label\" submit: offline ($status), showing cached or saved results without a request")
        refreshState.update { it.skippedBecauseOffline() }
        // The repository records recent searches as part of a refresh; do the same without one.
        if (recordWhenOffline && searchQuery != null) viewModelScope.launch { recordSearch(searchQuery) }
    }

    /**
     * Starts a request unless one is already running for this query: repeated Retry taps or
     * pull-to-refresh gestures never cancel or restart the in-flight request.
     */
    private fun refresh() {
        if (refreshState.value.isRunning) {
            Log.d(TAG, "\"$label\" refresh: ignored, a request is already running")
            return
        }
        val generation = ++refreshGeneration
        refreshState.update { it.started() }
        Log.d(TAG, "\"$label\" refresh #$generation: started")
        val start = TimeSource.Monotonic.markNow()
        viewModelScope.launch {
            try {
                val result = refreshSearchResults(request)
                Log.d(TAG, "\"$label\" refresh #$generation: $result after ${start.elapsedNow().inWholeMilliseconds} ms")
                if (result is RefreshResult.Success) {
                    // The repository has persisted the response; end loading only once Room exposes it,
                    // so the screen goes straight from the previous state to the authoritative data.
                    observeSearchResults(request).first { it.hasCachedResult }
                }
                if (generation == refreshGeneration) {
                    refreshState.update {
                        when (result) {
                            RefreshResult.Success -> it.succeeded()
                            is RefreshResult.Failure -> it.failed(result.error)
                        }
                    }
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "\"$label\" refresh #$generation: cancelled")
                if (generation == refreshGeneration) refreshState.update { it.cancelled() }
                throw e
            }
        }
    }

    /**
     * Explicit request for the next TMDB page. Ignored while a next page or a page-1 refresh is
     * running, or when the shown results have no further page. Offline (fresh platform read), nothing
     * is sent and a retryable failure is shown under the existing results. A page arriving after a
     * page-1 refresh is rejected by the repository's transaction, so it can never corrupt the results.
     */
    private fun loadMore() {
        if (nextPageState.value.isLoading || refreshState.value.isRunning) {
            Log.d(TAG, "\"$label\" load more: ignored, a request is already running")
            return
        }
        if (uiState.value.nextPage !is ResultsContract.NextPage.Available &&
            uiState.value.nextPage !is ResultsContract.NextPage.Failed
        ) {
            Log.d(TAG, "\"$label\" load more: ignored, no further page")
            return
        }
        val status = getConnectivityStatus()
        if (status.isConfirmedOffline) {
            Log.d(TAG, "\"$label\" load more: offline ($status), no request sent")
            nextPageState.value = NextPageState(failure = ResultsContract.LoadMoreFailure.Offline)
            return
        }
        nextPageState.value = NextPageState(isLoading = true)
        Log.d(TAG, "\"$label\" load more: started")
        viewModelScope.launch {
            try {
                val pagesBefore = observeSearchResults(request).first().loadedPages
                val result = loadNextResultsPage(request)
                Log.d(TAG, "\"$label\" load more: $result")
                if (result is RefreshResult.Success) {
                    // The page is persisted; keep the footer loading until Room exposes it, so the
                    // button never reappears for a frame next to the old list. Bounded, because the
                    // repository succeeds without changes when the page was discarded as stale.
                    withTimeoutOrNull(PAGE_SYNC_TIMEOUT_MS) {
                        observeSearchResults(request).first { it.loadedPages != pagesBefore }
                    }
                }
                nextPageState.value = when (result) {
                    RefreshResult.Success -> NextPageState()
                    is RefreshResult.Failure -> NextPageState(failure = ResultsContract.LoadMoreFailure.Error(result.error))
                }
            } catch (e: CancellationException) {
                nextPageState.value = NextPageState()
                throw e
            }
        }
    }

    private fun posterUrlOf(movieId: Long): String? {
        val movies = when (val content = uiState.value.content) {
            is ResultsContract.Content.Movies -> content.movies
            is ResultsContract.Content.LocalMatches -> content.movies
            else -> emptyList()
        }
        return movies.firstOrNull { it.id == movieId }?.posterUrl
    }

    companion object {
        private const val TAG = "ResultsViewModel"
        private const val KEY_SUBMITTED = "results_submitted"
        private const val PAGE_SYNC_TIMEOUT_MS = 2_000L
    }
}
