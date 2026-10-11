package com.canolabs.movieshelf.data.remote.datasource

import com.canolabs.movieshelf.domain.model.DataError

/** Data-layer representation of a remote call: either typed DTOs or a classified failure. */
sealed interface RemoteResult<out T> {
    data class Success<T>(val data: T) : RemoteResult<T>
    data class Failure(val error: DataError) : RemoteResult<Nothing>
}
