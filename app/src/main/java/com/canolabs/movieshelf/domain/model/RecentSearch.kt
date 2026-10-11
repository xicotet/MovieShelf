package com.canolabs.movieshelf.domain.model

data class RecentSearch(
    val query: SearchQuery,
    val lastSearchedAt: Long,
)
