package com.canolabs.movieshelf.presentation.common

import androidx.annotation.StringRes
import com.canolabs.movieshelf.R
import com.canolabs.movieshelf.domain.model.DataError

@StringRes
fun DataError.titleRes(): Int = when (this) {
    DataError.Network -> R.string.error_network_title
    DataError.Timeout -> R.string.error_timeout_title
    DataError.Unauthorized -> R.string.error_unauthorized_title
    DataError.NotFound -> R.string.error_not_found_title
    DataError.Server, DataError.InvalidResponse, DataError.Unknown -> R.string.error_generic_title
}

@StringRes
fun DataError.messageRes(): Int = when (this) {
    DataError.Network -> R.string.error_network_message
    DataError.Timeout -> R.string.error_timeout_message
    DataError.Unauthorized -> R.string.error_unauthorized_message
    DataError.NotFound -> R.string.error_not_found_message
    DataError.Server, DataError.InvalidResponse, DataError.Unknown -> R.string.error_generic_message
}
