package com.canolabs.movieshelf.di

import com.canolabs.movieshelf.domain.usecase.GetConnectivityStatusUseCase
import com.canolabs.movieshelf.domain.usecase.GetConnectivityStatusUseCaseImpl
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
import com.canolabs.movieshelf.domain.usecase.RefreshMovieDetailsUseCase
import com.canolabs.movieshelf.domain.usecase.RefreshMovieDetailsUseCaseImpl
import com.canolabs.movieshelf.domain.usecase.RefreshSearchResultsUseCase
import com.canolabs.movieshelf.domain.usecase.RecordSearchUseCase
import com.canolabs.movieshelf.domain.usecase.RecordSearchUseCaseImpl
import com.canolabs.movieshelf.domain.usecase.RefreshSearchResultsUseCaseImpl
import org.koin.dsl.module

val domainModule = module {
    factory<ObserveSearchResultsUseCase> { ObserveSearchResultsUseCaseImpl(get()) }
    factory<RefreshSearchResultsUseCase> { RefreshSearchResultsUseCaseImpl(get()) }
    factory<ObserveRecentSearchesUseCase> { ObserveRecentSearchesUseCaseImpl(get()) }
    factory<ObserveMovieDetailsUseCase> { ObserveMovieDetailsUseCaseImpl(get()) }
    factory<RefreshMovieDetailsUseCase> { RefreshMovieDetailsUseCaseImpl(get()) }
    factory<RecordSearchUseCase> { RecordSearchUseCaseImpl(get()) }
    factory<ObserveLocalTitleMatchesUseCase> { ObserveLocalTitleMatchesUseCaseImpl(get()) }
    factory<ObserveConnectivityUseCase> { ObserveConnectivityUseCaseImpl(get()) }
    factory<GetConnectivityStatusUseCase> { GetConnectivityStatusUseCaseImpl(get()) }
}
