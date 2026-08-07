package com.ttcn.promotionsdk

import com.ttcn.promotionsdk.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.data.remote.KtorPromotionApiService
import com.ttcn.promotionsdk.data.remote.PromotionHttpClient
import com.ttcn.promotionsdk.data.remote.PromotionRemoteDataSource
import com.ttcn.promotionsdk.data.repository.PromotionRepositoryImpl
import com.ttcn.promotionsdk.domain.model.PromotionResult
import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffersResult
import com.ttcn.promotionsdk.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.domain.model.redemption.CreateRedemptionResult
import com.ttcn.promotionsdk.domain.model.redemption.RedemptionItemRequest
import com.ttcn.promotionsdk.domain.model.stackablediscount.DiscountItemRequest
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsResult
import com.ttcn.promotionsdk.domain.usecase.CreateRedemptionSessionUseCase
import com.ttcn.promotionsdk.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.domain.usecase.GetCustomerVoucherDetailUseCase
import com.ttcn.promotionsdk.domain.usecase.PromotionUseCases
import com.ttcn.promotionsdk.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.domain.usecase.ValidateStackableDiscountsUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Server trả **số** cho các field tiền tệ, còn DTO khai `String`.
 *
 * Gson (bản Retrofit cũ) tự ép số sang chuỗi nên Android không bao giờ lộ ra. kotlinx.serialization
 * nghiêm ngặt hơn và ném `JsonDecodingException`, khiến app iOS `abort()`:
 *
 *     Expected quotation mark '"', but had '5' instead at path: $.data.preview.originalAmount
 *     JSON input: ..."originalAmount":500000,"totalDiscount":75000,...
 *
 * `Json { isLenient = true }` khôi phục hành vi của Gson. Test này giữ cho nó không bị gỡ.
 */
private class NoTokenCtx : PromotionRequestContextProvider

private fun useCases(
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
): PromotionUseCases {
    val client = HttpClient(MockEngine(handler)) {
        with(PromotionHttpClient) { configure("https://api.example.com", NoTokenCtx(), false) }
    }
    val repo = PromotionRepositoryImpl(PromotionRemoteDataSource(KtorPromotionApiService(client)))
    return PromotionUseCases(
        SearchCustomerVouchersUseCase(repo),
        GetCustomerVoucherDetailUseCase(repo),
        ValidateStackableDiscountsUseCase(repo),
        CreateRedemptionSessionUseCase(repo),
        FindEligibleCampaignsUseCase(repo),
    )
}

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

class NumericAmountTest {

    @Test
    fun createRedemption_withNumericAmounts_parses() = runTest {
        // Payload lấy đúng từ crash log của demo iOS: số KHÔNG có ngoặc kép.
        val useCases = useCases {
            respond(
                """
                {"success":true,"data":{
                  "sessionId":"s-1","createdAt":"2026-07-09","expiresAt":"2026-07-09",
                  "preview":{
                    "orderId":"ORDER-1234","originalAmount":500000,"totalDiscount":75000,
                    "finalAmount":425000,"effectiveDiscountRate":0.15,"stackingMode":"BEST",
                    "appliedDiscounts":[{
                      "redeemableId":"v-1","redeemableType":"CAMPAIGN","redeemableName":"Giảm 75k",
                      "discountType":"AMOUNT","discountAmount":75000,"discountPercentage":15,
                      "priority":1,"appliedTo":"ORDER"
                    }]
                  },
                  "budgetHolds":[{"redeemableId":"v-1","holdId":"h-1","heldAmount":75000,
                                  "campaignId":"c-1","expiresAt":"2026-07-09"}],
                  "validationErrors":[]
                }}
                """.trimIndent(),
                HttpStatusCode.OK,
                jsonHeaders,
            )
        }

        val result = useCases.createRedemption(
            CreateRedemptionRequest(
                orderId = "ORDER-1234", orderValue = "500000",
                items = listOf(RedemptionItemRequest(objectId = "v-1", objectType = "CAMPAIGN")),
            )
        )

        val data = assertIs<PromotionResult.Success<CreateRedemptionResult>>(result).data
        assertEquals("s-1", data.sessionId)
        assertEquals("75000", data.totalDiscount)   // số 75000 → chuỗi "75000", như Gson
        assertEquals("425000", data.finalAmount)
        assertTrue(!data.hasErrors)
    }

    @Test
    fun validateDiscounts_withNumericAmounts_parses() = runTest {
        val useCases = useCases {
            respond(
                """
                {"success":true,"data":{
                  "validationResult":{"overallValid":true,"canStack":true,
                     "totalDiscountAmount":75000,"finalAmount":425000,"effectiveDiscountRate":0.15},
                  "discountDetails":[{"objectId":"v-1","objectType":"CAMPAIGN","valid":true,
                     "calculatedDiscount":75000,"eligibilityStatus":"ELIGIBLE"}]
                }}
                """.trimIndent(),
                HttpStatusCode.OK,
                jsonHeaders,
            )
        }

        val result = useCases.validateDiscounts(
            ValidateDiscountsRequest(
                orderId = "o-1", orderValue = "500000",
                items = listOf(DiscountItemRequest(objectId = "v-1")),
            )
        )

        val data = assertIs<PromotionResult.Success<ValidateDiscountsResult>>(result).data
        assertTrue(data.overallValid)
        assertEquals("75000", data.totalDiscountAmount)
        assertEquals("75000", data.items.single().calculatedDiscount)
    }

    @Test
    fun findEligible_withNumericDiscountPreview_parses() = runTest {
        val useCases = useCases {
            respond(
                """
                {"success":true,"data":{
                  "myOffers":{"content":[{"campaignId":"c-1","voucherId":"v-1","campaignName":"Giảm 50k",
                     "usable":true,
                     "discountPreview":{"estimatedDiscount":50000,"discountPercentage":10,
                                        "maxDiscount":100000,"minOrderValue":200000},
                     "budgetStatus":{"available":true,"remainingBudget":1000000}}],
                     "last":true,"totalElements":1},
                  "otherOffers":{"content":[],"last":true,"totalElements":0}
                }}
                """.trimIndent(),
                HttpStatusCode.OK,
                jsonHeaders,
            )
        }

        val result = useCases.findEligible(
            FindEligibleCampaignsRequest(orderId = "o-1", orderValue = "500000")
        )

        val data = assertIs<PromotionResult.Success<EligibleOffersResult>>(result).data
        val offer = data.myOffers.single()
        assertEquals("50000", offer.estimatedDiscount)
        assertEquals("200000", offer.minOrderValue)
    }
}
