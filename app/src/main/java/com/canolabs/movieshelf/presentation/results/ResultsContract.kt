package com.canolabs.movieshelf.presentation.results

import com.canolabs.movieshelf.domain.model.CachedSearch
import com.canolabs.movieshelf.domain.model.ConnectivityStatus
import com.canolabs.movieshelf.domain.model.DataError
import com.canolabs.movieshelf.domain.model.Movie

/**
 * MVI contract for the Results screen.
 *
 * This screen merges several independent sources: the Room stream for the query, local title
 * matches, live connectivity and the lifecycle of the network refresh, into one state, and emits
 * navigation as one-off actions. A pure reducer keeps those combinations (e.g. "offline, nothing
 * cached, but saved movies match") explicit and testable. The other screens stay plain MVVM.
 */
object ResultsContract {

    data class UiState(
        val query: String,
        val content: Content,
        /** A request for this query is running, whatever content is currently shown. */
        val isRefreshing: Boolean,
        /**
         * Persistent notice shown above content-bearing states ([Content.Movies], [Content.Empty],
         * [Content.LocalMatches]). Full-screen states carry their own explanation instead.
         */
        val notice: Notice?,
        val lastUpdatedAt: Long?,
    ) {
        /**
         * The pull-to-refresh indicator is only used for refreshes of healthy cached content; while a
         * notice or full-screen state is shown, that element carries the retry progress instead.
         */
        val showPullRefreshIndicator: Boolean
            get() = isRefreshing && notice == null &&
                (content is Content.Movies || content == Content.Empty)
    }

    sealed interface Content {
        /** Nothing cached for this query yet and a request is running or about to start. */
        data object Loading : Content

        /** TMDB results for this query, as persisted in Room. */
        data class Movies(val movies: List<Movie>) : Content

        /** A successful TMDB response with no matches. */
        data object Empty : Content

        /**
         * Nothing cached for this query and TMDB is unavailable, but saved movies match the title.
         * These are not TMDB results and are never written to the query's cache.
         */
        data class LocalMatches(val movies: List<Movie>) : Content

        /** Nothing cached, TMDB unavailable and no saved movie matches. [reason] explains why. */
        data class Unavailable(val reason: Notice) : Content

        /** A request failed for a reason other than connectivity, with nothing cached. */
        data class Error(val error: DataError) : Content

        /**
         * Nothing cached, no request running and no recorded failure, e.g. a request cancelled before
         * it completed. Never shown as loading, because nothing would end it.
         */
        data object NotLoaded : Content
    }

    /** Why the shown content may be incomplete or out of date. */
    sealed interface Notice {
        /** The platform reports no usable network. No claim beyond that is made. */
        data object Offline : Notice

        /** A remote search was skipped while offline and connectivity has since returned. */
        data object BackOnline : Notice

        /** The last request failed (timeout, unreachable server, HTTP error…). */
        data class RefreshFailed(val error: DataError) : Notice
    }

    sealed interface UiIntent {
        /**
         * Explicit user request. Sends a request unless a fresh connectivity read still reports the
         * device offline, in which case [UiAction.ShowStillOffline] is emitted instead.
         */
        data object Retry : UiIntent
        data class MovieSelected(val movieId: Long) : UiIntent
        data object SearchBarSelected : UiIntent
    }

    sealed interface UiAction {
        data class OpenMovieDetail(val movieId: Long, val posterUrl: String?) : UiAction
        data class OpenSearchEntry(val query: String) : UiAction

        /** Retry was tapped while the device is still offline: short, transient feedback. */
        data object ShowStillOffline : UiAction
    }
}

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
    )
}
