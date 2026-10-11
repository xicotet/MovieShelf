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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the real Room SQL for local title search on an in-memory database. */
@RunWith(AndroidJUnit4::class)
class MovieTitleSearchDaoTest {

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

    private fun movie(id: Long, title: String, votes: Int?) = MovieEntity(
        id = id,
        title = title,
        overview = null,
        releaseDate = null,
        posterPath = null,
        backdropPath = null,
        voteAverage = null,
        voteCount = votes,
        updatedAt = 1L,
    )

    private suspend fun cache(queryKey: String, vararg movies: MovieEntity) {
        local.recordSearch(queryKey, queryKey, searchedAt = 1L)
        local.replaceSearchResults(queryKey, queryKey, movies.toList(), refreshedAt = 2L)
    }

    @Test
    fun matchesTitlesCaseInsensitivelyAcrossQueriesInDeterministicOrder() = runTest {
        cache("batman", movie(1, "Batman Begins", 100), movie(2, "The Batman", 300), movie(3, "Joker", 500))
        cache("lego", movie(4, "The LEGO Batman Movie", 300), movie(5, "Lego Movie", 50))

        val titles = local.observeTitleMatches("bAtMaN", limit = 10).first().map { it.title }

        // Most-voted first; equal votes ordered by title, case-insensitively.
        assertEquals(listOf("The Batman", "The LEGO Batman Movie", "Batman Begins"), titles)
    }

    @Test
    fun treatsLikeWildcardsInInputLiterally() = runTest {
        cache("wolf", movie(1, "100% Wolf", 10), movie(2, "1000 Wolves", 10), movie(3, "a_b", 10), movie(4, "axb", 10))

        assertEquals(listOf("100% Wolf"), local.observeTitleMatches("100%", 10).first().map { it.title })
        assertEquals(listOf("a_b"), local.observeTitleMatches("a_b", 10).first().map { it.title })
    }

    @Test
    fun blankInputAndLimitAreHandled() = runTest {
        cache("star", movie(1, "Star Wars", 3), movie(2, "Star Trek", 2), movie(3, "Lone Star", 1))

        assertTrue(local.observeTitleMatches("   ", 10).first().isEmpty())
        assertEquals(listOf("Star Wars", "Star Trek"), local.observeTitleMatches("star", 2).first().map { it.title })
    }

    @Test
    fun accentsAreNotFolded() = runTest {
        cache("amelie", movie(1, "Amélie", 10))

        assertEquals(1, local.observeTitleMatches("amélie", 10).first().size)
        // Diacritics are significant: a documented limitation of plain LIKE matching.
        assertTrue(local.observeTitleMatches("amelie", 10).first().isEmpty())
    }

    @Test
    fun localSearchNeverCreatesOrChangesQueryResults() = runTest {
        cache("batman", movie(1, "Batman Begins", 100))

        local.observeTitleMatches("bat", 10).first()

        assertNull(local.observeSearchResults("bat").first())
        assertEquals(listOf(1L), local.observeSearchResults("batman").first()?.movies?.map { it.id })
    }
}
