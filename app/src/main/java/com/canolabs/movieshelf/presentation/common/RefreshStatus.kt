package com.canolabs.movieshelf.presentation.common

import com.canolabs.movieshelf.domain.model.DataError
import com.canolabs.movieshelf.domain.model.RefreshResult

/** Lifecycle of a one-shot remote refresh, kept separately from the cached content it updates. */
sealed interface RefreshStatus {
    data object Idle : RefreshStatus
    data object InProgress : RefreshStatus
    data object Succeeded : RefreshStatus
    data class Failed(val error: DataError) : RefreshStatus
}

fun RefreshResult.toRefreshStatus(): RefreshStatus = when (this) {
    RefreshResult.Success -> RefreshStatus.Succeeded
    is RefreshResult.Failure -> RefreshStatus.Failed(error)
}
