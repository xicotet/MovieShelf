package com.canolabs.movieshelf.domain.usecase

import com.canolabs.movieshelf.domain.model.RefreshResult
import com.canolabs.movieshelf.domain.model.ResultsRequest
import com.canolabs.movieshelf.domain.repository.SearchRepository

/** Submits a [ResultsRequest] to the API and persists the result; content is observed separately. */
interface RefreshSearchResultsUseCase {
    suspend operator fun invoke(request: ResultsRequest): RefreshResult
}

class RefreshSearchResultsUseCaseImpl(
    private val repository: SearchRepository,
) : RefreshSearchResultsUseCase {
    override suspend fun invoke(request: ResultsRequest): RefreshResult = repository.refresh(request)
}
