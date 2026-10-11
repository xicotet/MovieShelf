package com.canolabs.movieshelf.presentation.results

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
        /** Pagination of TMDB results; only relevant while [content] is [Content.Movies]. */
        val nextPage: NextPage = NextPage.Unavailable,
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

    /** The "Load more" footer, independent of the initial/page-1 refresh state. */
    sealed interface NextPage {
        /** No further TMDB page exists (or pagination does not apply to the shown content). */
        data object Unavailable : NextPage

        /** Another page exists and can be requested. */
        data object Available : NextPage
        data object Loading : NextPage

        /** The last next-page attempt failed; earlier pages stay visible. */
        data class Failed(val reason: LoadMoreFailure) : NextPage
    }

    sealed interface LoadMoreFailure {
        /** Not attempted: the device is offline. */
        data object Offline : LoadMoreFailure
        data class Error(val error: DataError) : LoadMoreFailure
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

        /** Explicit request for the next TMDB page of the shown results. */
        data object LoadMore : UiIntent
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
