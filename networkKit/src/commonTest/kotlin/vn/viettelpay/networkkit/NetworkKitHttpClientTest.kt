package vn.viettelpay.networkkit

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NetworkKitHttpClientTest {

    @Test
    fun baseUrlWithoutTrailingSlashStillJoinsPathCorrectly() = runTest {
        var capturedUrl: String? = null
        val engine = MockEngine { request ->
            capturedUrl = request.url.toString()
            respond(content = "", status = HttpStatusCode.OK)
        }
        val client = testClient(baseUrl = "https://api.example.com/base", engine = engine)

        client.get("path/to/resource")

        assertEquals("https://api.example.com/base/path/to/resource", capturedUrl)
    }

    @Test
    fun baseUrlWithTrailingSlashJoinsPathTheSameWay() = runTest {
        var capturedUrl: String? = null
        val engine = MockEngine { request ->
            capturedUrl = request.url.toString()
            respond(content = "", status = HttpStatusCode.OK)
        }
        val client = testClient(baseUrl = "https://api.example.com/base/", engine = engine)

        client.get("path/to/resource")

        assertEquals("https://api.example.com/base/path/to/resource", capturedUrl)
    }

    @Test
    fun requestTimesOutWhenServerIsSlowerThanConfiguredTimeout() = runTest {
        val engine = MockEngine {
            delay(5_000)
            respond(content = "", status = HttpStatusCode.OK)
        }
        val client = testClient(baseUrl = "https://api.example.com", timeoutMillis = 50, engine = engine)

        assertFailsWith<HttpRequestTimeoutException> {
            client.get("ping")
        }
    }

    @Test
    fun defaultTimeoutIsThirtySeconds() {
        assertEquals(30_000L, NetworkClientConfig.DEFAULT_TIMEOUT_MILLIS)
        assertEquals(30_000L, NetworkClientConfig(baseUrl = "https://api.example.com").timeoutMillis)
    }

    private fun testClient(
        baseUrl: String,
        timeoutMillis: Long = NetworkClientConfig.DEFAULT_TIMEOUT_MILLIS,
        engine: MockEngine,
    ): HttpClient {
        val config = NetworkClientConfig(baseUrl = baseUrl, timeoutMillis = timeoutMillis)
        return HttpClient(engine) {
            with(NetworkKitHttpClient) { configure(config) }
        }
    }
}
