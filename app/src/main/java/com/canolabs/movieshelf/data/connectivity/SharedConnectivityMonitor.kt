package com.canolabs.movieshelf.data.connectivity

import com.canolabs.movieshelf.domain.model.ConnectivityStatus
import com.canolabs.movieshelf.domain.repository.ConnectivityMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

/**
 * Shares one platform registration among all collectors. The callback is registered while at least
 * one collector is active (plus a short grace period across configuration changes) and unregistered
 * afterwards. The initial value is a synchronous snapshot, so the status is never "pending".
 */
class SharedConnectivityMonitor(
    private val source: NetworkStatusSource,
    scope: CoroutineScope,
    stopTimeoutMillis: Long = 5_000,
) : ConnectivityMonitor {

    override val status: StateFlow<ConnectivityStatus> = callbackFlow {
        // Re-read on every (re)start: changes while nobody was listening were not delivered.
        trySend(source.currentStatus())
        val registration = source.register { trySend(it) }
        if (registration == null) {
            // Changes cannot be observed, so the snapshot may go stale: do not claim to know.
            trySend(ConnectivityStatus.Unknown)
        }
        awaitClose { registration?.close() }
    }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis), source.currentStatus())

    override fun currentStatus(): ConnectivityStatus = source.currentStatus()
}
