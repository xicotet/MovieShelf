package com.canolabs.movieshelf.domain.model

/**
 * What the platform reports about the device's network. It describes whether a network *appears*
 * usable; it never guarantees that TMDB is reachable or that a request will succeed.
 */
enum class ConnectivityStatus {
    /** Not determined, e.g. the platform signal could not be read. Never treated as offline. */
    Unknown,

    /** No network with internet capability: the device is confirmed offline. */
    Unavailable,

    /** A network claims internet capability but has not been validated (e.g. captive portal, still checking). */
    Unvalidated,

    /** A network with internet capability that the platform has validated. */
    Validated,
    ;

    /** Only a positive platform signal counts as offline; unknown or unvalidated states still allow requests. */
    val isConfirmedOffline: Boolean get() = this == Unavailable
}
