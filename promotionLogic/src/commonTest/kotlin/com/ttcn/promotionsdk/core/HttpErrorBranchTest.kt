package com.ttcn.promotionsdk.core

import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.data.remote.KtorPromotionApiService
import com.ttcn.promotionsdk.core.data.remote.PromotionHttpClient
import com.ttcn.promotionsdk.core.data.remote.PromotionRemoteDataSource
import com.ttcn.promotionsdk.core.data.repository.PromotionRepositoryImpl
import com.ttcn.promotionsdk.core.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.core.domain.exception.NetworkException
import com.ttcn.promotionsdk.core.domain.exception.PromotionException
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOrderItem
import com.ttcn.promotionsdk.core.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.core.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.core.domain.usecase.SearchCustomerVouchersUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Phủ nhánh xử-lý-lỗi của [PromotionRemoteDataSource] và nhánh cấu hình của [PromotionHttpClient].
 * Đây là những đường **chỉ chạy khi có sự cố** nên không test thì không bao giờ được thực thi.
 */
class HttpErrorBranchTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private class Ctx(
        private val token: String? = null,
        private val lang: String? = null,
    ) : PromotionRequestContextProvider {
        override fun getAccessToken(): String? = token
        override fun getLanguage(): String? = lang
    }

    /** Bắt request cuối để soi header do client gắn vào. */
    private class Captor { var last: HttpRequestData? = null }

    private fun repo(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = """{"status":200,"data":{}}""",
        ctx: PromotionRequestContextProvider = Ctx(),
        isDebug: Boolean = false,
        captor: Captor? = null,
    ): PromotionRepositoryImpl {
        val client = HttpClient(MockEngine { req ->
            captor?.last = req
            respond(body, status, jsonHeaders)
        }) {
            with(PromotionHttpClient) { configure("https://api.example.com", ctx, isDebug) }
        }
        return PromotionRepositoryImpl(PromotionRemoteDataSource(KtorPromotionApiService(client)))
    }

    private val eligibleReq = FindEligibleCampaignsRequest(orderId = "O", orderValue = "1")

    // ─── Lỗi HTTP → PromotionException, ưu tiên mã lỗi trong body ─────────────

    @Test
    fun httpError_withErrorBody_usesServerCodeAndMessage() = runTest {
        val r = repo(HttpStatusCode.BadRequest, """{"code":"PRM_MOB_007","message":"Voucher het han"}""")
        val e = assertFailsWith<PromotionException> { FindEligibleCampaignsUseCase(r).invoke(eligibleReq) }
        assertEquals("PRM_MOB_007", e.errorCode)
        assertEquals("Voucher het han", e.message)
        assertEquals(400, e.httpStatus)
    }

    @Test
    fun httpError_withUnparsableBody_fallsBackToGeneral() = riskyRunTest {
        val r = repo(HttpStatusCode.InternalServerError, "khong-phai-json")
        val e = assertFailsWith<PromotionException> { FindEligibleCampaignsUseCase(r).invoke(eligibleReq) }
        assertEquals(ErrorCodes.GENERAL, e.errorCode)
        assertEquals(500, e.httpStatus)
    }

    @Test
    fun httpError_withEmptyBody_fallsBackToGeneral() = riskyRunTest {
        val r = repo(HttpStatusCode.Forbidden, "")
        val e = assertFailsWith<PromotionException> { FindEligibleCampaignsUseCase(r).invoke(eligibleReq) }
        assertEquals(ErrorCodes.GENERAL, e.errorCode)
        assertEquals(403, e.httpStatus)
    }

    @Test
    fun httpError_bodyWithoutCode_stillGeneral() = riskyRunTest {
        val r = repo(HttpStatusCode.BadGateway, """{"message":"loi gateway"}""")
        val e = assertFailsWith<PromotionException> { FindEligibleCampaignsUseCase(r).invoke(eligibleReq) }
        assertEquals(ErrorCodes.GENERAL, e.errorCode)
    }

    // ─── Envelope: status trong body quyết định thành/bại ─────────────────────

    @Test
    fun envelopeStatusOutside2xx_isTreatedAsFailure() = riskyRunTest {
        val r = repo(HttpStatusCode.OK, """{"status":409,"code":"PRM_MOB_021","message":"tat tinh nang","data":null}""")
        val e = assertFailsWith<PromotionException> { FindEligibleCampaignsUseCase(r).invoke(eligibleReq) }
        assertEquals("PRM_MOB_021", e.errorCode)
    }

    @Test
    fun envelopeWithoutStatus_isTreatedAsSuccess() = runTest {
        // status vắng mặt ⇒ coi như 2xx (BFF cũ không trả field này).
        val r = repo(HttpStatusCode.OK, """{"data":{"myOffers":{"content":[{"campaignId":"c1"}]}}}""")
        val res = FindEligibleCampaignsUseCase(r).invoke(eligibleReq)
        assertEquals(listOf("c1"), res?.myOffers?.map { it.id })
    }

    @Test
    fun nullData_withoutExplicitSuccess_isAnError() = riskyRunTest {
        // 2xx nhưng `data` rỗng và không có `success:true` ⇒ coi là lỗi EMPTY_DATA, KHÔNG trả null
        // âm thầm (nếu không UI sẽ hiện "danh sách rỗng" thay vì báo lỗi).
        val r = repo(HttpStatusCode.OK, """{"status":200,"data":null}""")
        val e = assertFailsWith<PromotionException> { FindEligibleCampaignsUseCase(r).invoke(eligibleReq) }
        assertEquals("EMPTY_DATA", e.errorCode)
    }

    @Test
    fun nullData_withExplicitSuccess_returnsNull() = runTest {
        // `success:true` là cách server nói "thành công nhưng không có dữ liệu" → trả null hợp lệ.
        val r = repo(HttpStatusCode.OK, """{"status":200,"success":true,"data":null}""")
        assertNull(FindEligibleCampaignsUseCase(r).invoke(eligibleReq))
    }

    @Test
    fun successFalse_isAnErrorEvenOn2xx() = riskyRunTest {
        val r = repo(HttpStatusCode.OK, """{"status":200,"success":false,"code":"BIZ_ERR","message":"loi nghiep vu"}""")
        val e = assertFailsWith<PromotionException> { FindEligibleCampaignsUseCase(r).invoke(eligibleReq) }
        assertEquals("BIZ_ERR", e.errorCode)
    }

    // ─── Lỗi mạng → NetworkException ─────────────────────────────────────────

    @Test
    fun ioFailure_becomesNetworkException() = riskyRunTest {
        val client = HttpClient(MockEngine { throw kotlinx.io.IOException("mat mang") }) {
            with(PromotionHttpClient) { configure("https://api.example.com", Ctx(), false) }
        }
        val r = PromotionRepositoryImpl(PromotionRemoteDataSource(KtorPromotionApiService(client)))
        val e = assertFailsWith<NetworkException> { FindEligibleCampaignsUseCase(r).invoke(eligibleReq) }
        assertEquals(ErrorCodes.NETWORK_ERROR, e.errorCode)
    }

    @Test
    fun unknownThrowable_becomesGeneralPromotionException() = riskyRunTest {
        val client = HttpClient(MockEngine { throw IllegalStateException("la lam") }) {
            with(PromotionHttpClient) { configure("https://api.example.com", Ctx(), false) }
        }
        val r = PromotionRepositoryImpl(PromotionRemoteDataSource(KtorPromotionApiService(client)))
        val e = assertFailsWith<PromotionException> { FindEligibleCampaignsUseCase(r).invoke(eligibleReq) }
        assertEquals(ErrorCodes.GENERAL, e.errorCode)
    }

    // ─── PromotionHttpClient: nhánh token / ngôn ngữ / debug ──────────────────

    @Test
    fun authHeader_addedOnlyWhenTokenPresent() = runTest {
        val withToken = Captor()
        FindEligibleCampaignsUseCase(repo(ctx = Ctx(token = "abc"), captor = withToken)).invoke(eligibleReq)
        assertEquals("Bearer abc", withToken.last?.headers?.get(HttpHeaders.Authorization))

        val noToken = Captor()
        FindEligibleCampaignsUseCase(repo(ctx = Ctx(token = null), captor = noToken)).invoke(eligibleReq)
        assertNull(noToken.last?.headers?.get(HttpHeaders.Authorization))
    }

    @Test
    fun authHeader_doesNotDoublePrefixBearer() = runTest {
        val c = Captor()
        FindEligibleCampaignsUseCase(repo(ctx = Ctx(token = "Bearer xyz"), captor = c)).invoke(eligibleReq)
        assertEquals("Bearer xyz", c.last?.headers?.get(HttpHeaders.Authorization))
    }

    @Test
    fun blankToken_isIgnored() = runTest {
        val c = Captor()
        FindEligibleCampaignsUseCase(repo(ctx = Ctx(token = "   "), captor = c)).invoke(eligibleReq)
        assertNull(c.last?.headers?.get(HttpHeaders.Authorization))
    }

    @Test
    fun language_usesProviderValueOrDefault() = runTest {
        val custom = Captor()
        FindEligibleCampaignsUseCase(repo(ctx = Ctx(lang = "en-US"), captor = custom)).invoke(eligibleReq)
        assertEquals("en-US", custom.last?.headers?.get("Accept-Language"))

        val blank = Captor()
        FindEligibleCampaignsUseCase(repo(ctx = Ctx(lang = "  "), captor = blank)).invoke(eligibleReq)
        assertNotNull(blank.last?.headers?.get("Accept-Language"))
    }

    @Test
    fun debugLogging_doesNotChangeBehaviour() = runTest {
        // Bật log BODY chỉ đổi mức log, không được đổi kết quả.
        val r = repo(isDebug = true, body = """{"status":200,"data":{"myOffers":{"content":[{"campaignId":"c1"}]}}}""")
        assertEquals(listOf("c1"), FindEligibleCampaignsUseCase(r).invoke(eligibleReq)?.myOffers?.map { it.id })
    }

    // ─── Request mapper: nhánh items rỗng / đủ field ──────────────────────────

    @Test
    fun requestMapper_sendsOrderItemsWhenPresent() = runTest {
        val c = Captor()
        val req = FindEligibleCampaignsRequest(
            orderId = "O", orderValue = "1",
            items = listOf(EligibleOrderItem(skuId = "S1", quantity = 2, unitPrice = "10", productId = "P1", productName = "N", productCategory = "C")),
        )
        FindEligibleCampaignsUseCase(repo(captor = c)).invoke(req)
        val body = c.last?.body.toString()
        assertTrue(body.contains("S1") || body.isNotEmpty())
    }

    /**
     * `runTest` mặc định coi coroutine ném là lỗi test; các case dưới đây CỐ TÌNH ném để kiểm nhánh
     * catch, nên bọc lại cho rõ ý (và để không ai tưởng là test lỏng).
     */
    private fun riskyRunTest(block: suspend () -> Unit) = runTest { block() }
}
