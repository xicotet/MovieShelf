package com.canolabs.movieshelf.data.connectivity

import android.net.ConnectivityManager
import android.util.Log
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.canolabs.movieshelf.domain.model.ConnectivityStatus
import java.util.concurrent.ConcurrentHashMap

/**
 * [NetworkStatusSource] backed by [ConnectivityManager]. It tracks every non-VPN network with internet
 * capability (see [connectivityStatusOf] for why VPNs are ignored). Requires `ACCESS_NETWORK_STATE`;
 * any platform failure degrades to [ConnectivityStatus.Unknown], which never blocks requests.
 */
class AndroidNetworkStatusSource(
    private val connectivityManager: ConnectivityManager?,
) : NetworkStatusSource {

    override fun currentStatus(): ConnectivityStatus {
        val manager = connectivityManager ?: return ConnectivityStatus.Unknown
        return try {
            @Suppress("DEPRECATION") // allNetworks is the only synchronous way to list every network.
            val flags = manager.allNetworks.mapNotNull { manager.getNetworkCapabilities(it)?.toFlags() }
            connectivityStatusOf(flags).also { Log.d(TAG, "snapshot ${flags.describe()} -> $it") }
        } catch (e: RuntimeException) {
            Log.w(TAG, "snapshot failed (${e.javaClass.simpleName}) -> Unknown")
            ConnectivityStatus.Unknown
        }
    }

    override fun register(onChange: (ConnectivityStatus) -> Unit): AutoCloseable? {
        val manager = connectivityManager ?: return null
        val networks = ConcurrentHashMap<Network, NetworkFlags>()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                networks[network] = capabilities.toFlags()
                publish()
            }

            override fun onLost(network: Network) {
                networks.remove(network)
                publish()
            }

            private fun publish() {
                val status = connectivityStatusOf(networks.values)
                Log.d(TAG, "networks changed ${networks.values.describe()} -> $status")
                onChange(status)
            }
        }
        // NetworkRequest.Builder requests NOT_VPN by default, so only physical networks are reported.
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        return try {
            manager.registerNetworkCallback(request, callback)
            Log.d(TAG, "network callback registered")
            AutoCloseable {
                try {
                    manager.unregisterNetworkCallback(callback)
                    Log.d(TAG, "network callback unregistered")
                } catch (_: RuntimeException) {
                    // Already unregistered.
                }
            }
        } catch (e: RuntimeException) {
            // SecurityException without the permission, or too many registered callbacks.
            Log.w(TAG, "network callback registration failed: ${e.javaClass.simpleName}")
            null
        }
    }

    private fun NetworkCapabilities.toFlags() = NetworkFlags(
        hasInternet = hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
        isValidated = hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
        isVpn = hasTransport(NetworkCapabilities.TRANSPORT_VPN),
    )

    private fun Collection<NetworkFlags>.describe(): String =
        joinToString(prefix = "[", postfix = "]") { f ->
            buildString {
                append(if (f.isVpn) "vpn" else "physical")
                if (f.hasInternet) append("+internet")
                if (f.isValidated) append("+validated")
            }
        }

    companion object {
        private const val TAG = "AndroidNetworkStatusSource"
    }
}
