package com.canolabs.movieshelf.domain.model

/**
 * A movie as it appears in a search result list.
 *
 * @property releaseDate ISO-8601 date (`yyyy-MM-dd`) as provided by the API, or `null` when unknown.
 */
data class Movie(
    val id: Long,
    val title: String,
    val overview: String?,
    val releaseDate: String?,
    val posterUrl: String?,
    val voteAverage: Double?,
    val voteCount: Int?,
)
