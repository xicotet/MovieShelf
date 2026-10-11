package com.canolabs.movieshelf.domain.usecase

import com.canolabs.movieshelf.domain.model.RefreshResult
import com.canolabs.movieshelf.domain.model.SearchQuery
import com.canolabs.movieshelf.domain.repository.SearchRepository

/** Submits [SearchQuery] to the API and persists the result; content is observed separately. */
interface RefreshSearchResultsUseCase {
    suspend operator fun invoke(query: SearchQuery): RefreshResult
}

class RefreshSearchResultsUseCaseImpl(
    private val repository: SearchRepository,
) : RefreshSearchResultsUseCase {
    override suspend fun invoke(query: SearchQuery): RefreshResult = repository.refresh(query)
}
