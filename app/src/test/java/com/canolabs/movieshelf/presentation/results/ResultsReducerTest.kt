package com.canolabs.movieshelf.presentation.results

import com.canolabs.movieshelf.domain.model.CachedSearch
import com.canolabs.movieshelf.domain.model.ConnectivityStatus
import com.canolabs.movieshelf.domain.model.DataError
import com.canolabs.movieshelf.domain.model.Movie
import com.canolabs.movieshelf.presentation.results.ResultsContract.Content
import com.canolabs.movieshelf.presentation.results.ResultsContract.Notice
import com.canolabs.movieshelf.testing.movie
import com.canolabs.movieshelf.testing.query
import com.canolabs.movieshelf.testing.search
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultsReducerTest {

    private val miss = CachedSearch(search("Batman"), emptyList(), lastRefreshedAt = null)
    private val cachedEmpty = CachedSearch(search("Batman"), emptyList(), lastRefreshedAt = 5L)
    private val cachedMovies = CachedSearch(search("Batman"), listOf(movie(1)), lastRefreshedAt = 5L)
    private val local = listOf(movie(2))

    private fun reduce(
        cache: CachedSearch?,
        refresh: RefreshState,
        connectivity: ConnectivityStatus = ConnectivityStatus.Validated,
        localMatches: List<Movie> = emptyList(),
    ) = reduceResultsState("Batman", cache, localMatches, connectivity, refresh)

    @Test
    fun `a cache miss with no request running and no failure is not a loading state`() {
        val idle = RefreshState(isRunning = false, lastFailure = null, isAwaitingLoad = false)

        val state = reduce(miss, idle)

        assertEquals(Content.NotLoaded, state.content)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `a cache miss is loading only while a request runs or is about to start`() {
        assertEquals(Content.Loading, reduce(miss, RefreshState()).content)
        assertEquals(Content.Loading, reduce(miss, RefreshState().started()).content)
        assertEquals(Content.NotLoaded, reduce(miss, RefreshState().started().cancelled()).content)
    }

    @Test
    fun `offline with nothing running never shows loading`() {
        val idle = RefreshState(isAwaitingLoad = false)

        assertEquals(Content.Unavailable(Notice.Offline), reduce(miss, idle, ConnectivityStatus.Unavailable).content)
        assertEquals(
            Content.LocalMatches(local),
            reduce(miss, idle, ConnectivityStatus.Unavailable, localMatches = local).content,
        )
    }

    @Test
    fun `a failed refresh of a cached empty result shows the empty state with the notice`() {
        val state = reduce(cachedEmpty, RefreshState().started().failed(DataError.Network))

        assertEquals(Content.Empty, state.content)
        assertEquals(Notice.RefreshFailed(DataError.Network), state.notice)
    }

    @Test
    fun `a cached empty result without any failure has no notice`() {
        val state = reduce(cachedEmpty, RefreshState().started().succeeded())

        assertEquals(Content.Empty, state.content)
        assertNull(state.notice)
    }

    @Test
    fun `no notice is shown while the first refresh of cached content is still running`() {
        val state = reduce(cachedMovies, RefreshState().started())

        assertNull(state.notice)
        assertTrue(state.isRefreshing)
        assertTrue(state.showPullRefreshIndicator)
    }

    @Test
    fun `the failure is kept while a retry runs`() {
        val retrying = RefreshState().started().failed(DataError.Timeout).started()

        val cached = reduce(cachedMovies, retrying)
        assertEquals(Notice.RefreshFailed(DataError.Timeout), cached.notice)
        assertTrue(cached.isRefreshing)
        assertFalse(cached.showPullRefreshIndicator)

        val uncached = reduce(miss, retrying)
        assertEquals(Content.Unavailable(Notice.RefreshFailed(DataError.Timeout)), uncached.content)
        assertTrue(uncached.isRefreshing)
    }

    @Test
    fun `a success clears the previous failure`() {
        val state = reduce(cachedMovies, RefreshState().started().failed(DataError.Network).started().succeeded())

        assertNull(state.notice)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `cancellation keeps the previous failure without recording a new one`() {
        val state = reduce(miss, RefreshState().started().failed(DataError.Network).started().cancelled())

        assertEquals(Content.Unavailable(Notice.RefreshFailed(DataError.Network)), state.content)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `confirmed offline takes precedence over a recorded failure in the notice`() {
        val state = reduce(cachedMovies, RefreshState().started().failed(DataError.Timeout), ConnectivityStatus.Unavailable)

        assertEquals(Notice.Offline, state.notice)
    }

    @Test
    fun `timeout, unreachable server and offline produce distinct explanations`() {
        val timeout = reduce(miss, RefreshState().started().failed(DataError.Timeout))
        val unreachable = reduce(miss, RefreshState().started().failed(DataError.Network))
        val offline = reduce(miss, RefreshState().skippedBecauseOffline(), ConnectivityStatus.Unavailable)

        assertEquals(Content.Unavailable(Notice.RefreshFailed(DataError.Timeout)), timeout.content)
        assertEquals(Content.Unavailable(Notice.RefreshFailed(DataError.Network)), unreachable.content)
        assertEquals(Content.Unavailable(Notice.Offline), offline.content)
    }

    @Test
    fun `after an offline skip, regained connectivity is reported as back online`() {
        val skipped = RefreshState().skippedBecauseOffline()

        assertEquals(Notice.BackOnline, reduce(cachedMovies, skipped, ConnectivityStatus.Validated).notice)
        assertEquals(
            Content.LocalMatches(local),
            reduce(miss, skipped, ConnectivityStatus.Validated, localMatches = local).content,
        )
    }

    @Test
    fun `non-connectivity failures are errors and never fall back to local matches`() {
        val state = reduce(miss, RefreshState().started().failed(DataError.Unauthorized), localMatches = local)

        assertEquals(Content.Error(DataError.Unauthorized), state.content)
        assertNull(state.notice)
    }

    @Test
    fun `cached remote results always win over local matches`() {
        val state = reduce(cachedMovies, RefreshState().skippedBecauseOffline(), ConnectivityStatus.Unavailable, local)

        assertEquals(Content.Movies(listOf(movie(1))), state.content)
    }

    @Test
    fun `local matches are not shown during a normal online request`() {
        val state = reduce(miss, RefreshState().started(), localMatches = local)

        assertEquals(Content.Loading, state.content)
    }

    @Test
    fun `load more follows tmdb pagination and only applies to tmdb results`() {
        val idle = RefreshState(isAwaitingLoad = false)
        val morePages = cachedMovies.copy(loadedPages = 1, totalPages = 3)
        val lastPage = cachedMovies.copy(loadedPages = 3, totalPages = 3)

        assertEquals(ResultsContract.NextPage.Available, reduce(morePages, idle).nextPage)
        assertEquals(ResultsContract.NextPage.Unavailable, reduce(lastPage, idle).nextPage)
        assertEquals(ResultsContract.NextPage.Unavailable, reduce(cachedEmpty, idle).nextPage)
        assertEquals(
            ResultsContract.NextPage.Unavailable,
            reduce(miss, RefreshState().skippedBecauseOffline(), ConnectivityStatus.Unavailable, local).nextPage,
        )
    }

    @Test
    fun `next page loading and failures are separate from the refresh state`() {
        val morePages = cachedMovies.copy(loadedPages = 1, totalPages = 3)
        val idle = RefreshState(isAwaitingLoad = false)

        val loading = reduceResultsState("Batman", morePages, emptyList(), ConnectivityStatus.Validated, idle, NextPageState(isLoading = true))
        val failed = reduceResultsState(
            "Batman", morePages, emptyList(), ConnectivityStatus.Validated, idle,
            NextPageState(failure = ResultsContract.LoadMoreFailure.Error(DataError.Timeout)),
        )

        assertEquals(ResultsContract.NextPage.Loading, loading.nextPage)
        assertFalse(loading.isRefreshing)
        assertEquals(Content.Movies(listOf(movie(1))), failed.content)
        assertEquals(ResultsContract.NextPage.Failed(ResultsContract.LoadMoreFailure.Error(DataError.Timeout)), failed.nextPage)
        assertNull(failed.notice)
    }
}
