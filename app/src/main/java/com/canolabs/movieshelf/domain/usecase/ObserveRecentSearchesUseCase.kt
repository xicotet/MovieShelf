package com.canolabs.movieshelf.domain.usecase

import com.canolabs.movieshelf.domain.model.RecentSearch
import com.canolabs.movieshelf.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow

interface ObserveRecentSearchesUseCase {
    operator fun invoke(): Flow<List<RecentSearch>>
}

class ObserveRecentSearchesUseCaseImpl(
    private val repository: SearchRepository,
) : ObserveRecentSearchesUseCase {
    override fun invoke(): Flow<List<RecentSearch>> = repository.observeRecentSearches(MAX_RECENT_SEARCHES)

    private companion object {
        const val MAX_RECENT_SEARCHES = 8
    }
}
