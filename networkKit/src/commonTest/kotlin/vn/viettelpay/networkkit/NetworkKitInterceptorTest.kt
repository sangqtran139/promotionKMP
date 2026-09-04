package vn.viettelpay.networkkit

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkKitInterceptorTest {

    @Test
    fun requestSucceedsUnchangedWhenNoInterceptorsRegistered() = runTest {
        var callCount = 0
        val engine = MockEngine {
            callCount++
            respond(content = "", status = HttpStatusCode.OK)
        }
        val client = interceptorClient(engine, interceptors = emptyList())

        client.get("ping")

        assertEquals(1, callCount)
    }

    @Test
    fun passthroughInterceptorDoesNotChangeRequestCount() = runTest {
        var callCount = 0
        val engine = MockEngine {
            callCount++
            respond(content = "", status = HttpStatusCode.OK)
        }
        val passthrough = NetworkInterceptor { chain -> chain.proceed(chain.request) }
        val client = interceptorClient(engine, interceptors = listOf(passthrough))

        client.get("ping")

        assertEquals(1, callCount)
    }

    @Test
    fun interceptorCanRetryBasedOnResponseHeader() = runTest {
        // Dùng header tuỳ biến thay vì status lỗi thật để không lẫn với ResponseException do
        // expectSuccess=true (UC6) ném ra — interceptor ở đây chủ ý retry theo tín hiệu riêng của nó,
        // không phải theo lỗi HTTP.
        var callCount = 0
        val engine = MockEngine {
            callCount++
            respond(
                content = "",
                status = HttpStatusCode.OK,
                headers = headersOf("X-Needs-Retry", listOf(if (callCount == 1) "true" else "false")),
            )
        }
        val retryOnce = NetworkInterceptor { chain ->
            val call = chain.proceed(chain.request)
            if (call.response.headers["X-Needs-Retry"] == "true") {
                chain.proceed(chain.request)
            } else {
                call
            }
        }
        val client = interceptorClient(engine, interceptors = listOf(retryOnce))

        val response = client.get("ping")

        assertEquals(2, callCount)
        assertEquals("false", response.headers["X-Needs-Retry"])
    }

    @Test
    fun multipleInterceptorsRunInRegistrationOrder() = runTest {
        val order = mutableListOf<String>()
        val engine = MockEngine {
            order.add("engine")
            respond(content = "", status = HttpStatusCode.OK)
        }
        val first = NetworkInterceptor { chain ->
            order.add("first-before")
            val call = chain.proceed(chain.request)
            order.add("first-after")
            call
        }
        val second = NetworkInterceptor { chain ->
            order.add("second-before")
            val call = chain.proceed(chain.request)
            order.add("second-after")
            call
        }
        val client = interceptorClient(engine, interceptors = listOf(first, second))

        client.get("ping")

        assertEquals(listOf("first-before", "second-before", "engine", "second-after", "first-after"), order)
    }

    @Test
    fun interceptorCanChangeHeaderOnRetry() = runTest {
        // Mô phỏng refresh token: request đầu mang token cũ, interceptor phát hiện qua header phản
        // hồi rồi tự đổi Authorization trước khi proceed() lần hai.
        val capturedTokens = mutableListOf<String?>()
        val engine = MockEngine { request ->
            capturedTokens.add(request.headers["Authorization"])
            val needsRetry = capturedTokens.size == 1
            respond(
                content = "",
                status = HttpStatusCode.OK,
                headers = headersOf("X-Needs-Retry", listOf(needsRetry.toString())),
            )
        }
        val refreshOnSignal = NetworkInterceptor { chain ->
            val call = chain.proceed(chain.request)
            if (call.response.headers["X-Needs-Retry"] == "true") {
                chain.request.headers.remove("Authorization")
                chain.request.headers.append("Authorization", "Bearer refreshed-token")
                chain.proceed(chain.request)
            } else {
                call
            }
        }
        val client = interceptorClient(engine, interceptors = listOf(refreshOnSignal))

        client.get("ping") {
            header("Authorization", "Bearer stale-token")
        }

        assertEquals(listOf<String?>("Bearer stale-token", "Bearer refreshed-token"), capturedTokens)
    }

    private fun interceptorClient(engine: MockEngine, interceptors: List<NetworkInterceptor>): HttpClient {
        val config = NetworkClientConfig(baseUrl = "https://api.example.com", interceptors = interceptors)
        val client = HttpClient(engine) {
            with(NetworkKitHttpClient) { configure(config) }
        }
        with(NetworkKitHttpClient) { client.applyInterceptors(config) }
        return client
    }
}
