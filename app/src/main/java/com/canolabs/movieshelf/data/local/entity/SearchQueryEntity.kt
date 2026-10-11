package com.canolabs.movieshelf.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

private const val TABLE_SEARCH_QUERIES = "search_queries"
private const val COLUMN_QUERY_KEY = "query_key"
private const val COLUMN_DISPLAY_QUERY = "display_query"
private const val COLUMN_LAST_SEARCHED_AT = "last_searched_at"
private const val COLUMN_LAST_REFRESHED_AT = "last_refreshed_at"
private const val COLUMN_LOADED_PAGES = "loaded_pages"
private const val COLUMN_TOTAL_PAGES = "total_pages"
private const val COLUMN_IN_HISTORY = "in_history"

/**
 * One row per normalized query. Doubles as the recent-search history (ordered by
 * [lastSearchedAt]) and as the owner of the query's cached result set.
 *
 * [lastRefreshedAt] is `null` until a request for the query has succeeded, so a recent search
 * without cached results is distinguishable from a cached empty result.
 *
 * Pagination: [loadedPages] TMDB pages are stored for the query (0 until page 1 succeeds) and
 * [totalPages] is TMDB's `total_pages` for it, or `null` when unknown (rows migrated from v1).
 *
 * [inHistory] controls whether the row appears in recent searches. Removing a recent search only
 * clears it, so the query's cached results (which cascade from this row) are kept; the Discover
 * result set is stored with it cleared.
 */
@Entity(
    tableName = TABLE_SEARCH_QUERIES,
    indices = [Index(value = [COLUMN_LAST_SEARCHED_AT])],
)
data class SearchQueryEntity(
    @PrimaryKey
    @ColumnInfo(name = COLUMN_QUERY_KEY) val queryKey: String,
    @ColumnInfo(name = COLUMN_DISPLAY_QUERY) val displayQuery: String,
    @ColumnInfo(name = COLUMN_LAST_SEARCHED_AT) val lastSearchedAt: Long,
    @ColumnInfo(name = COLUMN_LAST_REFRESHED_AT) val lastRefreshedAt: Long?,
    @ColumnInfo(name = COLUMN_LOADED_PAGES, defaultValue = "0") val loadedPages: Int = 0,
    @ColumnInfo(name = COLUMN_TOTAL_PAGES) val totalPages: Int? = null,
    @ColumnInfo(name = COLUMN_IN_HISTORY, defaultValue = "1") val inHistory: Boolean = true,
)
