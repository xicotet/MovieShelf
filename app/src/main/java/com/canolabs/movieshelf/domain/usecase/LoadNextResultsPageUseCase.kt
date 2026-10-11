package com.canolabs.movieshelf.domain.usecase

import com.canolabs.movieshelf.domain.model.RefreshResult
import com.canolabs.movieshelf.domain.model.SearchQuery
import com.canolabs.movieshelf.domain.repository.SearchRepository

/** Appends the next TMDB result page for a query; the results themselves are observed separately. */
interface LoadNextResultsPageUseCase {
    suspend operator fun invoke(query: SearchQuery): RefreshResult
}

class LoadNextResultsPageUseCaseImpl(
    private val repository: SearchRepository,
) : LoadNextResultsPageUseCase {
    override suspend fun invoke(query: SearchQuery): RefreshResult = repository.loadNextPage(query)
}
