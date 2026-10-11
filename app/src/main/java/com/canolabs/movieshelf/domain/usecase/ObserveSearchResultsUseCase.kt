package com.canolabs.movieshelf.domain.usecase

import com.canolabs.movieshelf.domain.model.CachedSearch
import com.canolabs.movieshelf.domain.model.ResultsRequest
import com.canolabs.movieshelf.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow

interface ObserveSearchResultsUseCase {
    operator fun invoke(request: ResultsRequest): Flow<CachedSearch>
}

class ObserveSearchResultsUseCaseImpl(
    private val repository: SearchRepository,
) : ObserveSearchResultsUseCase {
    override fun invoke(request: ResultsRequest): Flow<CachedSearch> = repository.observeResults(request)
}
