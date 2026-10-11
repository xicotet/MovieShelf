package com.canolabs.movieshelf.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.canolabs.movieshelf.data.local.entity.MovieEntity

/**
 * One row of the query/result join. A query with no results yields a single row whose [movie] is
 * `null`, so query metadata and result membership are always read in one consistent statement.
 */
data class SearchResultRow(
    @ColumnInfo(name = "query_key") val queryKey: String,
    @ColumnInfo(name = "last_refreshed_at") val lastRefreshedAt: Long?,
    @Embedded val movie: MovieEntity?,
)
