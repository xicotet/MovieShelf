package com.canolabs.movieshelf.domain.usecase

import com.canolabs.movieshelf.domain.model.SearchQuery
import com.canolabs.movieshelf.domain.repository.SearchRepository

/** Removes one entry from search history, keeping its cached results and the saved movies. */
interface RemoveRecentSearchUseCase {
    suspend operator fun invoke(query: SearchQuery)
}

class RemoveRecentSearchUseCaseImpl(
    private val repository: SearchRepository,
) : RemoveRecentSearchUseCase {
    override suspend fun invoke(query: SearchQuery) = repository.removeRecentSearch(query)
}
