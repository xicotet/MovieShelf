package com.canolabs.movieshelf.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.canolabs.movieshelf.data.local.entity.SearchQueryEntity
import com.canolabs.movieshelf.data.local.entity.SearchResultCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SearchDao {

    /**
     * Query metadata joined with its ordered results, read atomically. Emits an empty list when the
     * query has never been recorded.
     */
    @Query(
        """
        SELECT q.query_key, q.last_refreshed_at, m.*
        FROM search_queries AS q
        LEFT JOIN search_results AS r ON r.query_key = q.query_key
        LEFT JOIN movies AS m ON m.id = r.movie_id
        WHERE q.query_key = :queryKey
        ORDER BY r.position ASC
        """,
    )
    abstract fun observeResultRows(queryKey: String): Flow<List<SearchResultRow>>

    @Query("SELECT * FROM search_queries ORDER BY last_searched_at DESC LIMIT :limit")
    abstract fun observeRecent(limit: Int): Flow<List<SearchQueryEntity>>

    /** Creates the query row if needed and marks it as the most recently searched. */
    @Transaction
    open suspend fun recordSearch(queryKey: String, displayQuery: String, searchedAt: Long) {
        insertIfAbsent(SearchQueryEntity(queryKey, displayQuery, searchedAt, lastRefreshedAt = null))
        updateLastSearched(queryKey, displayQuery, searchedAt)
    }

    /** Replaces the whole result membership of one query. Other queries are untouched. */
    @Transaction
    open suspend fun replaceResults(queryKey: String, results: List<SearchResultCrossRef>, refreshedAt: Long) {
        deleteResults(queryKey)
        insertResults(results)
        updateLastRefreshed(queryKey, refreshedAt)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertIfAbsent(query: SearchQueryEntity)

    @Query(
        "UPDATE search_queries SET display_query = :displayQuery, last_searched_at = :searchedAt " +
            "WHERE query_key = :queryKey",
    )
    protected abstract suspend fun updateLastSearched(queryKey: String, displayQuery: String, searchedAt: Long)

    @Query("UPDATE search_queries SET last_refreshed_at = :refreshedAt WHERE query_key = :queryKey")
    protected abstract suspend fun updateLastRefreshed(queryKey: String, refreshedAt: Long)

    @Query("DELETE FROM search_results WHERE query_key = :queryKey")
    protected abstract suspend fun deleteResults(queryKey: String)

    @Insert
    protected abstract suspend fun insertResults(results: List<SearchResultCrossRef>)
}
