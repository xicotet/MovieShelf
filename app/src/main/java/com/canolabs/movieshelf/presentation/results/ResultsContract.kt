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

    /**
     * Everything the Results screen renders.
     *
     * [showPullRefreshIndicator] is derived: the pull-to-refresh indicator is only used for refreshes
     * of healthy cached content; while a notice or full-screen state is shown, that element carries
     * the retry progress instead.
     *
     * @property query The submitted query text; empty for Discover.
     * @property content What fills the screen below the search bar.
     * @property isRefreshing A request for this query is running, whatever content is shown.
     * @property notice Persistent notice above content-bearing states ([Content.Movies],
     *   [Content.Empty], [Content.LocalMatches]); full-screen states carry their own explanation.
     * @property lastUpdatedAt When the shown TMDB results were last refreshed, if they are cached.
     * @property nextPage The "Load more" footer; only relevant while [content] is [Content.Movies].
     * @property isDiscover TMDB's popular-movies discovery rather than a typed search.
     */
    data class UiState(
        val query: String,
        val content: Content,
        val isRefreshing: Boolean,
        val notice: Notice?,
        val lastUpdatedAt: Long?,
        val nextPage: NextPage = NextPage.Unavailable,
        val isDiscover: Boolean = false,
    ) {
        val showPullRefreshIndicator: Boolean
            get() = isRefreshing && notice == null &&
                (content is Content.Movies || content == Content.Empty)
    }

    /**
     * What fills the screen below the search bar.
     *
     * - [Loading]: nothing cached for this query yet, and a request is running or about to start.
     * - [Movies]: TMDB results for this query, as persisted in Room.
     * - [Empty]: a successful TMDB response with no matches.
     * - [LocalMatches]: nothing cached and TMDB unavailable, but saved movies match the title. These
     *   are not TMDB results and are never written to the query's cache.
     * - [Unavailable]: nothing cached, TMDB unavailable and no saved movie matches; `reason` says why.
     * - [Error]: a request failed for a reason other than connectivity, with nothing cached.
     * - [NotLoaded]: nothing cached, no request running and no recorded failure (e.g. a request
     *   cancelled before it completed). Never shown as loading, because nothing would end it.
     */
    sealed interface Content {
        data object Loading : Content
        data class Movies(val movies: List<Movie>) : Content
        data object Empty : Content
        data class LocalMatches(val movies: List<Movie>) : Content
        data class Unavailable(val reason: Notice) : Content
        data class Error(val error: DataError) : Content
        data object NotLoaded : Content
    }

    /**
     * Why the shown content may be incomplete or out of date.
     *
     * - [Offline]: the platform reports no usable network; no claim beyond that is made.
     * - [BackOnline]: a remote search was skipped while offline, and connectivity has since returned.
     * - [RefreshFailed]: the last request failed (timeout, unreachable server, HTTP error…).
     */
    sealed interface Notice {
        data object Offline : Notice
        data object BackOnline : Notice
        data class RefreshFailed(val error: DataError) : Notice
    }

    /**
     * The "Load more" footer, independent of the initial and page-1 refresh state.
     *
     * - [Unavailable]: no further TMDB page exists, or pagination does not apply to the content.
     * - [Available]: another page exists and can be requested.
     * - [Loading]: the next page is being fetched; earlier pages stay visible.
     * - [Failed]: the last attempt failed; earlier pages stay visible and it can be retried.
     */
    sealed interface NextPage {
        data object Unavailable : NextPage
        data object Available : NextPage
        data object Loading : NextPage
        data class Failed(val reason: LoadMoreFailure) : NextPage
    }

    /**
     * Why the last "Load more" did not add a page.
     *
     * - [Offline]: not attempted, because the device is offline.
     * - [Error]: the request was sent and failed.
     */
    sealed interface LoadMoreFailure {
        data object Offline : LoadMoreFailure
        data class Error(val error: DataError) : LoadMoreFailure
    }

    /**
     * User intents, dispatched through the ViewModel's single `onIntent` entry point.
     *
     * - [Retry]: explicit request; sends a request unless a fresh connectivity read still reports the
     *   device offline, in which case [UiAction.ShowStillOffline] is emitted instead.
     * - [LoadMore]: explicit request for the next TMDB page of the shown results.
     * - [MovieSelected]: a result was tapped.
     * - [SearchBarSelected]: the search bar was tapped, to edit or start a new search.
     */
    sealed interface UiIntent {
        data object Retry : UiIntent
        data object LoadMore : UiIntent
        data class MovieSelected(val movieId: Long) : UiIntent
        data object SearchBarSelected : UiIntent
    }

    /**
     * One-off effects for the screen to perform.
     *
     * - [OpenMovieDetail]: navigate to a movie; `posterUrl` lets the shared poster draw immediately.
     * - [OpenSearchEntry]: open the search entry, prefilled with `query`.
     * - [ShowStillOffline]: Retry was tapped while still offline; short, transient feedback.
     */
    sealed interface UiAction {
        data class OpenMovieDetail(val movieId: Long, val posterUrl: String?) : UiAction
        data class OpenSearchEntry(val query: String) : UiAction
        data object ShowStillOffline : UiAction
    }
}
