package com.canolabs.movieshelf.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.canolabs.movieshelf.data.local.entity.MovieEntity

/** Pagination metadata of one cached query. */
data class QueryPagination(
    @ColumnInfo(name = "loaded_pages") val loadedPages: Int,
    @ColumnInfo(name = "total_pages") val totalPages: Int?,
)

/**
 * One row of the query/result join. A query with no results yields a single row whose [movie] is
 * `null`, so query metadata and result membership are always read in one consistent statement.
 */
data class SearchResultRow(
    @ColumnInfo(name = "query_key") val queryKey: String,
    @ColumnInfo(name = "last_refreshed_at") val lastRefreshedAt: Long?,
    @ColumnInfo(name = "loaded_pages") val loadedPages: Int,
    @ColumnInfo(name = "total_pages") val totalPages: Int?,
    @Embedded val movie: MovieEntity?,
)
