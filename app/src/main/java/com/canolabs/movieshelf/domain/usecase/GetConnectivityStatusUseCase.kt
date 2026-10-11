package com.canolabs.movieshelf.domain.usecase

import com.canolabs.movieshelf.domain.model.ConnectivityStatus
import com.canolabs.movieshelf.domain.repository.ConnectivityMonitor

/** Current connectivity, read fresh at the moment a decision is made. */
interface GetConnectivityStatusUseCase {
    operator fun invoke(): ConnectivityStatus
}

class GetConnectivityStatusUseCaseImpl(
    private val monitor: ConnectivityMonitor,
) : GetConnectivityStatusUseCase {
    override fun invoke(): ConnectivityStatus = monitor.currentStatus()
}
