package com.canolabs.movieshelf.data.connectivity

import com.canolabs.movieshelf.domain.model.ConnectivityStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SharedConnectivityMonitorTest {

    private class FakeNetworkStatusSource(var current: ConnectivityStatus) : NetworkStatusSource {
        var listener: ((ConnectivityStatus) -> Unit)? = null
        var registrations = 0
        var unregistrations = 0
        var failRegistration = false

        override fun currentStatus() = current

        override fun register(onChange: (ConnectivityStatus) -> Unit): AutoCloseable? {
            if (failRegistration) return null
            registrations++
            listener = onChange
            return AutoCloseable {
                unregistrations++
                listener = null
            }
        }

        /** A platform callback for a change of the default network. */
        fun change(status: ConnectivityStatus) {
            current = status
            listener?.invoke(status)
        }
    }

    private fun TestScope.monitor(source: NetworkStatusSource) =
        SharedConnectivityMonitor(source, backgroundScope, stopTimeoutMillis = 5_000)

    @Test
    fun `initial offline state is available immediately`() = runTest {
        val monitor = monitor(FakeNetworkStatusSource(ConnectivityStatus.Unavailable))

        assertEquals(ConnectivityStatus.Unavailable, monitor.status.value)
    }

    @Test
    fun `initial online state is available immediately`() = runTest {
        val monitor = monitor(FakeNetworkStatusSource(ConnectivityStatus.Validated))

        assertEquals(ConnectivityStatus.Validated, monitor.status.value)
    }

    @Test
    fun `propagates online to offline and back to online`() = runTest {
        val source = FakeNetworkStatusSource(ConnectivityStatus.Validated)
        val monitor = monitor(source)
        val seen = mutableListOf<ConnectivityStatus>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { monitor.status.toList(seen) }
        runCurrent()

        source.change(ConnectivityStatus.Unavailable)
        runCurrent()
        source.change(ConnectivityStatus.Unvalidated)
        runCurrent()
        source.change(ConnectivityStatus.Validated)
        runCurrent()

        assertEquals(
            listOf(
                ConnectivityStatus.Validated,
                ConnectivityStatus.Unavailable,
                ConnectivityStatus.Unvalidated,
                ConnectivityStatus.Validated,
            ),
            seen,
        )
    }

    @Test
    fun `several collectors share one platform registration`() = runTest {
        val source = FakeNetworkStatusSource(ConnectivityStatus.Validated)
        val monitor = monitor(source)

        repeat(3) { backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { monitor.status.collect {} } }
        runCurrent()

        assertEquals(1, source.registrations)
    }

    @Test
    fun `unregisters after the last collector leaves and re-reads the status on return`() = runTest {
        val source = FakeNetworkStatusSource(ConnectivityStatus.Validated)
        val monitor = monitor(source)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { monitor.status.collect {} }
        runCurrent()

        collector.cancel()
        advanceTimeBy(5_001)
        runCurrent()
        assertEquals(1, source.unregistrations)

        // A change happens while nobody listens; the next collector must see it, not a stale value.
        source.current = ConnectivityStatus.Unavailable
        val seen = mutableListOf<ConnectivityStatus>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { monitor.status.toList(seen) }
        runCurrent()

        assertEquals(2, source.registrations)
        assertEquals(ConnectivityStatus.Unavailable, seen.last())
    }

    @Test
    fun `a refused registration reports unknown instead of a possibly stale offline state`() = runTest {
        val source = FakeNetworkStatusSource(ConnectivityStatus.Unavailable).apply { failRegistration = true }
        val monitor = monitor(source)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { monitor.status.collect {} }
        runCurrent()

        assertEquals(ConnectivityStatus.Unknown, monitor.status.value)
        assertEquals(false, monitor.status.value.isConfirmedOffline)
    }

    @Test
    fun `current status is read fresh from the platform even without collectors`() = runTest {
        val source = FakeNetworkStatusSource(ConnectivityStatus.Validated)
        val monitor = monitor(source)

        source.current = ConnectivityStatus.Unavailable

        assertEquals(ConnectivityStatus.Unavailable, monitor.currentStatus())
    }

    private val validatedVpn = NetworkFlags(hasInternet = true, isValidated = true, isVpn = true)
    private val validatedWifi = NetworkFlags(hasInternet = true, isValidated = true, isVpn = false)
    private val unvalidatedWifi = NetworkFlags(hasInternet = true, isValidated = false, isVpn = false)
    private val noInternetCellular = NetworkFlags(hasInternet = false, isValidated = false, isVpn = false)

    @Test
    fun `no networks at all is offline`() {
        assertEquals(ConnectivityStatus.Unavailable, connectivityStatusOf(emptyList()))
    }

    @Test
    fun `a validated vpn with no physical network underneath is offline`() {
        // Observed on a Pixel 7 with NordVPN in airplane mode: the tunnel stays "validated".
        assertEquals(ConnectivityStatus.Unavailable, connectivityStatusOf(listOf(validatedVpn)))
    }

    @Test
    fun `a physical network decides connectivity even when a vpn is active`() {
        assertEquals(ConnectivityStatus.Validated, connectivityStatusOf(listOf(validatedVpn, validatedWifi)))
        assertEquals(ConnectivityStatus.Unvalidated, connectivityStatusOf(listOf(validatedVpn, unvalidatedWifi)))
    }

    @Test
    fun `physical networks without internet capability do not count`() {
        assertEquals(ConnectivityStatus.Unavailable, connectivityStatusOf(listOf(noInternetCellular)))
        assertEquals(ConnectivityStatus.Validated, connectivityStatusOf(listOf(noInternetCellular, validatedWifi)))
    }
}
