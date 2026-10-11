package com.canolabs.movieshelf.domain.model

/** Outcome of a one-shot remote refresh. Data itself is always observed from the local cache. */
sealed interface RefreshResult {
    data object Success : RefreshResult
    data class Failure(val error: DataError) : RefreshResult
}

/**
 * Why a remote request failed.
 *
 * - [Network]: the server could not be reached (no route, DNS or connection failure). Not proof that
 *   the device is offline, only that TMDB was unreachable.
 * - [Timeout]: a connection or response did not complete within the configured timeouts.
 * - [Unauthorized]: the API rejected the credentials (HTTP 401), e.g. a missing or invalid API key.
 * - [NotFound]: the requested resource does not exist (HTTP 404).
 * - [Server]: any other non-successful HTTP status.
 * - [InvalidResponse]: the response could not be parsed.
 * - [Unknown]: anything else.
 */
enum class DataError {
    Network,
    Timeout,
    Unauthorized,
    NotFound,
    Server,
    InvalidResponse,
    Unknown,
}
