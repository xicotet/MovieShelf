package com.canolabs.movieshelf.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.canolabs.movieshelf.data.local.database.MovieShelfDatabase
import com.canolabs.movieshelf.data.local.datasource.MovieLocalDataSourceImpl
import com.canolabs.movieshelf.data.local.entity.MovieEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Real Room SQL and transactions for result pages and the local movie catalogue. */
@RunWith(AndroidJUnit4::class)
class PaginationDaoTest {

    private lateinit var database: MovieShelfDatabase
    private lateinit var local: MovieLocalDataSourceImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MovieShelfDatabase::class.java,
        ).build()
        local = MovieLocalDataSourceImpl(database, database.searchDao(), database.movieDao())
    }

    @After
    fun tearDown() = database.close()

    private fun movies(vararg ids: Long) = ids.map {
        MovieEntity(
            id = it,
            title = "Alien $it",
            overview = null,
            releaseDate = null,
            posterPath = null,
            backdropPath = null,
            voteAverage = null,
            voteCount = null,
            updatedAt = 1L,
        )
    }

    private suspend fun firstPage(vararg ids: Long, totalPages: Int) {
        local.recordSearch("alien", "Alien", searchedAt = 1L)
        local.replaceSearchResults("alien", "Alien", movies(*ids), refreshedAt = 2L, totalPages = totalPages)
    }

    private suspend fun stored() = local.observeSearchResults("alien").first()!!

    @Test
    fun appendsTheNextPageInOrderWithoutDuplicates() = runTest {
        firstPage(1, 2, 3, totalPages = 3)

        val applied = local.appendSearchResultsPage("alien", page = 2, movies = movies(3, 4, 5, 4), totalPages = 3)

        assertTrue(applied)
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), stored().movies.map { it.id })
        assertEquals(2, stored().loadedPages)
        assertEquals(3, stored().totalPages)
    }

    @Test
    fun rejectsDuplicateSkippedAndFirstPagesWithoutTouchingResults() = runTest {
        firstPage(1, 2, totalPages = 4)
        local.appendSearchResultsPage("alien", page = 2, movies = movies(3), totalPages = 4)
        val before = stored()

        assertFalse(local.appendSearchResultsPage("alien", page = 2, movies = movies(9), totalPages = 4))
        assertFalse(local.appendSearchResultsPage("alien", page = 4, movies = movies(9), totalPages = 4))
        assertFalse(local.appendSearchResultsPage("alien", page = 1, movies = movies(9), totalPages = 4))
        assertFalse(local.appendSearchResultsPage("never-searched", page = 2, movies = movies(9), totalPages = 4))

        assertEquals(before, stored())
        // Rejected pages' movies still join the local catalogue.
        assertNotNull(database.movieDao().observeMovie(9).first())
    }

    @Test
    fun refreshingPageOneResetsPaginationAndDropsLaterPagesButKeepsTheMovies() = runTest {
        firstPage(1, 2, totalPages = 3)
        local.appendSearchResultsPage("alien", page = 2, movies = movies(3, 4), totalPages = 3)

        local.replaceSearchResults("alien", "Alien", movies(7), refreshedAt = 3L, totalPages = 2)

        assertEquals(listOf(7L), stored().movies.map { it.id })
        assertEquals(1, stored().loadedPages)
        assertEquals(2, stored().totalPages)
        listOf(1L, 2L, 3L, 4L).forEach { assertNotNull("movie $it", database.movieDao().observeMovie(it).first()) }
        // Movies from the dropped page are still found by offline title search.
        val titles = local.observeTitleMatches("alien", limit = 50).first().map { it.id }.toSet()
        assertEquals(setOf(1L, 2L, 3L, 4L, 7L), titles)
    }

    @Test
    fun evictingAQueryRemovesItsResultsButNotTheCatalogue() = runTest {
        firstPage(1, 2, totalPages = 1)

        // Deleting the query cascades to its result rows; movies have no reason to be deleted.
        database.openHelper.writableDatabase.execSQL("DELETE FROM search_queries WHERE query_key = 'alien'")

        assertNull(local.observeSearchResults("alien").first())
        assertNotNull(database.movieDao().observeMovie(1).first())
        assertEquals(2, local.observeTitleMatches("alien", limit = 50).first().size)
    }

    @Test
    fun anEmptyFirstPageIsACachedEmptyResultWithNoFurtherPages() = runTest {
        firstPage(totalPages = 0)

        val cached = stored()
        assertTrue(cached.movies.isEmpty())
        assertNotNull(cached.lastRefreshedAt)
        assertEquals(1, cached.loadedPages)
        assertEquals(0, cached.totalPages)
    }
}
