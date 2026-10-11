package com.canolabs.movieshelf.domain.repository

import com.canolabs.movieshelf.domain.model.ConnectivityStatus
import kotlinx.coroutines.flow.StateFlow

interface ConnectivityMonitor {

    /** Observable status for display. It always has a value, so collectors never wait for a first signal. */
    val status: StateFlow<ConnectivityStatus>

    /**
     * A fresh platform snapshot for one-off decisions (e.g. whether to send a request). Unlike
     * `status.value`, it is accurate even when nothing has been collecting [status].
     */
    fun currentStatus(): ConnectivityStatus
}
