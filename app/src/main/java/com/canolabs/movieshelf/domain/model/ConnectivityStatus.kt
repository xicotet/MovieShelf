package com.canolabs.movieshelf.domain.model

/**
 * What the platform reports about the device's network. It describes whether a network *appears*
 * usable; it never guarantees that TMDB is reachable or that a request will succeed.
 *
 * - [Unknown]: not determined, e.g. the platform signal could not be read. Never treated as offline.
 * - [Unavailable]: no network with internet capability; the device is confirmed offline.
 * - [Unvalidated]: a network claims internet capability but has not been validated (e.g. a captive
 *   portal, or still checking).
 * - [Validated]: a network with internet capability that the platform has validated.
 *
 * [isConfirmedOffline] is true only for [Unavailable]: only a positive platform signal counts as
 * offline, so unknown or unvalidated states still allow requests.
 */
enum class ConnectivityStatus {
    Unknown,
    Unavailable,
    Unvalidated,
    Validated;

    val isConfirmedOffline: Boolean get() = this == Unavailable
}
