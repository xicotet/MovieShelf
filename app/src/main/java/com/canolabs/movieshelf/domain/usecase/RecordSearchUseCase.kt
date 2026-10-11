package com.canolabs.movieshelf.domain.usecase

import com.canolabs.movieshelf.domain.model.SearchQuery
import com.canolabs.movieshelf.domain.repository.SearchRepository

/** Adds a submitted query to recent searches when no remote request is made for it. */
interface RecordSearchUseCase {
    suspend operator fun invoke(query: SearchQuery)
}

class RecordSearchUseCaseImpl(
    private val repository: SearchRepository,
) : RecordSearchUseCase {
    override suspend fun invoke(query: SearchQuery) = repository.recordSearch(query)
}
