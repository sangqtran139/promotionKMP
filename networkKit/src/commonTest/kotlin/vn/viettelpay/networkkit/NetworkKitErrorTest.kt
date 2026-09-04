package vn.viettelpay.networkkit

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NetworkKitErrorTest {

    @Serializable
    data class SampleDto(val id: Int)

    @Test
    fun slowServerBecomesTimeoutError() = runTest {
        val engine = MockEngine {
            delay(5_000)
            respond(content = "", status = HttpStatusCode.OK)
        }
        val client = errorClient(engine, timeoutMillis = 50)

        assertFailsWith<NetworkError.Timeout> {
            networkCall { client.getJson<SampleDto>("sample") }
        }
    }

    @Test
    fun engineIoFailureBecomesNoConnectionError() = runTest {
        val engine = MockEngine {
            throw IOException("no network")
        }
        val client = errorClient(engine)

        assertFailsWith<NetworkError.NoConnection> {
            networkCall { client.getJson<SampleDto>("sample") }
        }
    }

    @Test
    fun httpErrorStatusBecomesHttpErrorWithStatusAndBody() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"code":"NOT_FOUND"}""",
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = errorClient(engine)

        val error = assertFailsWith<NetworkError.Http> {
            networkCall { client.getJson<SampleDto>("sample") }
        }

        assertEquals(404, error.status)
        assertEquals("""{"code":"NOT_FOUND"}""", error.rawBody)
    }

    @Test
    fun malformedJsonBecomesSerializationError() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"id":"not-a-number"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = errorClient(engine)

        assertFailsWith<NetworkError.Serialization> {
            networkCall { client.getJson<SampleDto>("sample") }
        }
    }

    private fun errorClient(
        engine: MockEngine,
        timeoutMillis: Long = NetworkClientConfig.DEFAULT_TIMEOUT_MILLIS,
    ): HttpClient {
        val config = NetworkClientConfig(baseUrl = "https://api.example.com/base", timeoutMillis = timeoutMillis)
        return HttpClient(engine) {
            with(NetworkKitHttpClient) { configure(config) }
        }
    }
}
