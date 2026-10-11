package com.canolabs.movieshelf.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MovieDetailsDto(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String,
    @SerialName("overview") val overview: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    @SerialName("runtime") val runtime: Int? = null,
    @SerialName("tagline") val tagline: String? = null,
    @SerialName("genres") val genres: List<GenreDto> = emptyList(),
)

@Serializable
data class GenreDto(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
)
