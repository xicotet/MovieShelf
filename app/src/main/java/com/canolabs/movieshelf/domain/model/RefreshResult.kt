package com.canolabs.movieshelf.domain.model

/** Outcome of a one-shot remote refresh. Data itself is always observed from the local cache. */
sealed interface RefreshResult {
    data object Success : RefreshResult
    data class Failure(val error: DataError) : RefreshResult
}

enum class DataError {
    /**
     * The server could not be reached (no route, DNS or connection failure). This is not proof that
     * the device is offline, only that TMDB was unreachable.
     */
    Network,

    /** A connection or response did not complete within the configured timeouts. */
    Timeout,

    /** The API rejected the credentials (HTTP 401), e.g. a missing or invalid API key. */
    Unauthorized,

    /** The requested resource does not exist (HTTP 404). */
    NotFound,

    /** Any other non-successful HTTP status. */
    Server,

    /** The response could not be parsed. */
    InvalidResponse,

    Unknown,
}
