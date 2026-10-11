package com.canolabs.movieshelf.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.canolabs.movieshelf.data.local.database.MIGRATION_1_2
import com.canolabs.movieshelf.data.local.database.MIGRATION_2_3
import com.canolabs.movieshelf.data.local.database.MovieShelfDatabase
import com.canolabs.movieshelf.data.local.datasource.MovieLocalDataSourceImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Migrates a real v1 database file (built from the exported 1.json schema) to v2. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), MovieShelfDatabase::class.java)

    private val dbName = "migration-test.db"

    @Test
    fun migrates1To2KeepingAllDataAndAddingPagination() = runTest {
        helper.createDatabase(dbName, 1).apply {
            execSQL("INSERT INTO movies (id, title, updated_at) VALUES (1, 'Alien', 1), (2, 'Aliens', 1)")
            execSQL("INSERT INTO search_queries (query_key, display_query, last_searched_at, last_refreshed_at) VALUES ('alien', 'Alien', 10, 20)")
            execSQL("INSERT INTO search_queries (query_key, display_query, last_searched_at, last_refreshed_at) VALUES ('offline', 'Offline', 30, NULL)")
            execSQL("INSERT INTO search_results (query_key, movie_id, position) VALUES ('alien', 2, 0), ('alien', 1, 1)")
            close()
        }

        // Validates the migrated schema against the exported 2.json.
        helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2).close()

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MovieShelfDatabase::class.java,
            dbName,
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
        try {
            val local = MovieLocalDataSourceImpl(database, database.searchDao(), database.movieDao())

            val alien = local.observeSearchResults("alien").first()!!
            assertEquals(listOf(2L, 1L), alien.movies.map { it.id })
            assertEquals(20L, alien.lastRefreshedAt)
            // A refreshed v1 query holds one page; TMDB's total is unknown until the next refresh.
            assertEquals(1, alien.loadedPages)
            assertNull(alien.totalPages)

            val neverRefreshed = local.observeSearchResults("offline").first()!!
            assertEquals(0, neverRefreshed.loadedPages)
            assertNull(neverRefreshed.lastRefreshedAt)
            assertFalse(local.observeRecentSearches(10).first().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun migrates2To3KeepingEveryQueryInHistory() = runTest {
        helper.createDatabase(dbName, 2).apply {
            execSQL("INSERT INTO movies (id, title, updated_at) VALUES (1, 'Alien', 1)")
            execSQL(
                "INSERT INTO search_queries (query_key, display_query, last_searched_at, last_refreshed_at, loaded_pages, total_pages) " +
                    "VALUES ('alien', 'Alien', 10, 20, 2, 5), ('heat', 'Heat', 30, NULL, 0, NULL)",
            )
            execSQL("INSERT INTO search_results (query_key, movie_id, position) VALUES ('alien', 1, 0)")
            close()
        }

        // Validates the migrated schema against the exported 3.json.
        helper.runMigrationsAndValidate(dbName, 3, true, MIGRATION_2_3).close()

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MovieShelfDatabase::class.java,
            dbName,
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
        try {
            val local = MovieLocalDataSourceImpl(database, database.searchDao(), database.movieDao())
            assertEquals(listOf("heat", "alien"), local.observeRecentSearches(10).first().map { it.queryKey })
            val alien = local.observeSearchResults("alien").first()!!
            assertEquals(listOf(1L), alien.movies.map { it.id })
            assertEquals(2, alien.loadedPages)
            assertEquals(5, alien.totalPages)
        } finally {
            database.close()
        }
    }

    @Test
    fun migratesAllTheWayFrom1To3() {
        helper.createDatabase(dbName, 1).apply {
            execSQL("INSERT INTO search_queries (query_key, display_query, last_searched_at, last_refreshed_at) VALUES ('alien', 'Alien', 10, 20)")
            close()
        }

        helper.runMigrationsAndValidate(dbName, 3, true, MIGRATION_1_2, MIGRATION_2_3).use { db ->
            db.query("SELECT loaded_pages, in_history FROM search_queries WHERE query_key = 'alien'").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
                assertEquals(1, cursor.getInt(1))
            }
        }
    }
}
