package com.canolabs.movieshelf.di

import com.canolabs.movieshelf.domain.model.ResultsRequest
import com.canolabs.movieshelf.presentation.detail.MovieDetailViewModel
import com.canolabs.movieshelf.presentation.results.ResultsViewModel
import com.canolabs.movieshelf.presentation.searchentry.SearchEntryViewModel
import com.canolabs.movieshelf.presentation.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { SearchEntryViewModel(observeRecentSearches = get(), removeRecentSearch = get()) }
    viewModel { SettingsViewModel(observeSettings = get(), setThemeMode = get(), setTextSize = get()) }
    viewModel { (request: ResultsRequest) ->
        ResultsViewModel(
            request = request,
            savedStateHandle = get(),
            observeSearchResults = get(),
            refreshSearchResults = get(),
            loadNextResultsPage = get(),
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
