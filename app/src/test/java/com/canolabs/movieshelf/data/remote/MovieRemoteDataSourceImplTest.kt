package com.canolabs.movieshelf.data.remote

import com.canolabs.movieshelf.data.remote.api.TmdbApiConfig
import com.canolabs.movieshelf.data.remote.api.TmdbApiService
import com.canolabs.movieshelf.data.remote.api.TmdbJson
import com.canolabs.movieshelf.data.remote.api.createTmdbHttpClient
import com.canolabs.movieshelf.data.remote.datasource.MovieRemoteDataSourceImpl
import com.canolabs.movieshelf.data.remote.datasource.RemoteResult
import com.canolabs.movieshelf.data.remote.datasource.redactApiKey
import com.canolabs.movieshelf.domain.model.DataError
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class MovieRemoteDataSourceImplTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun dataSource(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        MovieRemoteDataSourceImpl(
            TmdbApiService(
                client = createTmdbHttpClient(
                    MockEngine { request ->
                        requests += request
                        handler(request)
                    },
                    TmdbJson,
                ),
                config = TmdbApiConfig(apiKey = "test-key", baseUrl = "https://example.test/3/"),
            ),
        )

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    @Test
    fun `parses search results and sends the query with the api key`() = runTest {
        val source = dataSource {
            json("""{"page":1,"results":[{"id":78,"title":"Blade Runner","release_date":"1982-06-25","poster_path":"/p.jpg","unknown":true}],"total_pages":1}""")
        }

        val result = source.searchMovies("blade runner")

        assertEquals(listOf(78L), (result as RemoteResult.Success).data.results.map { it.id })
        val url = requests.single().url
        assertEquals("/3/search/movie", url.encodedPath)
        assertEquals("blade runner", url.parameters["query"])
        assertEquals("test-key", url.parameters["api_key"])
        assertEquals("1", url.parameters["page"])
    }

    @Test
    fun `requests the given page and exposes tmdb's pagination metadata`() = runTest {
        val source = dataSource { json("""{"page":3,"results":[{"id":5,"title":"Alien 3"}],"total_pages":7,"total_results":140}""") }

        val page = (source.searchMovies("alien", page = 3) as RemoteResult.Success).data

        assertEquals("3", requests.single().url.parameters["page"])
        assertEquals(3, page.page)
        assertEquals(7, page.totalPages)
        assertEquals(listOf(5L), page.results.map { it.id })
    }

    @Test
    fun `an empty result list is a success, not an error`() = runTest {
        val source = dataSource { json("""{"page":1,"results":[],"total_pages":0,"total_results":0}""") }

        val page = (source.searchMovies("zzzz") as RemoteResult.Success).data

        assertTrue(page.results.isEmpty())
        assertEquals(0, page.totalPages)
    }

    @Test
    fun `maps unauthorized responses`() = runTest {
        val source = dataSource { json("""{"status_code":7,"status_message":"Invalid API key"}""", HttpStatusCode.Unauthorized) }

        assertEquals(RemoteResult.Failure(DataError.Unauthorized), source.searchMovies("x"))
    }

    @Test
    fun `maps server errors`() = runTest {
        val source = dataSource { json("{}", HttpStatusCode.ServiceUnavailable) }

        assertEquals(RemoteResult.Failure(DataError.Server), source.searchMovies("x"))
    }

    @Test
    fun `maps missing movies to not found`() = runTest {
        val source = dataSource { json("{}", HttpStatusCode.NotFound) }

        assertEquals(RemoteResult.Failure(DataError.NotFound), source.movieDetails(1))
    }

    @Test
    fun `maps malformed bodies to invalid response`() = runTest {
        val source = dataSource { json("""{"page":"not a number"""") }

        assertEquals(RemoteResult.Failure(DataError.InvalidResponse), source.searchMovies("x"))
    }

    @Test
    fun `maps connectivity failures to network errors`() = runTest {
        val source = dataSource { throw IOException("Unable to resolve host") }

        assertEquals(RemoteResult.Failure(DataError.Network), source.searchMovies("x"))
    }

    @Test
    fun `maps dns and refused connections to network errors, not timeouts`() = runTest {
        assertEquals(
            RemoteResult.Failure(DataError.Network),
            dataSource { throw UnknownHostException("api.themoviedb.org") }.searchMovies("x"),
        )
        assertEquals(
            RemoteResult.Failure(DataError.Network),
            dataSource { throw ConnectException("Connection refused") }.searchMovies("x"),
        )
    }

    @Test
    fun `maps request, connect and socket timeouts to timeout errors`() = runTest {
        assertEquals(
            RemoteResult.Failure(DataError.Timeout),
            dataSource { throw HttpRequestTimeoutException("https://example.test/3/search/movie", 20_000L) }
                .searchMovies("x"),
        )
        assertEquals(
            RemoteResult.Failure(DataError.Timeout),
            dataSource { throw ConnectTimeoutException("connect timed out") }.searchMovies("x"),
        )
        assertEquals(
            RemoteResult.Failure(DataError.Timeout),
            dataSource { throw SocketTimeoutException("read timed out") }.searchMovies("x"),
        )
    }

    @Test
    fun `coroutine cancellation is propagated instead of becoming an error`() = runTest {
        val source = dataSource { throw CancellationException("caller left") }

        val thrown = runCatching { source.searchMovies("x") }.exceptionOrNull()

        assertTrue(thrown is CancellationException)
    }

    @Test
    fun `masks the api key in exception messages before they are logged`() {
        // Ktor puts the full request URL, including api_key, into its timeout message.
        val message = HttpRequestTimeoutException("https://example.test/3/search/movie?api_key=test-key&query=slow", 20_000L).message

        val redacted = redactApiKey(message)

        assertTrue(message!!.contains("test-key"))
        assertFalse(redacted.contains("test-key"))
        assertTrue(redacted.contains("api_key=***&query=slow"))
        assertEquals("GET /movie/1?api_key=***", redactApiKey("GET /movie/1?api_key=secret"))
        assertEquals("", redactApiKey(null))
    }
}
