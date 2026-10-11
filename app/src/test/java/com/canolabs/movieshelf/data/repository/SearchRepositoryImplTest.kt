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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun `movies shared by several queries survive replacing one of them`() = runTest {
        remote.searchResponses["Batman"] = RemoteResult.Success(listOf(movieDto(1), movieDto(2)))
        remote.searchResponses["Dark Knight"] = RemoteResult.Success(listOf(movieDto(2)))
        repository.refresh(query("Batman"))
        repository.refresh(query("Dark Knight"))

        remote.searchResponses["Batman"] = RemoteResult.Success(emptyList())
        repository.refresh(query("Batman"))

        assertEquals(listOf(2L), repository.observeResults(query("Dark Knight")).first().movies.map { it.id })
        assertNull(local.movie(1))
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
}
