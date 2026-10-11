package com.canolabs.movieshelf.data.connectivity

import com.canolabs.movieshelf.domain.model.ConnectivityStatus

/**
 * Minimal seam over the platform's network APIs, so the sharing and lifecycle logic in
 * [SharedConnectivityMonitor] can be tested without Android framework types.
 */
interface NetworkStatusSource {

    /** Synchronous snapshot of the device's networks. */
    fun currentStatus(): ConnectivityStatus

    /**
     * Starts delivering connectivity changes to [onChange]. Returns the handle that stops them, or
     * `null` when the platform refused the registration.
     */
    fun register(onChange: (ConnectivityStatus) -> Unit): AutoCloseable?
}

/** The capability flags of one network that matter for connectivity. */
data class NetworkFlags(
    val hasInternet: Boolean,
    val isValidated: Boolean,
    val isVpn: Boolean,
)

/**
 * Derives connectivity from the device's *physical* networks (Wi-Fi, cellular, ethernet…).
 *
 * VPNs are ignored on purpose: a VPN (e.g. one with a kill switch) can stay "connected" and report
 * validated internet while no network underlies it, which would hide a real offline state and send
 * requests into a dead tunnel until they time out.
 */
fun connectivityStatusOf(networks: Collection<NetworkFlags>): ConnectivityStatus {
    val physical = networks.filter { it.hasInternet && !it.isVpn }
    return when {
        physical.isEmpty() -> ConnectivityStatus.Unavailable
        physical.any { it.isValidated } -> ConnectivityStatus.Validated
        else -> ConnectivityStatus.Unvalidated
    }
}
