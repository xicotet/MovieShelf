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

/**
 * One row per normalized query. Doubles as the recent-search history (ordered by
 * [lastSearchedAt]) and as the owner of the query's cached result set.
 *
 * [lastRefreshedAt] is `null` until a request for the query has succeeded, so a recent search
 * without cached results is distinguishable from a cached empty result.
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
)
