package vn.viettelpay.networkkit

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Test lắp ráp — không kiểm từng UC riêng lẻ (đã có ở các file khác), mà xác nhận UC1–UC8 **cùng
 * chạy đúng khi ráp lại**: config (header tĩnh/động + token) + interceptor + request builder + giải
 * mã JSON + [NetworkClient] luôn bọc [networkCall] + business status check do consumer tự áp.
 */
class NetworkClientTest {

    @Serializable
    private data class Envelope(val code: String?, val message: String? = null, val data: Payload? = null)

    @Serializable
    private data class Payload(val id: Int, val name: String)

    private fun Envelope.toBusinessStatus(): BusinessStatus? =
        code?.let { c -> object : BusinessStatus {
            override val code = c
            override val message = this@toBusinessStatus.message
        } }

    private val sessionExpiredChain = StatusCodeHandlerChain(StatusCodeHandler.of("09"))

    @Test
    fun everyMechanismParticipatesInASingleSuccessfulCall() = runTest {
        var capturedProduct: String? = null
        var capturedRequestId: String? = null
        var capturedAuth: String? = null
        var capturedMarker: String? = null
        var requestIdCounter = 0

        val engine = MockEngine { request ->
            capturedProduct = request.headers["Product"]
            capturedRequestId = request.headers["X-Request-ID"]
            capturedAuth = request.headers["Authorization"]
            capturedMarker = request.headers["X-Interceptor-Marker"]
            respond(
                content = """{"code":"00","data":{"id":1,"name":"Voucher A"}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        var interceptorRan = false
        val markerInterceptor = NetworkInterceptor { chain ->
            interceptorRan = true
            chain.request.headers.append("X-Interceptor-Marker", "yes")
            chain.proceed(chain.request)
        }
        val config = NetworkClientConfig(
            baseUrl = "https://api.example.com/base",
            headers = mapOf("Product" to "promotion"), // UC2 — tĩnh
            dynamicHeaders = listOf(DynamicHeader("X-Request-ID") { "req-${++requestIdCounter}" }), // UC2 — động
            tokenProvider = TokenProvider { "abc123" }, // UC3
            interceptors = listOf(markerInterceptor), // UC7 — interceptor
            isDebug = true, // UC8 — chạy cùng lúc, không được làm hỏng gì
        )
        val client = networkClient(engine, config)

        val result = client.getJson<Envelope>("vouchers") // UC4 + UC5, luôn qua networkCall (UC6)
            .checkBusinessStatus(sessionExpiredChain) { it.toBusinessStatus() } // UC7 — status handler

        assertEquals(1, result.data?.id)
        assertEquals("Voucher A", result.data?.name)
        assertEquals(true, interceptorRan)
        assertEquals("promotion", capturedProduct)
        assertEquals("req-1", capturedRequestId)
        assertEquals("Bearer abc123", capturedAuth)
        assertEquals("yes", capturedMarker)
    }

    @Test
    fun businessErrorFromDecodedEnvelopeSurfacesAsBusinessError() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"code":"09","message":"Session expired"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = networkClient(engine, NetworkClientConfig(baseUrl = "https://api.example.com/base"))

        val error = assertFailsWith<BusinessError> {
            client.getJson<Envelope>("vouchers")
                .checkBusinessStatus(sessionExpiredChain) { it.toBusinessStatus() }
        }

        assertEquals("09", error.code)
        assertEquals("Session expired", error.message)
    }

    @Test
    fun transportErrorNeverLeaksRawKtorExceptionThroughNetworkClient() = runTest {
        val engine = MockEngine { respond(content = "", status = HttpStatusCode.InternalServerError) }
        val client = networkClient(engine, NetworkClientConfig(baseUrl = "https://api.example.com/base"))

        // NetworkClient.getJson luôn bọc networkCall {} — không cần caller tự nhớ bọc, khác gọi thẳng
        // HttpClient.getJson (UC4/UC5) nơi quên bọc là lỗi hoàn toàn có thể xảy ra.
        val error = assertFailsWith<NetworkError.Http> {
            client.getJson<Envelope>("vouchers")
        }

        assertEquals(500, error.status)
    }

    private fun networkClient(engine: MockEngine, config: NetworkClientConfig): NetworkClient {
        val httpClient = HttpClient(engine) {
            with(NetworkKitHttpClient) { configure(config) }
        }
        with(NetworkKitHttpClient) { httpClient.applyInterceptors(config) }
        return NetworkClient(httpClient)
    }
}
