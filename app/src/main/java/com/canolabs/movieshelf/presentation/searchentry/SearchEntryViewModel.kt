package com.canolabs.movieshelf.presentation.searchentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canolabs.movieshelf.domain.model.RecentSearch
import com.canolabs.movieshelf.domain.model.SearchQuery
import com.canolabs.movieshelf.domain.usecase.ObserveRecentSearchesUseCase
import com.canolabs.movieshelf.domain.usecase.RemoveRecentSearchUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchEntryUiState(
    val recentSearches: List<RecentSearch> = emptyList(),
    val isLoaded: Boolean = false,
)

/**
 * Exposes persisted recent searches. Typing never touches the network: the editable text lives in
 * the screen (`rememberSaveable`) and a query is only handed to navigation once it is submitted.
 */
class SearchEntryViewModel(
    observeRecentSearches: ObserveRecentSearchesUseCase,
    private val removeRecentSearch: RemoveRecentSearchUseCase,
) : ViewModel() {

    val uiState: StateFlow<SearchEntryUiState> = observeRecentSearches()
        .map { SearchEntryUiState(recentSearches = it, isLoaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchEntryUiState())

    /** Validates and normalizes user input; returns `null` for blank text, which is not submittable. */
    fun validate(text: String): SearchQuery? = SearchQuery.from(text)

    /** Removes one entry from history; the list updates from Room. Cached results are kept. */
    fun removeRecent(query: SearchQuery) {
        viewModelScope.launch { removeRecentSearch(query) }
    }
}
