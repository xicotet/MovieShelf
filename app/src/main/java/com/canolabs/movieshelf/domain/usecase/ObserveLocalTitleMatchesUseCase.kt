package com.canolabs.movieshelf.domain.usecase

import com.canolabs.movieshelf.domain.model.Movie
import com.canolabs.movieshelf.domain.model.SearchQuery
import com.canolabs.movieshelf.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow

/** Saved movies whose title matches the query; a local fallback, not a substitute for TMDB search. */
interface ObserveLocalTitleMatchesUseCase {
    operator fun invoke(query: SearchQuery): Flow<List<Movie>>
}

class ObserveLocalTitleMatchesUseCaseImpl(
    private val repository: SearchRepository,
) : ObserveLocalTitleMatchesUseCase {
    override fun invoke(query: SearchQuery): Flow<List<Movie>> =
        repository.observeLocalTitleMatches(query, MAX_LOCAL_MATCHES)

    private companion object {
        const val MAX_LOCAL_MATCHES = 50
    }
}
