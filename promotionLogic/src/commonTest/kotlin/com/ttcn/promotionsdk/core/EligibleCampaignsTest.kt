
package com.ttcn.promotionsdk.core

import com.ttcn.promotionsdk.core.data.remote.KtorPromotionApiService
import com.ttcn.promotionsdk.core.data.remote.PromotionHttpClient
import com.ttcn.promotionsdk.core.data.remote.PromotionRemoteDataSource
import com.ttcn.promotionsdk.core.data.repository.PromotionRepositoryImpl
import com.ttcn.promotionsdk.core.domain.model.PromotionResult
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffersResult
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOrderItem
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleSection
import com.ttcn.promotionsdk.core.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.core.domain.usecase.CreateRedemptionSessionUseCase
import com.ttcn.promotionsdk.core.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.core.domain.usecase.GetCustomerVoucherDetailUseCase
import com.ttcn.promotionsdk.core.domain.usecase.PromotionUseCases
import com.ttcn.promotionsdk.core.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.core.domain.usecase.ValidateStackableDiscountsUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

private class NoTokenContextProvider : com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider

private fun eligibleUseCases(
    captured: MutableList<HttpRequestData>,
    responseJson: String,
): PromotionUseCases {
    val engine = MockEngine { request ->
        captured.add(request)
        respond(responseJson, HttpStatusCode.OK, jsonHeaders)
    }
    val client = HttpClient(engine) {
        with(PromotionHttpClient) {
            configure("https://api.example.com", NoTokenContextProvider(), isDebug = false)
        }
    }
    val repo = PromotionRepositoryImpl(PromotionRemoteDataSource(KtorPromotionApiService(client)))
    return PromotionUseCases(
        searchVouchersUseCase = SearchCustomerVouchersUseCase(repo),
        voucherDetailUseCase = GetCustomerVoucherDetailUseCase(repo),
        validateDiscountsUseCase = ValidateStackableDiscountsUseCase(repo),
        createRedemptionUseCase = CreateRedemptionSessionUseCase(repo),
        findEligibleCampaignsUseCase = FindEligibleCampaignsUseCase(repo),
    )
}

private const val ELIGIBLE_RESPONSE = """
{
  "success": true,
  "data": {
    "activeTab": "all",
    "tabs": [
      {"code":"expiring","label":"Sắp hết hạn","order":2,"count":1},
      {"code":"all","label":"Tất cả","default":true,"order":1,"count":3}
    ],
    "myOffers": {
      "content": [
        {
          "campaignId": "camp-1", "voucherId": "v-1", "campaignName": "Giảm 50k",
          "campaignType": "VOUCHER", "usable": true,
          "discountPreview": {"estimatedDiscount":"50000","minOrderValue":"200000"},
          "validity": {"endDate":"2026-12-31","remainingRedemptions":2},
          "budgetStatus": {"available": true}
        }
      ],
      "last": false, "totalElements": 5
    },
    "otherOffers": {
      "content": [
        {
          "campaignId": "camp-2", "campaignName": "Ưu đãi công khai",
          "usable": false, "displayMode": "DISABLED",
          "discountPreview": {"minOrderValue":"1000000"},
          "eligibilityDetails": {"eligible": false, "unmatchedRules": ["MIN_ORDER_VALUE"]}
        },
        { "campaignName": "Khong co id nao" }
      ],
      "last": true, "totalElements": 2
    }
  }
}
"""

class EligibleCampaignsTest {

