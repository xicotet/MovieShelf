package com.canolabs.movieshelf.data.remote.datasource

import android.util.Log
import com.canolabs.movieshelf.data.remote.api.TmdbApiService
import com.canolabs.movieshelf.data.remote.dto.MovieDetailsDto
import com.canolabs.movieshelf.data.remote.dto.MovieDto
import com.canolabs.movieshelf.domain.model.DataError
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.time.TimeSource

interface MovieRemoteDataSource {
    suspend fun searchMovies(query: String): RemoteResult<List<MovieDto>>
    suspend fun movieDetails(movieId: Long): RemoteResult<MovieDetailsDto>
}

class MovieRemoteDataSourceImpl(
    private val api: TmdbApiService,
) : MovieRemoteDataSource {

    override suspend fun searchMovies(query: String): RemoteResult<List<MovieDto>> =
        safeCall("search/movie \"$query\"", describe = { "${it.size} results" }) { api.searchMovies(query).results }

    override suspend fun movieDetails(movieId: Long): RemoteResult<MovieDetailsDto> =
        safeCall("movie/$movieId", describe = { "\"${it.title}\"" }) { api.movieDetails(movieId) }

    /** Runs one request, logging endpoint, duration and outcome (never the URL, which holds the key). */
    private suspend fun <T> safeCall(
        endpoint: String,
        describe: (T) -> String,
        block: suspend () -> T,
    ): RemoteResult<T> {
        val start = TimeSource.Monotonic.markNow()
        Log.d(TAG, "GET $endpoint started")
        return try {
            val data = block()
            Log.d(TAG, "GET $endpoint OK in ${start.elapsedNow().inWholeMilliseconds} ms: ${describe(data)}")
            RemoteResult.Success(data)
        } catch (e: CancellationException) {
            Log.d(TAG, "GET $endpoint cancelled after ${start.elapsedNow().inWholeMilliseconds} ms")
            throw e
        } catch (e: Exception) {
            val error = e.toDataError()
            Log.w(
                TAG,
                // Ktor exception messages contain the request URL, including the API key: mask it.
                "GET $endpoint failed after ${start.elapsedNow().inWholeMilliseconds} ms -> $error " +
                    "(${e.javaClass.simpleName}: ${redactApiKey(e.message)})",
            )
            RemoteResult.Failure(error)
        }
    }

    companion object {
        private const val TAG = "MovieRemoteDataSource"
    }

    private fun Exception.toDataError(): DataError = when (this) {
        is ResponseException -> when (response.status) {
            HttpStatusCode.Unauthorized -> DataError.Unauthorized
            HttpStatusCode.NotFound -> DataError.NotFound
            else -> DataError.Server
        }
        // Ktor's timeout exceptions are IOExceptions too, so they must be matched first.
        is HttpRequestTimeoutException,
        is ConnectTimeoutException,
        is SocketTimeoutException -> DataError.Timeout
        // DNS failures, refused/reset connections and other transport errors.
        is IOException -> DataError.Network
        is SerializationException,
        is ContentConvertException,
        is NoTransformationFoundException -> DataError.InvalidResponse
        else -> DataError.Unknown
    }
}

private val API_KEY_PARAM = Regex("""(api_key=)[^&\s\]"']+""", RegexOption.IGNORE_CASE)

/** Masks the `api_key` query parameter in text such as Ktor exception messages before it is logged. */
internal fun redactApiKey(text: String?): String = text.orEmpty().replace(API_KEY_PARAM, "$1***")
