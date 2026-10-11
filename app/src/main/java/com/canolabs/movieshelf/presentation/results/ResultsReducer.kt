package com.canolabs.movieshelf.presentation.results

import com.canolabs.movieshelf.domain.model.CachedSearch
import com.canolabs.movieshelf.domain.model.ConnectivityStatus
import com.canolabs.movieshelf.domain.model.DataError
import com.canolabs.movieshelf.domain.model.Movie

/**
 * Refresh lifecycle owned by the ViewModel, independent of the cached content it updates.
 */
internal data class RefreshState(
    /** A request for the query is running. */
    val isRunning: Boolean = false,
    /** Most recent failure; survives retries and is cleared only by a successful refresh. */
    val lastFailure: DataError? = null,
    /** The destination has not decided yet whether to request; a request is expected imminently. */
    val isAwaitingLoad: Boolean = true,
    /** The submission was not sent because the device was offline. Kept while a retry runs. */
    val skippedOffline: Boolean = false,
) {
    fun started() = copy(isRunning = true, isAwaitingLoad = false)
    fun succeeded() = RefreshState(isRunning = false, lastFailure = null, isAwaitingLoad = false, skippedOffline = false)
    fun failed(error: DataError) = copy(isRunning = false, lastFailure = error, isAwaitingLoad = false, skippedOffline = false)

    /** Cancellation is not a failure: the previous failure (if any) is kept, nothing new is recorded. */
    fun cancelled() = copy(isRunning = false, isAwaitingLoad = false)
    fun noLoadNeeded() = copy(isAwaitingLoad = false)
    fun skippedBecauseOffline() = copy(isRunning = false, isAwaitingLoad = false, skippedOffline = true)
}

/** Next-page requests, kept apart from [RefreshState] so their failures never hide loaded pages. */
internal data class NextPageState(
    val isLoading: Boolean = false,
    val failure: ResultsContract.LoadMoreFailure? = null,
)

/** Failures that mean "TMDB could not be reached", as opposed to TMDB answering with an error. */
internal val DataError.isTransport: Boolean
    get() = this == DataError.Network || this == DataError.Timeout

/**
 * Pure reducer: derives the screen state from the latest cached snapshot (`null` until Room has
 * emitted for this query), saved title matches, live connectivity and the refresh lifecycle.
 */
internal fun reduceResultsState(
    query: String,
    cache: CachedSearch?,
    localMatches: List<Movie>,
    connectivity: ConnectivityStatus,
    refresh: RefreshState,
    nextPage: NextPageState = NextPageState(),
): ResultsContract.UiState {
    val offline = connectivity.isConfirmedOffline
    val hasCachedResult = cache?.hasCachedResult == true

    // Live connectivity takes precedence, so the notice follows the network without a new request.
    val notice: ResultsContract.Notice? = when {
        offline -> ResultsContract.Notice.Offline
        refresh.lastFailure != null -> ResultsContract.Notice.RefreshFailed(refresh.lastFailure)
        refresh.skippedOffline -> ResultsContract.Notice.BackOnline
        else -> null
    }

    // TMDB is unavailable for this query: the remote search was skipped offline, failed to reach the
    // server, or the device is offline with nothing running or about to run.
    val remoteUnavailable = refresh.skippedOffline ||
        refresh.lastFailure?.isTransport == true ||
        (offline && !refresh.isRunning && !refresh.isAwaitingLoad)

    val content = when {
        // Room always emits for an observed query, so this is only the initial frame.
        cache == null -> ResultsContract.Content.Loading
        hasCachedResult && cache.movies.isEmpty() -> ResultsContract.Content.Empty
        hasCachedResult -> ResultsContract.Content.Movies(cache.movies)
        remoteUnavailable && localMatches.isNotEmpty() -> ResultsContract.Content.LocalMatches(localMatches)
        remoteUnavailable && notice != null -> ResultsContract.Content.Unavailable(notice)
        refresh.lastFailure != null -> ResultsContract.Content.Error(refresh.lastFailure)
        refresh.isRunning || refresh.isAwaitingLoad -> ResultsContract.Content.Loading
        else -> ResultsContract.Content.NotLoaded
    }
    val showsNotice = content is ResultsContract.Content.Movies ||
        content == ResultsContract.Content.Empty ||
        content is ResultsContract.Content.LocalMatches
    return ResultsContract.UiState(
        query = query,
        content = content,
        isRefreshing = refresh.isRunning,
        notice = notice?.takeIf { showsNotice },
        lastUpdatedAt = cache?.lastRefreshedAt,
        nextPage = when {
            // Pagination applies to TMDB results only, never to local title matches.
            content !is ResultsContract.Content.Movies -> ResultsContract.NextPage.Unavailable
            nextPage.isLoading -> ResultsContract.NextPage.Loading
            cache?.canLoadMore != true -> ResultsContract.NextPage.Unavailable
            nextPage.failure != null -> ResultsContract.NextPage.Failed(nextPage.failure)
            else -> ResultsContract.NextPage.Available
        },
    )
}
