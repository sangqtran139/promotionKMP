package com.ttcn.promotionsdk.core

import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.data.remote.KtorPromotionApiService
import com.ttcn.promotionsdk.core.data.remote.PromotionHttpClient
import com.ttcn.promotionsdk.core.data.remote.PromotionRemoteDataSource
import com.ttcn.promotionsdk.core.data.repository.PromotionRepositoryImpl
import com.ttcn.promotionsdk.core.domain.model.PromotionResult
import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.core.domain.model.redemption.RedemptionItemRequest
import com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersRequest
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherDisplayState
import com.ttcn.promotionsdk.core.domain.model.voucher.displayState
import com.ttcn.promotionsdk.core.domain.usecase.CreateRedemptionSessionUseCase
import com.ttcn.promotionsdk.core.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.core.domain.usecase.GetCustomerVoucherDetailUseCase
import com.ttcn.promotionsdk.core.domain.usecase.PromotionUseCases
import com.ttcn.promotionsdk.core.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.core.domain.usecase.ValidateStackableDiscountsUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val BASE_URL = "https://api.example.com"

private class FakeContextProvider(
    private val token: String? = "abc123",
    private val language: String? = null,
) : PromotionRequestContextProvider {
    override fun getAccessToken(): String? = token
    override fun getLanguage(): String? = language
}

private fun HttpRequestData.bodyText(): String = (body as TextContent).text

private fun useCasesWith(
    provider: PromotionRequestContextProvider = FakeContextProvider(),
    capture: MutableList<HttpRequestData>? = null,
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
): PromotionUseCases {
    val engine = MockEngine { request ->
        capture?.add(request)
        handler(request)
    }
    val client = HttpClient(engine) {
        with(PromotionHttpClient) { configure(BASE_URL, provider, isDebug = false) }
    }
    val repository = PromotionRepositoryImpl(PromotionRemoteDataSource(KtorPromotionApiService(client)))
    return PromotionUseCases(
        searchVouchersUseCase = SearchCustomerVouchersUseCase(repository),
        voucherDetailUseCase = GetCustomerVoucherDetailUseCase(repository),
        validateDiscountsUseCase = ValidateStackableDiscountsUseCase(repository),
        createRedemptionUseCase = CreateRedemptionSessionUseCase(repository),
        findEligibleCampaignsUseCase = FindEligibleCampaignsUseCase(repository),
    )
}

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

class PromotionPipelineTest {

