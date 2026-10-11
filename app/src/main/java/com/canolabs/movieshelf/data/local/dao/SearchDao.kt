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
        SELECT q.query_key, q.last_refreshed_at, q.loaded_pages, q.total_pages, m.*
        FROM search_queries AS q
        LEFT JOIN search_results AS r ON r.query_key = q.query_key
        LEFT JOIN movies AS m ON m.id = r.movie_id
        WHERE q.query_key = :queryKey
        ORDER BY r.position ASC
        """,
    )
    abstract fun observeResultRows(queryKey: String): Flow<List<SearchResultRow>>

    @Query("SELECT * FROM search_queries WHERE in_history = 1 ORDER BY last_searched_at DESC LIMIT :limit")
    abstract fun observeRecent(limit: Int): Flow<List<SearchQueryEntity>>

    /** Hides a query from recent searches; its cached results are untouched. */
    @Query("UPDATE search_queries SET in_history = 0 WHERE query_key = :queryKey")
    abstract suspend fun removeFromHistory(queryKey: String)

    /** Creates the query row if needed and marks it as the most recently searched (back in history). */
    @Transaction
    open suspend fun recordSearch(queryKey: String, displayQuery: String, searchedAt: Long) {
        insertIfAbsent(SearchQueryEntity(queryKey, displayQuery, searchedAt, lastRefreshedAt = null))
        updateLastSearched(queryKey, displayQuery, searchedAt)
    }

    /**
     * Replaces the whole result membership of one query with TMDB page 1 and resets its pagination
     * (later pages of the previous result set are dropped). Other queries are untouched.
     */
    @Transaction
    open suspend fun replaceResults(
        queryKey: String,
        results: List<SearchResultCrossRef>,
        refreshedAt: Long,
        totalPages: Int,
    ) {
        deleteResults(queryKey)
        insertResults(results)
        updateRefreshedFirstPage(queryKey, refreshedAt, totalPages)
    }

    /**
     * Appends TMDB page [page] to a query's results, after its existing ones and without duplicates.
     *
     * Applied only if exactly `page - 1` pages are stored, so a duplicate request for the same page,
     * a page arriving after a page-1 refresh reset the query, or an invalid page number all leave
     * the stored results untouched. The referenced movies must already be saved.
     *
     * @return whether the page was applied.
     */
    @Transaction
    open suspend fun appendPage(queryKey: String, page: Int, movieIds: List<Long>, totalPages: Int): Boolean {
        val loadedPages = loadedPages(queryKey) ?: return false
        if (page < 2 || loadedPages != page - 1) return false
        val existing = resultMovieIds(queryKey).toSet()
        val firstPosition = (maxPosition(queryKey) ?: -1) + 1
        val newIds = movieIds.distinct().filterNot { it in existing }
        insertResults(newIds.mapIndexed { index, id -> SearchResultCrossRef(queryKey, id, firstPosition + index) })
        updatePagination(queryKey, page, totalPages)
        return true
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertIfAbsent(query: SearchQueryEntity)

    @Query(
        "UPDATE search_queries SET display_query = :displayQuery, last_searched_at = :searchedAt, " +
            "in_history = 1 WHERE query_key = :queryKey",
    )
    protected abstract suspend fun updateLastSearched(queryKey: String, displayQuery: String, searchedAt: Long)

    @Query(
        "UPDATE search_queries SET last_refreshed_at = :refreshedAt, loaded_pages = 1, " +
            "total_pages = :totalPages WHERE query_key = :queryKey",
    )
    protected abstract suspend fun updateRefreshedFirstPage(queryKey: String, refreshedAt: Long, totalPages: Int)

    @Query("UPDATE search_queries SET loaded_pages = :loadedPages, total_pages = :totalPages WHERE query_key = :queryKey")
    protected abstract suspend fun updatePagination(queryKey: String, loadedPages: Int, totalPages: Int)

    /** Pagination of the query, or `null` if it was never recorded. */
    @Query("SELECT loaded_pages, total_pages FROM search_queries WHERE query_key = :queryKey")
    abstract suspend fun pagination(queryKey: String): QueryPagination?

    @Query("SELECT loaded_pages FROM search_queries WHERE query_key = :queryKey")
    protected abstract suspend fun loadedPages(queryKey: String): Int?

    @Query("SELECT movie_id FROM search_results WHERE query_key = :queryKey")
    protected abstract suspend fun resultMovieIds(queryKey: String): List<Long>

    @Query("SELECT MAX(position) FROM search_results WHERE query_key = :queryKey")
    protected abstract suspend fun maxPosition(queryKey: String): Int?

    @Query("DELETE FROM search_results WHERE query_key = :queryKey")
    protected abstract suspend fun deleteResults(queryKey: String)

    @Insert
    protected abstract suspend fun insertResults(results: List<SearchResultCrossRef>)
}
