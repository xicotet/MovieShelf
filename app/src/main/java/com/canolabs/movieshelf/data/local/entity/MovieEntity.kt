package com.canolabs.movieshelf.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Separator used to store genre names in a single [MovieEntity.genres] column. */
const val GENRE_SEPARATOR = "|"

private const val TABLE_MOVIES = "movies"
private const val COLUMN_ID = "id"
private const val COLUMN_TITLE = "title"
private const val COLUMN_OVERVIEW = "overview"
private const val COLUMN_RELEASE_DATE = "release_date"
private const val COLUMN_POSTER_PATH = "poster_path"
private const val COLUMN_BACKDROP_PATH = "backdrop_path"
private const val COLUMN_VOTE_AVERAGE = "vote_average"
private const val COLUMN_VOTE_COUNT = "vote_count"
private const val COLUMN_RUNTIME_MINUTES = "runtime_minutes"
private const val COLUMN_TAGLINE = "tagline"
private const val COLUMN_GENRES = "genres"
private const val COLUMN_DETAILS_FETCHED_AT = "details_fetched_at"
private const val COLUMN_UPDATED_AT = "updated_at"

/**
 * One canonical row per TMDB movie, shared by every query whose results contain it.
 *
 * Summary columns are refreshed by every search; the detail columns ([runtimeMinutes], [tagline],
 * [genres]) are only written by the detail endpoint and are marked by [detailsFetchedAt].
 */
@Entity(tableName = TABLE_MOVIES)
data class MovieEntity(
    @PrimaryKey
    @ColumnInfo(name = COLUMN_ID) val id: Long,
    @ColumnInfo(name = COLUMN_TITLE) val title: String,
    @ColumnInfo(name = COLUMN_OVERVIEW) val overview: String?,
    @ColumnInfo(name = COLUMN_RELEASE_DATE) val releaseDate: String?,
    @ColumnInfo(name = COLUMN_POSTER_PATH) val posterPath: String?,
    @ColumnInfo(name = COLUMN_BACKDROP_PATH) val backdropPath: String?,
    @ColumnInfo(name = COLUMN_VOTE_AVERAGE) val voteAverage: Double?,
    @ColumnInfo(name = COLUMN_VOTE_COUNT) val voteCount: Int?,
    @ColumnInfo(name = COLUMN_RUNTIME_MINUTES) val runtimeMinutes: Int? = null,
    @ColumnInfo(name = COLUMN_TAGLINE) val tagline: String? = null,
    /** Genre names separated by [GENRE_SEPARATOR]. */
    @ColumnInfo(name = COLUMN_GENRES) val genres: String? = null,
    @ColumnInfo(name = COLUMN_DETAILS_FETCHED_AT) val detailsFetchedAt: Long? = null,
    @ColumnInfo(name = COLUMN_UPDATED_AT) val updatedAt: Long,
)

/**
 * Partial update of the columns a search response provides. Used so that refreshing search results
 * never wipes detail columns fetched earlier for the same movie.
 */
data class MovieSummaryUpdate(
    @ColumnInfo(name = COLUMN_ID) val id: Long,
    @ColumnInfo(name = COLUMN_TITLE) val title: String,
    @ColumnInfo(name = COLUMN_OVERVIEW) val overview: String?,
    @ColumnInfo(name = COLUMN_RELEASE_DATE) val releaseDate: String?,
    @ColumnInfo(name = COLUMN_POSTER_PATH) val posterPath: String?,
    @ColumnInfo(name = COLUMN_BACKDROP_PATH) val backdropPath: String?,
    @ColumnInfo(name = COLUMN_VOTE_AVERAGE) val voteAverage: Double?,
    @ColumnInfo(name = COLUMN_VOTE_COUNT) val voteCount: Int?,
    @ColumnInfo(name = COLUMN_UPDATED_AT) val updatedAt: Long,
)

fun MovieEntity.toSummaryUpdate() = MovieSummaryUpdate(
    id = id,
    title = title,
    overview = overview,
    releaseDate = releaseDate,
    posterPath = posterPath,
    backdropPath = backdropPath,
    voteAverage = voteAverage,
    voteCount = voteCount,
    updatedAt = updatedAt,
)
