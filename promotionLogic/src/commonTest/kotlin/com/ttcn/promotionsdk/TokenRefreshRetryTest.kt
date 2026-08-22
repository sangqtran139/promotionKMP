package com.ttcn.promotionsdk

import com.ttcn.promotionsdk.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.data.remote.FeatureFlagRemoteDataSource
import com.ttcn.promotionsdk.data.remote.KtorFeatureFlagApiService
import com.ttcn.promotionsdk.data.remote.KtorPromotionApiService
import com.ttcn.promotionsdk.data.remote.PromotionHttpClient
import com.ttcn.promotionsdk.data.remote.PromotionRemoteDataSource
import com.ttcn.promotionsdk.data.remote.TokenRefreshGate
import com.ttcn.promotionsdk.data.repository.PromotionRepositoryImpl
import com.ttcn.promotionsdk.common.SdkLock
import com.ttcn.promotionsdk.common.withLock
import com.ttcn.promotionsdk.domain.exception.FeatureFlagException
import com.ttcn.promotionsdk.domain.exception.PromotionErrorCodes
import com.ttcn.promotionsdk.domain.exception.PromotionException
import com.ttcn.promotionsdk.domain.model.voucher.SearchCustomerVouchersRequest
import com.ttcn.promotionsdk.domain.usecase.SearchCustomerVouchersUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Thử lại khi 401: token chết **giữa** lúc màn hình SDK đang mở thì SDK xin host token mới rồi chạy
 * lại request hỏng — user không thấy màn lỗi.
 *
 * Đây là lớp thứ hai, sau `TokenPullPerRequestTest`. `tokenProvider` chỉ cứu được ca host đã refresh
 * xong **trước** khi SDK gọi; ca token chết đúng giữa lượt gọi phải do cổng này lo.
 */
class TokenRefreshRetryTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val okBody = """{"status":200,"success":true,"data":null}"""

    /**
     * Sổ ghi header `Authorization` của từng lượt gửi. Có khoá vì test single-flight chạy 4 request
     * song song — `MutableList` trần ở đó là chính test tự sinh race, không phải code sản phẩm.
     */
    private class AuthLog {
        private val lock = SdkLock()
        private val entries = mutableListOf<String>()

        fun record(value: String) = lock.withLock { entries += value }
        fun snapshot(): List<String> = lock.withLock { entries.toList() }
    }

    /**
     * Host giả: giữ token hiện hành, và `refreshAccessToken` đổi nó sang [freshToken] rồi báo
     * `true`. [freshToken] `null` = host chịu, báo `false`.
     * [refreshCount] đếm số lần SDK thật sự hỏi host — chốt của phần single-flight.
     */
    private class FakeHost(
        private var token: String,
        private val freshToken: String?,
    ) : PromotionRequestContextProvider {
        var refreshCount = 0
            private set

        override fun getAccessToken(): String = token

        override fun refreshAccessToken(onResult: (Boolean) -> Unit) {
            refreshCount++
            // Hợp đồng: ghi vào kho TRƯỚC rồi mới báo — lượt thử lại đọc lại `getAccessToken()`.
            if (freshToken != null) token = freshToken
            onResult(freshToken != null)
        }
    }

    /** Engine chỉ chấp nhận [validToken]; token khác đều 401. Ghi lại header của từng lượt. */
    private fun dataSourceOf(
        host: FakeHost,
        validToken: String,
        seen: AuthLog,
    ): PromotionRemoteDataSource {
        val client = HttpClient(MockEngine { r: HttpRequestData ->
            val auth = r.headers[HttpHeaders.Authorization].orEmpty()
            seen.record(auth)
            if (auth == "Bearer $validToken") {
                respond(okBody, HttpStatusCode.OK, jsonHeaders)
            } else {
                respond("""{"code":"UNAUTHORIZED","message":"expired"}""", HttpStatusCode.Unauthorized, jsonHeaders)
            }
        }) {
            with(PromotionHttpClient) { configure("https://api.example.com", host, false) }
        }
        return PromotionRemoteDataSource(KtorPromotionApiService(client), TokenRefreshGate(host))
    }

    private fun useCaseOf(ds: PromotionRemoteDataSource) =
        SearchCustomerVouchersUseCase(PromotionRepositoryImpl(ds))

    @Test
    fun on401_refreshesOnce_thenRetriesWithNewToken() {
        val host = FakeHost(token = "dead", freshToken = "alive")
        val seen = AuthLog()
        val useCase = useCaseOf(dataSourceOf(host, validToken = "alive", seen = seen))

        runBlocking { useCase(SearchCustomerVouchersRequest()) }

        assertEquals(1, host.refreshCount)
        assertEquals(listOf("Bearer dead", "Bearer alive"), seen.snapshot())
    }

    @Test
    fun whenHostCannotRefresh_errorSurfaces_andNoRetry() {
        val host = FakeHost(token = "dead", freshToken = null)
        val seen = AuthLog()
        val useCase = useCaseOf(dataSourceOf(host, validToken = "alive", seen = seen))

        val error = assertFailsWith<PromotionException> {
            runBlocking { useCase(SearchCustomerVouchersRequest()) }
        }

        assertEquals(401, error.httpStatus)
        assertEquals(1, host.refreshCount)
        // Chỉ một lượt gửi — refresh hỏng thì không thử lại, để `TOKEN_EXPIRED` nổi lên host.
        assertEquals(1, seen.snapshot().size)
    }

    @Test
    fun retryHappensOnce_notInALoop() {
        // Server 401 cả với token mới (phiên chết thật) → đúng 2 lượt gửi rồi dừng.
        val host = FakeHost(token = "dead", freshToken = "also-dead")
        val seen = AuthLog()
        val useCase = useCaseOf(dataSourceOf(host, validToken = "never-matches", seen = seen))

        assertFailsWith<PromotionException> {
            runBlocking { useCase(SearchCustomerVouchersRequest()) }
        }

        assertEquals(1, host.refreshCount)
        assertEquals(2, seen.snapshot().size)
    }

    @Test
    fun concurrent401s_askHostToRefreshOnlyOnce() {
        // Mở một màn là vài request bay song song; token chết thì tất cả cùng ăn 401. Host chỉ được
        // hỏi MỘT lần — `generation` của cổng lo phần này.
        val host = FakeHost(token = "dead", freshToken = "alive")
        val seen = AuthLog()
        val useCase = useCaseOf(dataSourceOf(host, validToken = "alive", seen = seen))

        runBlocking {
            List(4) { async { useCase(SearchCustomerVouchersRequest()) } }.awaitAll()
        }

        assertEquals(1, host.refreshCount)
        // Mỗi lượt gọi phải kết thúc bằng đúng một request mang token mới — không hơn, không kém.
        val log = seen.snapshot()
        assertEquals(4, log.count { it == "Bearer alive" }, "log: $log")
    }

    @Test
    fun non401Errors_areNotRetried() {
        val host = FakeHost(token = "any", freshToken = "alive")
        val seen = AuthLog()
        val client = HttpClient(MockEngine {
            seen.record(it.headers[HttpHeaders.Authorization].orEmpty())
            respond("""{"code":"BOOM","message":"server"}""", HttpStatusCode.InternalServerError, jsonHeaders)
        }) {
            with(PromotionHttpClient) { configure("https://api.example.com", host, false) }
        }
        val useCase = useCaseOf(PromotionRemoteDataSource(KtorPromotionApiService(client), TokenRefreshGate(host)))

        val error = assertFailsWith<PromotionException> {
            runBlocking { useCase(SearchCustomerVouchersRequest()) }
        }

        assertEquals("BOOM", error.errorCode)
        assertEquals(0, host.refreshCount)
        assertEquals(1, seen.snapshot().size)
    }

    @Test
    fun featureFlag401_khongThuLai() {
        // Cờ tính năng fail-open: 401 ở đó chỉ rơi về cache, không được kéo theo một lượt refresh.
        // Nếu có, mở app với token chết sẽ kích refresh ngay từ `initialize` — ngoài ý muốn.
        val host = FakeHost(token = "dead", freshToken = "alive")
        val seen = AuthLog()
        val client = HttpClient(MockEngine {
            seen.record(it.headers[HttpHeaders.Authorization].orEmpty())
            respond("""{"code":"UNAUTHORIZED"}""", HttpStatusCode.Unauthorized, jsonHeaders)
        }) {
            with(PromotionHttpClient) { configure("https://api.example.com", host, false) }
        }
        val ds = FeatureFlagRemoteDataSource(KtorFeatureFlagApiService(client))

        assertFailsWith<FeatureFlagException> { runBlocking { ds.getFeatureFlags() } }

        assertEquals(0, host.refreshCount)
        assertEquals(1, seen.snapshot().size)
    }

    @Test
    fun envelope401_onHttp200_alsoTriggersRefresh() {
        // Server bọc lỗi trong envelope: HTTP 200 nhưng `status: 401`. `expectSuccess` không bắt
        // được đường này — nếu chỉ nhìn ResponseException thì cơ chế thử lại im lặng không chạy.
        val host = FakeHost(token = "dead", freshToken = "alive")
        val seen = AuthLog()
        val client = HttpClient(MockEngine { r: HttpRequestData ->
            val auth = r.headers[HttpHeaders.Authorization].orEmpty()
            seen.record(auth)
            if (auth == "Bearer alive") {
                respond(okBody, HttpStatusCode.OK, jsonHeaders)
            } else {
                respond(
                    """{"status":401,"success":false,"code":"${PromotionErrorCodes.TOKEN_EXPIRED}","message":"expired"}""",
                    HttpStatusCode.OK,
                    jsonHeaders,
                )
            }
        }) {
            with(PromotionHttpClient) { configure("https://api.example.com", host, false) }
        }
        val useCase = useCaseOf(PromotionRemoteDataSource(KtorPromotionApiService(client), TokenRefreshGate(host)))

        runBlocking { useCase(SearchCustomerVouchersRequest()) }

        assertEquals(1, host.refreshCount)
        assertEquals(listOf("Bearer dead", "Bearer alive"), seen.snapshot())
    }
}
