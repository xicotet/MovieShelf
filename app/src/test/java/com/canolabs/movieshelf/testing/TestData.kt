package com.canolabs.movieshelf.testing

import com.canolabs.movieshelf.data.remote.dto.MovieDto
import com.canolabs.movieshelf.domain.model.Movie
import com.canolabs.movieshelf.domain.model.ResultsRequest
import com.canolabs.movieshelf.domain.model.SearchQuery

fun query(text: String): SearchQuery = requireNotNull(SearchQuery.from(text))

/** A typed-search [ResultsRequest] for [text]. */
fun search(text: String): ResultsRequest = ResultsRequest.Search(query(text))

fun movieDto(id: Long, title: String = "Movie $id", posterPath: String? = "/poster$id.jpg") = MovieDto(
    id = id,
    title = title,
    overview = "Overview $id",
    releaseDate = "2001-02-03",
    posterPath = posterPath,
    backdropPath = null,
    voteAverage = 7.5,
    voteCount = 100,
)

fun movie(id: Long, title: String = "Movie $id") = Movie(
    id = id,
    title = title,
    overview = "Overview $id",
    releaseDate = "2001-02-03",
    posterUrl = null,
    voteAverage = 7.5,
    voteCount = 100,
)
