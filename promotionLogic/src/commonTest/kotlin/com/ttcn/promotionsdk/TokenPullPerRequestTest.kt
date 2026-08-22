package com.ttcn.promotionsdk

import com.ttcn.promotionsdk.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.data.remote.KtorPromotionApiService
import com.ttcn.promotionsdk.data.remote.PromotionHttpClient
import com.ttcn.promotionsdk.data.remote.PromotionRemoteDataSource
import com.ttcn.promotionsdk.data.repository.PromotionRepositoryImpl
import com.ttcn.promotionsdk.domain.model.voucher.SearchCustomerVouchersRequest
import com.ttcn.promotionsdk.domain.usecase.SearchCustomerVouchersUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Token là **pull, không phải push**: lõi hỏi lại [PromotionRequestContextProvider.getAccessToken]
 * ở từng request thay vì chụp một lần lúc dựng `HttpClient`.
 *
 * Đây là điều kiện để host có cơ chế refresh riêng (token sống ~15 phút) dùng được SDK: host thay
 * token trong kho của mình, lượt gọi API kế tiếp của SDK phải mang token mới — không cần
 * `PromotionSDK.updateToken`, không dựng lại đồ thị DI. Nếu ai đó "tối ưu" bằng cách cache token
 * vào `defaultRequest` lúc dựng client, test này đỏ.
 */
class TokenPullPerRequestTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    /** Nguồn token đổi giá trị giữa hai lượt gọi — đúng như host refresh nền. */
    private class RotatingTokenCtx(var token: String) : PromotionRequestContextProvider {
        override fun getAccessToken(): String = token
    }

    private fun captureAuthHeaders(ctx: RotatingTokenCtx, between: () -> Unit): List<String> {
        val seen = mutableListOf<String>()
        val client = HttpClient(MockEngine { r: HttpRequestData ->
            seen += r.headers[HttpHeaders.Authorization].orEmpty()
            respond("""{"status":200,"success":true,"data":null}""", HttpStatusCode.OK, jsonHeaders)
        }) {
            with(PromotionHttpClient) { configure("https://api.example.com", ctx, false) }
        }
        val useCase = SearchCustomerVouchersUseCase(
            PromotionRepositoryImpl(PromotionRemoteDataSource(KtorPromotionApiService(client))),
        )
        runBlocking {
            useCase(SearchCustomerVouchersRequest())
            between()
            useCase(SearchCustomerVouchersRequest())
        }
        return seen
    }

    @Test
    fun accessToken_isReadAgainOnEveryRequest() {
        val ctx = RotatingTokenCtx("old-token")
        val headers = captureAuthHeaders(ctx) { ctx.token = "fresh-token" }

        assertEquals(2, headers.size)
        assertEquals("Bearer old-token", headers[0])
        // Client KHÔNG được dựng lại giữa hai lượt — token mới vẫn phải tới nơi.
        assertEquals("Bearer fresh-token", headers[1])
    }

    @Test
    fun khongCoToken_thiKhongGuiHeaderAuthorization() {
        // `currentToken()` trả rỗng → KHÔNG bịa ra giá trị nào, cũng không gửi "Bearer ".
        // Lỗi hiện ra ngay ở server thay vì SDK âm thầm gửi một token cũ.
        val ctx = RotatingTokenCtx("")
        val headers = captureAuthHeaders(ctx) {}

        assertEquals("", headers[0], "phải không có header Authorization")
    }

    @Test
    fun tokenCoKhoangTrang_thiDuocTrim() {
        val ctx = RotatingTokenCtx("  spaced-token  ")
        val headers = captureAuthHeaders(ctx) {}

        assertEquals("Bearer spaced-token", headers[0])
    }

    @Test
    fun bearerPrefix_isNotDuplicated_whenHostAlreadyPrefixes() {
        val ctx = RotatingTokenCtx("Bearer already-prefixed")
        val headers = captureAuthHeaders(ctx) {}

        assertEquals("Bearer already-prefixed", headers[0])
    }
}
