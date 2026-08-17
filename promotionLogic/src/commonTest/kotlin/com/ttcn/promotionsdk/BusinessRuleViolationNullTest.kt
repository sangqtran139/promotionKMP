package com.ttcn.promotionsdk

import com.ttcn.promotionsdk.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.data.remote.KtorPromotionApiService
import com.ttcn.promotionsdk.data.remote.PromotionHttpClient
import com.ttcn.promotionsdk.data.remote.PromotionRemoteDataSource
import com.ttcn.promotionsdk.data.repository.PromotionRepositoryImpl
import com.ttcn.promotionsdk.domain.model.stackablediscount.DiscountItemRequest
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.domain.usecase.ValidateStackableDiscountsUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/**
 * `businessRuleViolations[].ruleCode = null` **không được** làm hỏng cả response.
 *
 * Field này từng khai `String` không-null, và kotlinx.serialization ném ngay lúc parse:
 *
 *     Unexpected 'null' value instead of string literal
 *     at path: $.data.businessRuleViolations[0].ruleCode
 *
 * Vì nó ném khi đọc **cả body**, toàn bộ kết quả validate mất trắng — bấm "Áp dụng" xong widget
 * không hiện gì, còn lý do thật ("Voucher not found") nằm ngay trong body mà không ai đọc được.
 *
 * Body dưới đây là response **thật** của server, giữ nguyên.
 */
class BusinessRuleViolationNullTest {

    private class Ctx : PromotionRequestContextProvider {
        override fun getAccessToken() = "tok"
        override fun getOrderId() = "ORD-DEMO-001"
        override fun getOrderValue() = "500000"
    }

    private val body = """
        {"status":200,"code":"SUCCESS","success":true,"data":{
          "validationResult":{"overallValid":false,"canStack":true,"totalDiscountAmount":0,
            "finalAmount":500000,"effectiveDiscountRate":0.0,
            "validationSummary":"Some discounts cannot be applied due to business rule violations"},
          "decisionToken":null,"sessionId":null,
          "stackingAnalysis":{"stackableGroups":[],"conflicts":[],"exclusions":[]},
          "discountDetails":[{"objectId":"019fabb9-2860-7549-ab0c-018f64632302","objectType":"VOUCHER",
            "valid":false,"calculatedDiscount":0,"eligibilityStatus":"NOT_ELIGIBLE_ZERO_DISCOUNT",
            "budgetStatus":"UNKNOWN","validationMessages":["No discount calculated or zero discount"],
            "tags":[],"metadata":{"expectedDiscount":null,"maxDiscountCap":null,"priority":1}}],
          "warnings":[],
          "businessRuleViolations":[{"ruleCode":null,"message":"Voucher not found"}]},
          "errors":[]}
    """.trimIndent()

    @Test
    fun nullRuleCode_stillParses_andKeepsReason() = runTest {
        val client = HttpClient(MockEngine {
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }) { with(PromotionHttpClient) { configure("https://api.example.com", Ctx(), false) } }

        val repo = PromotionRepositoryImpl(PromotionRemoteDataSource(KtorPromotionApiService(client)))
        val result = ValidateStackableDiscountsUseCase(repo).invoke(
            ValidateDiscountsRequest(
                orderId = "ORD-DEMO-001",
                orderValue = "500000",
                items = listOf(
                    DiscountItemRequest(
                        objectId = "019fabb9-2860-7549-ab0c-018f64632302",
                        objectType = "VOUCHER",
                    ),
                ),
            ),
        )

        assertNotNull(result, "ruleCode=null KHÔNG được làm hỏng cả response")
        assertFalse(result.overallValid)

        // Lý do phải tới được tầng trên — trước đây parse xong là bỏ, nay giữ lại.
        assertEquals(listOf("Voucher not found"), result.businessRuleViolations)
        assertEquals(
            listOf("No discount calculated or zero discount"),
            result.reasonFor("019fabb9-2860-7549-ab0c-018f64632302"),
            "có validationMessages riêng thì ưu tiên nó hơn lý do cấp đơn",
        )
    }
}
