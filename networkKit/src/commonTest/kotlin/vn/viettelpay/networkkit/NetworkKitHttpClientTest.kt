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

    @Test
    fun staticHeaderIsSentOnEveryRequest() = runTest {
        var capturedValue: String? = null
        val engine = MockEngine { request ->
            capturedValue = request.headers["X-Product"]
            respond(content = "", status = HttpStatusCode.OK)
        }
        val client = testClient(
            baseUrl = "https://api.example.com",
            engine = engine,
            headers = mapOf("X-Product" to "promotion"),
        )

        client.get("ping")

        assertEquals("promotion", capturedValue)
    }

    @Test
    fun dynamicHeaderProvidesFreshValueOnEveryRequest() = runTest {
        var counter = 0
        val dynamicHeader = DynamicHeader("X-Request-ID") { (++counter).toString() }
        val capturedValues = mutableListOf<String?>()
        val engine = MockEngine { request ->
            capturedValues.add(request.headers["X-Request-ID"])
            respond(content = "", status = HttpStatusCode.OK)
        }
        val client = testClient(
            baseUrl = "https://api.example.com",
            engine = engine,
            dynamicHeaders = listOf(dynamicHeader),
        )

        client.get("ping")
        client.get("ping")

        assertEquals(listOf<String?>("1", "2"), capturedValues)
    }

    @Test
    fun callerHeaderIsNotOverriddenByStaticConfigHeader() = runTest {
        var capturedValue: String? = null
        val engine = MockEngine { request ->
            capturedValue = request.headers["X-Product"]
            respond(content = "", status = HttpStatusCode.OK)
        }
        val client = testClient(
            baseUrl = "https://api.example.com",
            engine = engine,
            headers = mapOf("X-Product" to "config-value"),
        )

        client.get("ping") {
            headers.append("X-Product", "caller-value")
        }

        assertEquals("caller-value", capturedValue)
    }

    private fun testClient(
        baseUrl: String,
        timeoutMillis: Long = NetworkClientConfig.DEFAULT_TIMEOUT_MILLIS,
        headers: Map<String, String> = emptyMap(),
        dynamicHeaders: List<DynamicHeader> = emptyList(),
        engine: MockEngine,
    ): HttpClient {
        val config = NetworkClientConfig(
            baseUrl = baseUrl,
            timeoutMillis = timeoutMillis,
            headers = headers,
            dynamicHeaders = dynamicHeaders,
        )
        return HttpClient(engine) {
            with(NetworkKitHttpClient) { configure(config) }
        }
    }
}