    @Test
    fun searchVouchers_parsesEnvelopeAndMapsToDomain() = runTest {
        val captured = mutableListOf<HttpRequestData>()
        val useCases = useCasesWith(capture = captured) {
            respond(
                content = """
                {
                  "status": 200, "success": true,
                  "data": {
                    "selectedTab": "ALL",
                    "tabs": [{"code":"ALL","label":"Tất cả","default":true,"count":2}],
                    "content": [
                      {
                        "voucherId": "v-1",
                        "merchantName": "Viettel",
                        "title": "Giảm 50k",
                        "campaignType": "CAMPAIGN",
                        "status": "AVAILABLE_TO_CLAIM",
                        "isAutoApplied": true,
                        "applicableProducts": [
                          {"productId":"p-1","name":"Data 5G","type":"DATA"}
                        ]
                      }
                    ],
                    "totalElements": 1, "last": true, "number": 0, "size": 20
                  }
                }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders,
            )
        }

        val result = useCases.searchVouchers(SearchCustomerVouchersRequest(customerId = "c-1", tab = "ALL"))

        val success = assertIs<PromotionResult.Success<*>>(result)
        val data = success.data as com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersResult
        assertEquals(1, data.content.size)
        assertEquals("v-1", data.content.first().voucherId)
        assertEquals("CAMPAIGN", data.content.first().objectType)
        assertEquals("Data 5G", data.content.first().applicableProducts.first().name)
        // isAutoApplied phải đi hết đường JSON → DTO → domain, nếu không voucher tự-áp-dụng
        // sẽ im lặng ngừng hoạt động (bản Kotlin trước đây thiếu hẳn field này).
        assertTrue(data.content.first().isAutoApplied)
        assertEquals(VoucherDisplayState.USABLE, data.content.first().displayState())
        assertEquals(1, data.tabs.size)
        assertTrue(data.tabs.first().isDefault)

        // Query params thay cho @Query của Retrofit: null bị bỏ qua, giá trị có mặt được gửi.
        val url = captured.single().url
        assertEquals("c-1", url.parameters["customerId"])
        assertEquals("ALL", url.parameters["tab"])
        assertEquals(null, url.parameters["keyword"])
        assertTrue(url.encodedPath.endsWith("/api/v1/vtm/customer-vouchers"), url.encodedPath)
    }

    @Test
    fun defaultRequest_appliesBearerTokenLanguageAndRequestId() = runTest {
        val captured = mutableListOf<HttpRequestData>()
        val useCases = useCasesWith(
            provider = FakeContextProvider(token = "abc123", language = null),
            capture = captured,
        ) {
            respond("""{"success":true,"data":{"voucherId":"v-1"}}""", HttpStatusCode.OK, jsonHeaders)
        }

        useCases.getVoucherDetail(voucherId = "v-1", customerId = "c-1")

        val headers = captured.single().headers
        assertEquals("Bearer abc123", headers[HttpHeaders.Authorization])
        assertEquals("vi-VN", headers[HttpHeaders.AcceptLanguage])
        assertNotNull(headers["X-Request-ID"])
    }

    @Test
    fun bearerPrefix_isNotDuplicatedWhenAlreadyPresent() = runTest {
        val captured = mutableListOf<HttpRequestData>()
        val useCases = useCasesWith(
            provider = FakeContextProvider(token = "Bearer xyz"),
            capture = captured,
        ) {
            respond("""{"success":true,"data":{"voucherId":"v-1"}}""", HttpStatusCode.OK, jsonHeaders)
        }

        useCases.getVoucherDetail(voucherId = "v-1", customerId = "c-1")

        assertEquals("Bearer xyz", captured.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun httpError_isMappedToFailureWithServerCodeAndStatus() = runTest {
        val useCases = useCasesWith {
            respondError(
                status = HttpStatusCode.BadRequest,
                content = """{"code":"VOUCHER_EXPIRED","message":"Voucher hết hạn"}""",
                headers = jsonHeaders,
            )
        }

        val result = useCases.getVoucherDetail(voucherId = "v-1", customerId = "c-1")

        val failure = assertIs<PromotionResult.Failure>(result)
        assertEquals("VOUCHER_EXPIRED", failure.errorCode)
        assertEquals("Voucher hết hạn", failure.message)
        assertEquals(400, failure.httpStatus)
    }

    @Test
    fun businessError_inEnvelope_isMappedToFailure() = runTest {
        val useCases = useCasesWith {
            respond(
                """{"status":200,"success":false,"code":"NOT_ELIGIBLE","message":"Không đủ điều kiện"}""",
                HttpStatusCode.OK,
                jsonHeaders,
            )
        }

        val result = useCases.getVoucherDetail(voucherId = "v-1", customerId = "c-1")

        val failure = assertIs<PromotionResult.Failure>(result)
        assertEquals("NOT_ELIGIBLE", failure.errorCode)
        assertEquals(200, failure.httpStatus)
    }

    @Test
    fun createRedemption_sendsIdempotencyKeyAndDefaults_thenMapsResult() = runTest {
        val captured = mutableListOf<HttpRequestData>()
        val useCases = useCasesWith(capture = captured) {
            respond(
                """
                {
                  "success": true,
                  "data": {
                    "sessionId": "s-1",
                    "preview": {
                      "orderId": "o-1", "originalAmount": "100000",
                      "totalDiscount": "50000", "finalAmount": "50000"
                    },
                    "validationErrors": [
                      {"field":"budget","code":"INSUFFICIENT_BUDGET","message":"Hết ngân sách"}
                    ]
                  }
                }
                """.trimIndent(),
                HttpStatusCode.OK,
                jsonHeaders,
            )
        }

        val result = useCases.createRedemption(
            CreateRedemptionRequest(
                customerId = "c-1",
                orderId = "o-1",
                orderValue = "100000",
                items = listOf(RedemptionItemRequest(objectId = "v-1", objectType = "CAMPAIGN")),
            )
        )

        val success = assertIs<PromotionResult.Success<*>>(result)
        val data = success.data as com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionResult
        assertEquals("s-1", data.sessionId)
        assertEquals("50000", data.totalDiscount)
        assertTrue(data.hasErrors)
        assertTrue(data.hasBudgetError)

        // encodeDefaults=true giữ payload giống Gson: sessionOptions/priority phải có mặt,
        // idempotencyKey do randomUuidString() sinh (thay java.util.UUID).
        val body = captured.single().bodyText()
        assertTrue("\"idempotencyKey\"" in body, body)
        assertTrue("\"sessionOptions\"" in body, body)
        assertTrue("\"timeoutSeconds\":300" in body, body)
        assertTrue("\"priority\":1" in body, body)
        assertTrue("\"customerId\":\"c-1\"" in body, body)
    }
}
