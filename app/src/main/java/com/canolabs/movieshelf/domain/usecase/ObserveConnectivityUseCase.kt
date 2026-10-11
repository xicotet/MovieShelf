package com.canolabs.movieshelf.domain.usecase

import com.canolabs.movieshelf.domain.model.ConnectivityStatus
import com.canolabs.movieshelf.domain.repository.ConnectivityMonitor
import kotlinx.coroutines.flow.StateFlow

interface ObserveConnectivityUseCase {
    operator fun invoke(): StateFlow<ConnectivityStatus>
}

class ObserveConnectivityUseCaseImpl(
    private val monitor: ConnectivityMonitor,
) : ObserveConnectivityUseCase {
    override fun invoke(): StateFlow<ConnectivityStatus> = monitor.status
}
