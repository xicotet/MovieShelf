package com.canolabs.movieshelf.data.repository

import com.canolabs.movieshelf.data.remote.datasource.RemoteResult
import com.canolabs.movieshelf.domain.model.DataError
import com.canolabs.movieshelf.domain.model.RefreshResult
import com.canolabs.movieshelf.testing.FakeMovieLocalDataSource
import com.canolabs.movieshelf.testing.FakeMovieRemoteDataSource
import com.canolabs.movieshelf.testing.movieDto
import com.canolabs.movieshelf.testing.query
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRepositoryImplTest {

    private val local = FakeMovieLocalDataSource()
    private val remote = FakeMovieRemoteDataSource()
    private var now = 1_000L
    private val repository = SearchRepositoryImpl(local, remote, clock = { now++ })

    @Test
    fun `persists mapped remote results before reporting success`() = runTest {
        val batman = query("Batman")
        remote.searchResponses["Batman"] = RemoteResult.Success(listOf(movieDto(1, "Batman Begins")))
        remote.beforeSearchResponse = {
            // While the request is in flight nothing has been cached for the query yet.
            assertFalse(repository.observeResults(batman).first().hasCachedResult)
        }

        val result = repository.refresh(batman)

        assertEquals(RefreshResult.Success, result)
        assertEquals(listOf("recordSearch:batman", "replaceSearchResults:batman"), local.writes)
        val cached = repository.observeResults(batman).first()
        assertTrue(cached.hasCachedResult)
        assertEquals(listOf("Batman Begins"), cached.movies.map { it.title })
        assertEquals("https://image.tmdb.org/t/p/w500/poster1.jpg", cached.movies.single().posterUrl)
    }

    @Test
    fun `keeps cached results available when a refresh fails`() = runTest {
        val alien = query("Alien")
        remote.searchResponses["Alien"] = RemoteResult.Success(listOf(movieDto(1), movieDto(2)))
        repository.refresh(alien)

        remote.searchResponses["Alien"] = RemoteResult.Failure(DataError.Network)
        val result = repository.refresh(alien)

        assertEquals(RefreshResult.Failure(DataError.Network), result)
        val cached = repository.observeResults(alien).first()
        assertEquals(listOf(1L, 2L), cached.movies.map { it.id })
        assertTrue(cached.hasCachedResult)
    }

    @Test
    fun `failed refresh preserves the previous refresh time and membership`() = runTest {
        val alien = query("Alien")
        remote.searchResponses["Alien"] = RemoteResult.Success(listOf(movieDto(1)))
        repository.refresh(alien)
        val before = repository.observeResults(alien).first()

        remote.searchResponses["Alien"] = RemoteResult.Failure(DataError.Server)
        repository.refresh(alien)

        assertEquals(before, repository.observeResults(alien).first())
    }

    @Test
    fun `reports an error instead of an empty result when nothing is cached`() = runTest {
        val dune = query("Dune")
        remote.searchResponses["Dune"] = RemoteResult.Failure(DataError.Network)

        val result = repository.refresh(dune)

        assertEquals(RefreshResult.Failure(DataError.Network), result)
        val cached = repository.observeResults(dune).first()
        assertFalse(cached.hasCachedResult)
        assertTrue(cached.movies.isEmpty())
    }

    @Test
    fun `successful empty response replaces previous results for the query`() = runTest {
        val heat = query("Heat")
        remote.searchResponses["Heat"] = RemoteResult.Success(listOf(movieDto(1), movieDto(2)))
        repository.refresh(heat)

        remote.searchResponses["Heat"] = RemoteResult.Success(emptyList())
        val result = repository.refresh(heat)

        assertEquals(RefreshResult.Success, result)
        val cached = repository.observeResults(heat).first()
        assertTrue(cached.hasCachedResult)
        assertTrue(cached.movies.isEmpty())
    }

    @Test
    fun `different queries keep isolated result sets`() = runTest {
        remote.searchResponses["Batman"] = RemoteResult.Success(listOf(movieDto(1), movieDto(2)))
        remote.searchResponses["Alien"] = RemoteResult.Success(listOf(movieDto(3)))
        repository.refresh(query("Batman"))
        repository.refresh(query("Alien"))

        remote.searchResponses["Alien"] = RemoteResult.Success(emptyList())
        repository.refresh(query("Alien"))

        assertEquals(listOf(1L, 2L), repository.observeResults(query("Batman")).first().movies.map { it.id })
        assertTrue(repository.observeResults(query("Alien")).first().movies.isEmpty())
    }

    @Test
    fun `replacing a query's results keeps every movie in the local catalogue`() = runTest {
        remote.searchResponses["Batman"] = RemoteResult.Success(listOf(movieDto(1), movieDto(2)))
        remote.searchResponses["Dark Knight"] = RemoteResult.Success(listOf(movieDto(2)))
        repository.refresh(query("Batman"))
        repository.refresh(query("Dark Knight"))

        remote.searchResponses["Batman"] = RemoteResult.Success(emptyList())
        repository.refresh(query("Batman"))

        assertEquals(listOf(2L), repository.observeResults(query("Dark Knight")).first().movies.map { it.id })
        assertTrue(repository.observeResults(query("Batman")).first().movies.isEmpty())
        // No longer referenced by any query, but still saved for offline title search.
        assertNotNull(local.movie(1))
    }

    @Test
    fun `queries differing only in case and spacing share one cache entry`() = runTest {
        remote.searchResponses["Blade Runner"] = RemoteResult.Success(listOf(movieDto(78)))
        repository.refresh(query("  Blade   Runner "))

        val cached = repository.observeResults(query("blade runner")).first()

        assertEquals(listOf(78L), cached.movies.map { it.id })
    }

    @Test
    fun `preserves the api result order`() = runTest {
        val ids = listOf(42L, 7L, 99L, 3L)
        remote.searchResponses["Order"] = RemoteResult.Success(ids.map { movieDto(it) })

        repository.refresh(query("Order"))

        assertEquals(ids, repository.observeResults(query("Order")).first().movies.map { it.id })
    }

    @Test
    fun `stores a movie returned twice in one response only once`() = runTest {
        remote.searchResponses["Dup"] = RemoteResult.Success(listOf(movieDto(1), movieDto(2), movieDto(1)))

        repository.refresh(query("Dup"))

        assertEquals(listOf(1L, 2L), repository.observeResults(query("Dup")).first().movies.map { it.id })
    }

    @Test
    fun `records a recent search even when the request fails`() = runTest {
        repository.refresh(query("Offline query"))

        val recent = repository.observeRecentSearches(limit = 10).first()

        assertEquals(listOf("Offline query"), recent.map { it.query.text })
        assertFalse(repository.observeResults(query("Offline query")).first().hasCachedResult)
    }

    @Test
    fun `recent searches are ordered by recency without duplicates`() = runTest {
        repository.refresh(query("Alien"))
        repository.refresh(query("Batman"))
        repository.refresh(query("alien"))

        val recent = repository.observeRecentSearches(limit = 10).first()

        assertEquals(listOf("alien", "batman"), recent.map { it.query.key })
    }

    @Test
    fun `a late response for an older query only updates that query`() = runTest {
        val gate = CompletableDeferred<Unit>()
        remote.searchResponses["Alien"] = RemoteResult.Success(listOf(movieDto(1)))
        remote.searchResponses["Batman"] = RemoteResult.Success(listOf(movieDto(2)))
        remote.beforeSearchResponse = { if (it == "Alien") gate.await() }

        val olderRequest = async { repository.refresh(query("Alien")) }
        runCurrent()
        repository.refresh(query("Batman"))
        gate.complete(Unit)
        olderRequest.await()

        assertEquals(listOf(2L), repository.observeResults(query("Batman")).first().movies.map { it.id })
        assertEquals(listOf(1L), repository.observeResults(query("Alien")).first().movies.map { it.id })
        // Recency reflects submission order, not completion order.
        assertEquals(
            listOf("batman", "alien"),
            repository.observeRecentSearches(limit = 10).first().map { it.query.key },
        )
    }

    @Test
    fun `local title matches are case-insensitive, ordered and never cached as a query result`() = runTest {
        remote.searchResponses["Batman"] = RemoteResult.Success(
            listOf(movieDto(1, "Batman Begins"), movieDto(2, "The Batman"), movieDto(3, "Joker")),
        )
        repository.refresh(query("Batman"))

        // "BAT" was never searched remotely: matching is case-insensitive and purely local.
        val matches = repository.observeLocalTitleMatches(query("BAT"), limit = 10).first()

        assertEquals(listOf("Batman Begins", "The Batman"), matches.map { it.title })
        assertFalse(repository.observeResults(query("BAT")).first().hasCachedResult)
        assertEquals(listOf("recordSearch:batman", "replaceSearchResults:batman"), local.writes)
    }

    @Test
    fun `blank local searches return nothing`() = runTest {
        remote.searchResponses["Batman"] = RemoteResult.Success(listOf(movieDto(1, "Batman Begins")))
        repository.refresh(query("Batman"))

        assertTrue(local.observeTitleMatches("   ", limit = 10).first().isEmpty())
    }

    @Test
    fun `recording an offline search adds history without marking results as cached`() = runTest {
        repository.recordSearch(query("Heat"))

        assertEquals(listOf("heat"), repository.observeRecentSearches(limit = 10).first().map { it.query.key })
        assertFalse(repository.observeResults(query("Heat")).first().hasCachedResult)
        assertTrue(remote.searchCalls.isEmpty())
    }

    // region Pagination

    private fun pageOf(range: IntRange) = range.map { movieDto(it.toLong()) }

    @Test
    fun `page 1 stores tmdb's pagination metadata`() = runTest {
        remote.searchResponses["Alien"] = RemoteResult.Success(pageOf(1..3))
        remote.totalPages["Alien"] = 4

        repository.refresh(query("Alien"))

        val cached = repository.observeResults(query("Alien")).first()
        assertEquals(1, cached.loadedPages)
        assertEquals(4, cached.totalPages)
        assertTrue(cached.canLoadMore)
    }

    @Test
    fun `next page is appended in order, deduplicated and persisted with its metadata`() = runTest {
        remote.searchResponses["Alien"] = RemoteResult.Success(pageOf(1..3))
        remote.totalPages["Alien"] = 3
        remote.pageResponses["Alien" to 2] = RemoteResult.Success(listOf(movieDto(3), movieDto(4), movieDto(5), movieDto(4)))
        repository.refresh(query("Alien"))

        val result = repository.loadNextPage(query("Alien"))

        assertEquals(RefreshResult.Success, result)
        assertEquals(listOf("Alien" to 1, "Alien" to 2), remote.pageCalls)
        assertEquals("appendSearchResultsPage:alien:2:applied", local.writes.last())
        val cached = repository.observeResults(query("Alien")).first()
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), cached.movies.map { it.id })
        assertEquals(2, cached.loadedPages)
        assertEquals(3, cached.totalPages)
    }

    @Test
    fun `no request is made when tmdb reports no further page`() = runTest {
        remote.searchResponses["Alien"] = RemoteResult.Success(pageOf(1..3))
        remote.totalPages["Alien"] = 1
        repository.refresh(query("Alien"))

        val result = repository.loadNextPage(query("Alien"))

        assertEquals(RefreshResult.Success, result)
        assertEquals(listOf("Alien" to 1), remote.pageCalls)
        assertFalse(repository.observeResults(query("Alien")).first().canLoadMore)
    }

    @Test
    fun `twenty results do not imply another page`() = runTest {
        remote.searchResponses["Alien"] = RemoteResult.Success(pageOf(1..20))
        remote.totalPages["Alien"] = 1

        repository.refresh(query("Alien"))

        assertFalse(repository.observeResults(query("Alien")).first().canLoadMore)
    }

    @Test
    fun `a failed next page keeps every loaded page`() = runTest {
        remote.searchResponses["Alien"] = RemoteResult.Success(pageOf(1..3))
        remote.totalPages["Alien"] = 3
        repository.refresh(query("Alien"))
        val before = repository.observeResults(query("Alien")).first()

        val result = repository.loadNextPage(query("Alien"))

        assertEquals(RefreshResult.Failure(DataError.Network), result)
        assertEquals(before, repository.observeResults(query("Alien")).first())
    }

    @Test
    fun `refreshing page 1 resets pagination and drops later pages`() = runTest {
        remote.searchResponses["Alien"] = RemoteResult.Success(pageOf(1..2))
        remote.totalPages["Alien"] = 5
        remote.pageResponses["Alien" to 2] = RemoteResult.Success(pageOf(3..4))
        repository.refresh(query("Alien"))
        repository.loadNextPage(query("Alien"))

        remote.searchResponses["Alien"] = RemoteResult.Success(pageOf(7..8))
        remote.totalPages["Alien"] = 2
        repository.refresh(query("Alien"))

        val cached = repository.observeResults(query("Alien")).first()
        assertEquals(listOf(7L, 8L), cached.movies.map { it.id })
        assertEquals(1, cached.loadedPages)
        assertEquals(2, cached.totalPages)
    }

    @Test
    fun `a late page response after a page 1 refresh cannot corrupt the results`() = runTest {
        remote.searchResponses["Alien"] = RemoteResult.Success(pageOf(1..2))
        remote.totalPages["Alien"] = 5
        repository.refresh(query("Alien"))
        repository.loadNextPage(query("Alien").also { remote.pageResponses["Alien" to 2] = RemoteResult.Success(pageOf(3..4)) })
        // Page 3 is requested, and while it is in flight page 1 is refreshed.
        val gate = CompletableDeferred<Unit>()
        remote.pageResponses["Alien" to 3] = RemoteResult.Success(pageOf(5..6))
        remote.beforeSearchResponse = { gate.await() }
        val latePage = async { repository.loadNextPage(query("Alien")) }
        runCurrent()
        remote.beforeSearchResponse = {}
        remote.searchResponses["Alien"] = RemoteResult.Success(pageOf(9..10))
        repository.refresh(query("Alien"))
        gate.complete(Unit)
        latePage.await()

        val cached = repository.observeResults(query("Alien")).first()
        assertEquals(listOf(9L, 10L), cached.movies.map { it.id })
        assertEquals(1, cached.loadedPages)
        assertEquals("appendSearchResultsPage:alien:3:rejected", local.writes.last())
        // The late page's movies still joined the catalogue.
        assertNotNull(local.movie(5))
    }

    @Test
    fun `a page for one query never touches another query`() = runTest {
        remote.searchResponses["Alien"] = RemoteResult.Success(pageOf(1..2))
        remote.totalPages["Alien"] = 2
        remote.pageResponses["Alien" to 2] = RemoteResult.Success(pageOf(3..4))
        remote.searchResponses["Heat"] = RemoteResult.Success(pageOf(10..11))
        repository.refresh(query("Alien"))
        repository.refresh(query("Heat"))

        repository.loadNextPage(query("Alien"))

        assertEquals(listOf(10L, 11L), repository.observeResults(query("Heat")).first().movies.map { it.id })
        assertEquals(1, repository.observeResults(query("Heat")).first().loadedPages)
    }

    @Test
    fun `local title search finds movies saved by other queries and later pages`() = runTest {
        remote.searchResponses["Alien"] = RemoteResult.Success(listOf(movieDto(1, "Alien")))
        remote.totalPages["Alien"] = 2
        remote.pageResponses["Alien" to 2] = RemoteResult.Success(listOf(movieDto(2, "Alien: Romulus")))
        remote.searchResponses["Ridley"] = RemoteResult.Success(listOf(movieDto(3, "Alien Covenant")))
        repository.refresh(query("Alien"))
        repository.loadNextPage(query("Alien"))
        repository.refresh(query("Ridley"))
        // Alien's page-1 refresh no longer contains the page-2 movie.
        remote.searchResponses["Alien"] = RemoteResult.Success(listOf(movieDto(1, "Alien")))
        repository.refresh(query("Alien"))

        val matches = repository.observeLocalTitleMatches(query("alien"), limit = 10).first().map { it.id }.toSet()

        assertEquals(setOf(1L, 2L, 3L), matches)
        // Local matching never wrote anything to an unsearched query's results.
        assertFalse(repository.observeResults(query("ali")).first().hasCachedResult)
    }

    @Test
    fun `a cached empty result stays distinct from a cache miss`() = runTest {
        remote.searchResponses["Nothing"] = RemoteResult.Success(emptyList())
        remote.totalPages["Nothing"] = 0
        repository.refresh(query("Nothing"))

        val cachedEmpty = repository.observeResults(query("Nothing")).first()
        val miss = repository.observeResults(query("Never searched")).first()

        assertTrue(cachedEmpty.hasCachedResult)
        assertFalse(cachedEmpty.canLoadMore)
        assertFalse(miss.hasCachedResult)
    }

    // endregion
}
