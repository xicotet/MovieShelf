package com.canolabs.movieshelf.domain.usecase

import com.canolabs.movieshelf.domain.model.CachedSearch
import com.canolabs.movieshelf.domain.model.SearchQuery
import com.canolabs.movieshelf.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow

interface ObserveSearchResultsUseCase {
    operator fun invoke(query: SearchQuery): Flow<CachedSearch>
}

class ObserveSearchResultsUseCaseImpl(
    private val repository: SearchRepository,
) : ObserveSearchResultsUseCase {
    override fun invoke(query: SearchQuery): Flow<CachedSearch> = repository.observeResults(query)
}
