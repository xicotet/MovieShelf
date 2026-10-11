package com.canolabs.movieshelf.presentation.detail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canolabs.movieshelf.domain.model.DataError
import com.canolabs.movieshelf.domain.model.MovieDetails
import com.canolabs.movieshelf.domain.usecase.ObserveMovieDetailsUseCase
import com.canolabs.movieshelf.domain.usecase.RefreshMovieDetailsUseCase
import com.canolabs.movieshelf.presentation.common.RefreshStatus
import com.canolabs.movieshelf.presentation.common.toRefreshStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MovieDetailUiState(
    val movieId: Long,
    /** `true` until the local cache has been read once. */
    val isLoading: Boolean = true,
    /** Locally cached information; `null` if the movie is not cached. */
    val details: MovieDetails? = null,
    val isRefreshing: Boolean = false,
    val refreshError: DataError? = null,
) {
    /** Runtime/genres/tagline have never been fetched and cannot be shown right now. */
    val extrasUnavailable: Boolean get() = details != null && details.extras == null && refreshError != null
    val isMissing: Boolean get() = !isLoading && details == null && !isRefreshing
}

/**
 * Renders the cached movie immediately (everything a search result provides works offline) and
 * enriches it with the detail endpoint. Fetched details are persisted to Room first and reach the
 * screen through the same observation stream.
 */
class MovieDetailViewModel(
    private val movieId: Long,
    private val savedStateHandle: SavedStateHandle,
    observeMovieDetails: ObserveMovieDetailsUseCase,
    private val refreshMovieDetails: RefreshMovieDetailsUseCase,
) : ViewModel() {

    private val refreshStatus = MutableStateFlow<RefreshStatus>(RefreshStatus.Idle)
    private var refreshJob: Job? = null

    val uiState: StateFlow<MovieDetailUiState> =
        combine(observeMovieDetails(movieId), refreshStatus) { details, refresh ->
            MovieDetailUiState(
                movieId = movieId,
                isLoading = false,
                details = details,
                isRefreshing = refresh == RefreshStatus.InProgress,
                refreshError = (refresh as? RefreshStatus.Failed)?.error,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MovieDetailUiState(movieId))

    /**
     * Called when the user has opened this destination. Fetches full details once per destination;
     * the flag in [SavedStateHandle] prevents repeats on configuration change or process recreation.
     */
    fun onOpened() {
        if (savedStateHandle.get<Boolean>(KEY_REQUESTED) == true) return
        savedStateHandle[KEY_REQUESTED] = true
        refresh()
    }

    fun retry() = refresh()

    private fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshStatus.value = RefreshStatus.InProgress
        Log.d(TAG, "movie $movieId: refreshing details")
        refreshJob = viewModelScope.launch {
            val result = refreshMovieDetails(movieId)
            Log.d(TAG, "movie $movieId: $result")
            refreshStatus.value = result.toRefreshStatus()
        }
    }

    companion object {
        private const val TAG = "MovieDetailViewModel"
        private const val KEY_REQUESTED = "details_requested"
    }
}
