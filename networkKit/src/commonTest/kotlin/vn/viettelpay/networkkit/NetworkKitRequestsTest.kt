package vn.viettelpay.networkkit

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkKitRequestsTest {

    @Test
    fun pathSegmentsWithSpecialCharactersRoundTripCorrectly() = runTest {
        var captured: HttpRequestData? = null
        val engine = MockEngine { request ->
            captured = request
            respond(content = "", status = HttpStatusCode.OK)
        }
        val client = testClient(engine)

        client.getRequest("vouchers", "mã ưu đãi #1")

        val segments = captured?.url?.segments
        assertEquals(listOf("base", "vouchers", "mã ưu đãi #1"), segments)
    }

    @Test
    fun queryParameterWithReservedCharactersRoundTripsCorrectly() = runTest {
        var captured: HttpRequestData? = null
        val engine = MockEngine { request ->
            captured = request
            respond(content = "", status = HttpStatusCode.OK)
        }
        val client = testClient(engine)

        client.getRequest("search", queryParameters = mapOf("q" to "a&b=c"))

        assertEquals("a&b=c", captured?.url?.parameters?.get("q"))
    }

    @Test
    fun postSendsBodyToConfiguredPath() = runTest {
        var captured: HttpRequestData? = null
        val engine = MockEngine { request ->
            captured = request
            respond(content = "", status = HttpStatusCode.OK)
        }
        val client = testClient(engine)

        client.postRequest("vouchers", body = "hello")

        assertEquals("POST", captured?.method?.value)
        val segments = captured?.url?.segments
        assertEquals(listOf("base", "vouchers"), segments)
    }

    private fun testClient(engine: MockEngine): HttpClient {
        val config = NetworkClientConfig(baseUrl = "https://api.example.com/base")
        return HttpClient(engine) {
            with(NetworkKitHttpClient) { configure(config) }
        }
    }
}
