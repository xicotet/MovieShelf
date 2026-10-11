package com.canolabs.movieshelf.data.mapper

import com.canolabs.movieshelf.data.local.datasource.CachedQueryResults
import com.canolabs.movieshelf.data.local.entity.GENRE_SEPARATOR
import com.canolabs.movieshelf.data.local.entity.MovieEntity
import com.canolabs.movieshelf.data.local.entity.SearchQueryEntity
import com.canolabs.movieshelf.data.remote.api.TmdbApiConfig
import com.canolabs.movieshelf.data.remote.dto.MovieDetailsDto
import com.canolabs.movieshelf.data.remote.dto.MovieDto
import com.canolabs.movieshelf.domain.model.CachedSearch
import com.canolabs.movieshelf.domain.model.Movie
import com.canolabs.movieshelf.domain.model.MovieDetails
import com.canolabs.movieshelf.domain.model.RecentSearch
import com.canolabs.movieshelf.domain.model.ResultsRequest
import com.canolabs.movieshelf.domain.model.SearchQuery

// DTO -> Entity

fun MovieDto.toEntity(updatedAt: Long) = MovieEntity(
    id = id,
    title = title,
    overview = overview.nullIfBlank(),
    releaseDate = releaseDate.nullIfBlank(),
    posterPath = posterPath.nullIfBlank(),
    backdropPath = backdropPath.nullIfBlank(),
    voteAverage = voteAverage,
    voteCount = voteCount,
    updatedAt = updatedAt,
)

/** Maps one result page, dropping duplicate IDs (a movie appears once per query) and keeping order. */
fun List<MovieDto>.toEntities(updatedAt: Long): List<MovieEntity> =
    distinctBy { it.id }.map { it.toEntity(updatedAt) }

fun MovieDetailsDto.toEntity(fetchedAt: Long) = MovieEntity(
    id = id,
    title = title,
    overview = overview.nullIfBlank(),
    releaseDate = releaseDate.nullIfBlank(),
    posterPath = posterPath.nullIfBlank(),
    backdropPath = backdropPath.nullIfBlank(),
    voteAverage = voteAverage,
    voteCount = voteCount,
    runtimeMinutes = runtime?.takeIf { it > 0 },
    tagline = tagline.nullIfBlank(),
    genres = genres.joinToString(GENRE_SEPARATOR) { it.name },
    detailsFetchedAt = fetchedAt,
    updatedAt = fetchedAt,
)

// Entity -> Domain

fun MovieEntity.toDomain() = Movie(
    id = id,
    title = title,
    overview = overview,
    releaseDate = releaseDate,
    posterUrl = posterPath?.let { TmdbApiConfig.IMAGE_BASE_URL + TmdbApiConfig.POSTER_SIZE + it },
    voteAverage = voteAverage,
    voteCount = voteCount,
)

fun MovieEntity.toDomainDetails() = MovieDetails(
    movie = toDomain(),
    backdropUrl = backdropPath?.let { TmdbApiConfig.IMAGE_BASE_URL + TmdbApiConfig.BACKDROP_SIZE + it },
    extras = detailsFetchedAt?.let {
        MovieDetails.Extras(
            runtimeMinutes = runtimeMinutes,
            genres = genres?.split(GENRE_SEPARATOR)?.filter { it.isNotBlank() }.orEmpty(),
            tagline = tagline,
        )
    },
)

fun CachedQueryResults?.toDomain(request: ResultsRequest) = CachedSearch(
    request = request,
    movies = this?.movies?.map { it.toDomain() }.orEmpty(),
    lastRefreshedAt = this?.lastRefreshedAt,
    loadedPages = this?.loadedPages ?: 0,
    totalPages = this?.totalPages,
)

/** Returns `null` for rows whose stored text no longer forms a valid query. */
fun SearchQueryEntity.toDomain(): RecentSearch? =
    SearchQuery.from(displayQuery)?.let { RecentSearch(query = it, lastSearchedAt = lastSearchedAt) }

private fun String?.nullIfBlank(): String? = this?.takeIf { it.isNotBlank() }
