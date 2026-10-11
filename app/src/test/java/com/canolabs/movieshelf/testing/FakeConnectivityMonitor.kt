package com.canolabs.movieshelf.testing

import com.canolabs.movieshelf.domain.model.ConnectivityStatus
import com.canolabs.movieshelf.domain.repository.ConnectivityMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Connectivity double. [status] is what observers see; [platformStatus] is what a fresh platform read
 * returns, so tests can model a delayed callback (the two disagreeing).
 */
class FakeConnectivityMonitor(initial: ConnectivityStatus = ConnectivityStatus.Validated) : ConnectivityMonitor {
    private val observed = MutableStateFlow(initial)
    var platformStatus: ConnectivityStatus = initial

    override val status: StateFlow<ConnectivityStatus> = observed

    override fun currentStatus(): ConnectivityStatus = platformStatus

    /** A connectivity change delivered by the platform callback. */
    fun emit(status: ConnectivityStatus) {
        platformStatus = status
        observed.value = status
    }
}