    @Test
    fun findEligible_mapsBothGroups_andPrefersVoucherIdAsId() = runTest {
        val captured = mutableListOf<HttpRequestData>()
        val useCases = eligibleUseCases(captured, ELIGIBLE_RESPONSE)

        val result = useCases.findEligible(
            FindEligibleCampaignsRequest(
                orderId = "o-1",
                orderValue = "500000",
                items = listOf(EligibleOrderItem(skuId = "SKU-1", quantity = 1, unitPrice = "500000")),
            )
        )

        val data = assertIs<PromotionResult.Success<EligibleOffersResult>>(result).data

        assertEquals(1, data.myOffers.size)
        val mine = data.myOffers.single()
        assertEquals("v-1", mine.id)            // voucherId ưu tiên hơn campaignId
        assertEquals("camp-1", mine.campaignId)
        assertTrue(mine.isOwnedVoucher)
        assertTrue(mine.usable)
        assertEquals("50000", mine.estimatedDiscount)
        assertEquals("VOUCHER", mine.objectType)
        assertEquals("2026-12-31", mine.expireDate)

        // Offer thứ hai của nhóm other không có voucherId lẫn campaignId → bị loại.
        assertEquals(1, data.otherOffers.size)
        val other = data.otherOffers.single()
        assertEquals("camp-2", other.id)
        assertNull(other.voucherId)
        assertTrue(!other.isOwnedVoucher)
        assertTrue(!other.usable)
        assertEquals("1000000", other.minOrderValue)
        assertEquals(listOf("MIN_ORDER_VALUE"), other.unmatchedRules)
        assertEquals("CAMPAIGN", other.objectType)   // campaignType null → fallback

        assertEquals(false, data.myIsLastPage)
        assertEquals(true, data.otherIsLastPage)
        assertEquals(5, data.myTotalElements)
        assertEquals(2, data.otherTotalElements)
        assertEquals("all", data.activeTab)
    }

    @Test
    fun findEligible_sortsTabsByOrder() = runTest {
        val useCases = eligibleUseCases(mutableListOf(), ELIGIBLE_RESPONSE)

        val data = assertIs<PromotionResult.Success<EligibleOffersResult>>(
            useCases.findEligible(FindEligibleCampaignsRequest("o-1", "500000"))
        ).data

        assertEquals(listOf("all", "expiring"), data.tabs.map { it.code })
        assertTrue(data.tabs.first().isDefault)
    }

    @Test
    fun findEligible_buildsPaginationAndSectionPayload() = runTest {
        val captured = mutableListOf<HttpRequestData>()
        val useCases = eligibleUseCases(captured, ELIGIBLE_RESPONSE)

        useCases.findEligible(
            FindEligibleCampaignsRequest(
                orderId = "o-1",
                orderValue = "500000",
                items = listOf(EligibleOrderItem(skuId = "SKU-1", quantity = 2, unitPrice = "250000")),
                tabCode = "expiring",
                section = EligibleSection.OTHER_OFFERS,
                otherPage = 3,
            )
        )

        val request = captured.single()
        assertTrue(request.url.encodedPath.endsWith("/api/v1/vtm/redemptions/eligible"), request.url.encodedPath)

        val body = (request.body as TextContent).text
        assertTrue("\"sectionCode\":\"other_offers\"" in body, body)
        // v1.6 bỏ tabCode khỏi request (không còn thanh tab).
        assertTrue("tabCode" !in body, body)
        assertTrue("\"otherOffers\":{\"page\":3,\"size\":10}" in body, body)
        assertTrue("\"myOffers\":{\"page\":0,\"size\":10}" in body, body)
        // Item dùng skuSourceId (v1.6) thay cho skuId.
        assertTrue("\"skuSourceId\":\"SKU-1\"" in body, body)
        assertTrue("\"currency\":\"VND\"" in body, body)
        assertTrue("\"checkBudgetAvailability\":true" in body, body)
    }

    @Test
    fun findEligible_omitsTabCodeAndSectionWhenNotSet() = runTest {
        val captured = mutableListOf<HttpRequestData>()
        val useCases = eligibleUseCases(captured, ELIGIBLE_RESPONSE)

        useCases.findEligible(FindEligibleCampaignsRequest("o-1", "500000"))

        val body = (captured.single().body as TextContent).text
        assertTrue("sectionCode" !in body, body)
        assertTrue("tabCode" !in body, body)
    }
}
