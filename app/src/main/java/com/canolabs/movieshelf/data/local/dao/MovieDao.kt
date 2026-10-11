package com.canolabs.movieshelf.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.canolabs.movieshelf.data.local.entity.MovieEntity
import com.canolabs.movieshelf.data.local.entity.MovieSummaryUpdate
import com.canolabs.movieshelf.data.local.entity.toSummaryUpdate
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MovieDao {

    @Query("SELECT * FROM movies WHERE id = :movieId")
    abstract fun observeMovie(movieId: Long): Flow<MovieEntity?>

    /**
     * Saved movies whose title contains [escapedPattern] (escaped for LIKE with `\`). LIKE is
     * case-insensitive (plain SQLite folds ASCII only; Android's build also folds other letters), but
     * accents are significant. Ordered deterministically: most-voted first, then by title and ID.
     * Read-only: it never touches query result membership.
     */
    @Query(
        """
        SELECT * FROM movies
        WHERE title LIKE '%' || :escapedPattern || '%' ESCAPE '\'
        ORDER BY vote_count DESC, title COLLATE NOCASE ASC, id ASC
        LIMIT :limit
        """,
    )
    abstract fun observeTitleMatches(escapedPattern: String, limit: Int): Flow<List<MovieEntity>>

    /** Inserts new movies or refreshes summary columns of existing ones, keeping detail columns. */
    @Transaction
    open suspend fun upsertSummaries(movies: List<MovieEntity>) {
        insertIgnoringExisting(movies)
        updateSummaries(movies.map { it.toSummaryUpdate() })
    }

    /** The detail endpoint returns every column, so a full upsert is correct here. */
    @Upsert
    abstract suspend fun upsertDetails(movie: MovieEntity)

    @Query("DELETE FROM movies WHERE id NOT IN (SELECT movie_id FROM search_results)")
    abstract suspend fun deleteUnreferenced()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertIgnoringExisting(movies: List<MovieEntity>)

    @Update(entity = MovieEntity::class)
    protected abstract suspend fun updateSummaries(movies: List<MovieSummaryUpdate>)
}
