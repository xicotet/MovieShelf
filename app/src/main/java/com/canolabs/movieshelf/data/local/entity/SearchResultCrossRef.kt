package com.canolabs.movieshelf.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

private const val TABLE_SEARCH_RESULTS = "search_results"
private const val COLUMN_QUERY_KEY = "query_key"
private const val COLUMN_MOVIE_ID = "movie_id"
private const val COLUMN_POSITION = "position"

// Columns of the parent tables referenced by the foreign keys.
private const val PARENT_COLUMN_QUERY_KEY = "query_key"
private const val PARENT_COLUMN_MOVIE_ID = "id"

/**
 * Junction between a query and a movie in its result set, with the movie's position in the API
 * response so ordering is stable.
 *
 * - Deleting a query cascades to its memberships.
 * - A movie cannot be deleted while any query still references it (RESTRICT); unreferenced movies
 *   are pruned explicitly after a result set is replaced.
 */
@Entity(
    tableName = TABLE_SEARCH_RESULTS,
    primaryKeys = [COLUMN_QUERY_KEY, COLUMN_MOVIE_ID],
    foreignKeys = [
        ForeignKey(
            entity = SearchQueryEntity::class,
            parentColumns = [PARENT_COLUMN_QUERY_KEY],
            childColumns = [COLUMN_QUERY_KEY],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MovieEntity::class,
            parentColumns = [PARENT_COLUMN_MOVIE_ID],
            childColumns = [COLUMN_MOVIE_ID],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = [COLUMN_MOVIE_ID])],
)
data class SearchResultCrossRef(
    @ColumnInfo(name = COLUMN_QUERY_KEY) val queryKey: String,
    @ColumnInfo(name = COLUMN_MOVIE_ID) val movieId: Long,
    @ColumnInfo(name = COLUMN_POSITION) val position: Int,
)
