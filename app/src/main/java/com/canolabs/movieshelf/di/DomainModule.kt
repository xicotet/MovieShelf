package com.canolabs.movieshelf.di

import com.canolabs.movieshelf.domain.usecase.GetConnectivityStatusUseCase
import com.canolabs.movieshelf.domain.usecase.GetConnectivityStatusUseCaseImpl
import com.canolabs.movieshelf.domain.usecase.LoadNextResultsPageUseCase
import com.canolabs.movieshelf.domain.usecase.LoadNextResultsPageUseCaseImpl
import com.canolabs.movieshelf.domain.usecase.ObserveConnectivityUseCase
import com.canolabs.movieshelf.domain.usecase.ObserveConnectivityUseCaseImpl
import com.canolabs.movieshelf.domain.usecase.ObserveLocalTitleMatchesUseCase
import com.canolabs.movieshelf.domain.usecase.ObserveLocalTitleMatchesUseCaseImpl
import com.canolabs.movieshelf.domain.usecase.ObserveMovieDetailsUseCase
import com.canolabs.movieshelf.domain.usecase.ObserveMovieDetailsUseCaseImpl
import com.canolabs.movieshelf.domain.usecase.ObserveRecentSearchesUseCase
import com.canolabs.movieshelf.domain.usecase.ObserveRecentSearchesUseCaseImpl
import com.canolabs.movieshelf.domain.usecase.ObserveSearchResultsUseCase
import com.canolabs.movieshelf.domain.usecase.ObserveSearchResultsUseCaseImpl
import com.canolabs.movieshelf.domain.usecase.ObserveSettingsUseCase
import com.canolabs.movieshelf.domain.usecase.ObserveSettingsUseCaseImpl
import com.canolabs.movieshelf.domain.usecase.RecordSearchUseCase
import com.canolabs.movieshelf.domain.usecase.RecordSearchUseCaseImpl
import com.canolabs.movieshelf.domain.usecase.RefreshMovieDetailsUseCase
import com.canolabs.movieshelf.domain.usecase.RefreshMovieDetailsUseCaseImpl
import com.canolabs.movieshelf.domain.usecase.RefreshSearchResultsUseCase
import com.canolabs.movieshelf.domain.usecase.RefreshSearchResultsUseCaseImpl
import com.canolabs.movieshelf.domain.usecase.RemoveRecentSearchUseCase
import com.canolabs.movieshelf.domain.usecase.RemoveRecentSearchUseCaseImpl
import com.canolabs.movieshelf.domain.usecase.SetTextSizeUseCase
import com.canolabs.movieshelf.domain.usecase.SetTextSizeUseCaseImpl
import com.canolabs.movieshelf.domain.usecase.SetThemeModeUseCase
import com.canolabs.movieshelf.domain.usecase.SetThemeModeUseCaseImpl
import org.koin.dsl.module

val domainModule = module {
    factory<ObserveSearchResultsUseCase> { ObserveSearchResultsUseCaseImpl(get()) }
    factory<RefreshSearchResultsUseCase> { RefreshSearchResultsUseCaseImpl(get()) }
    factory<ObserveRecentSearchesUseCase> { ObserveRecentSearchesUseCaseImpl(get()) }
    factory<ObserveMovieDetailsUseCase> { ObserveMovieDetailsUseCaseImpl(get()) }
    factory<RefreshMovieDetailsUseCase> { RefreshMovieDetailsUseCaseImpl(get()) }
    factory<RecordSearchUseCase> { RecordSearchUseCaseImpl(get()) }
    factory<LoadNextResultsPageUseCase> { LoadNextResultsPageUseCaseImpl(get()) }
    factory<RemoveRecentSearchUseCase> { RemoveRecentSearchUseCaseImpl(get()) }
    factory<ObserveSettingsUseCase> { ObserveSettingsUseCaseImpl(get()) }
    factory<SetThemeModeUseCase> { SetThemeModeUseCaseImpl(get()) }
    factory<SetTextSizeUseCase> { SetTextSizeUseCaseImpl(get()) }
    factory<ObserveLocalTitleMatchesUseCase> { ObserveLocalTitleMatchesUseCaseImpl(get()) }
    factory<ObserveConnectivityUseCase> { ObserveConnectivityUseCaseImpl(get()) }
    factory<GetConnectivityStatusUseCase> { GetConnectivityStatusUseCaseImpl(get()) }
}
