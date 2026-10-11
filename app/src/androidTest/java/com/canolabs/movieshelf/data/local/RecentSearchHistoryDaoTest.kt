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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Real Room SQL for removing recent searches and keeping non-history result sets out of history. */
@RunWith(AndroidJUnit4::class)
class RecentSearchHistoryDaoTest {

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

    private fun movie(id: Long) = MovieEntity(
        id = id,
        title = "Movie $id",
        overview = null,
        releaseDate = null,
        posterPath = null,
        backdropPath = null,
        voteAverage = null,
        voteCount = null,
        updatedAt = 1L,
    )

    private suspend fun search(key: String, searchedAt: Long, vararg movieIds: Long) {
        local.recordSearch(key, key, searchedAt)
        local.replaceSearchResults(key, key, movieIds.map(::movie), refreshedAt = searchedAt, totalPages = 1)
    }

    private suspend fun recentKeys() = local.observeRecentSearches(10).first().map { it.queryKey }

    @Test
    fun removingARecentSearchKeepsItsCachedResultsAndMovies() = runTest {
        search("alien", searchedAt = 1, 1, 2)
        search("heat", searchedAt = 2, 3)

        local.removeFromHistory("alien")

        assertEquals(listOf("heat"), recentKeys())
        assertEquals(listOf(1L, 2L), local.observeSearchResults("alien").first()!!.movies.map { it.id })
        assertNotNull(database.movieDao().observeMovie(1).first())
        assertEquals(listOf(3L), local.observeSearchResults("heat").first()!!.movies.map { it.id })
    }

    @Test
    fun searchingAgainRestoresARemovedEntry() = runTest {
        search("alien", searchedAt = 1, 1)
        local.removeFromHistory("alien")

        local.recordSearch("alien", "alien", searchedAt = 5)

        assertEquals(listOf("alien"), recentKeys())
    }

    @Test
    fun resultSetsStoredOutsideHistoryNeverAppearInRecentSearches() = runTest {
        search("alien", searchedAt = 1, 1)

        // As stored for Discover: cached results without a history entry.
        local.replaceSearchResults("DISCOVER:popular", "Discover", listOf(movie(9)), refreshedAt = 2, totalPages = 3, inHistory = false)

        assertEquals(listOf("alien"), recentKeys())
        assertEquals(listOf(9L), local.observeSearchResults("DISCOVER:popular").first()!!.movies.map { it.id })
    }
}
