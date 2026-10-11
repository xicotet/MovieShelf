package com.canolabs.movieshelf.di

import com.canolabs.movieshelf.presentation.detail.MovieDetailViewModel
import com.canolabs.movieshelf.presentation.results.ResultsViewModel
import com.canolabs.movieshelf.presentation.searchentry.SearchEntryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { SearchEntryViewModel(observeRecentSearches = get()) }
    viewModel { (query: String) ->
        ResultsViewModel(
            rawQuery = query,
            savedStateHandle = get(),
            observeSearchResults = get(),
            refreshSearchResults = get(),
            observeLocalTitleMatches = get(),
            recordSearch = get(),
            observeConnectivity = get(),
            getConnectivityStatus = get(),
        )
    }
    viewModel { (movieId: Long) ->
        MovieDetailViewModel(
            movieId = movieId,
            savedStateHandle = get(),
            observeMovieDetails = get(),
            refreshMovieDetails = get(),
        )
    }
}
