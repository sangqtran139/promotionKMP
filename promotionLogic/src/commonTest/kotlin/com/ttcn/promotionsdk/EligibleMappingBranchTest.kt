package com.ttcn.promotionsdk

import com.ttcn.promotionsdk.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.data.remote.KtorPromotionApiService
import com.ttcn.promotionsdk.data.remote.PromotionHttpClient
import com.ttcn.promotionsdk.data.remote.PromotionRemoteDataSource
import com.ttcn.promotionsdk.data.repository.PromotionRepositoryImpl
import com.ttcn.promotionsdk.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.domain.usecase.FindEligibleCampaignsUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Phủ **hai phía** của mọi `?:` trong `EligibleCampaignsMapper`: payload đầy đủ và payload tối
 * thiểu. Đây là chỗ dễ vỡ âm thầm khi BFF bỏ field — mapper toàn fallback nên không test thì
 * nhánh "thiếu field" không bao giờ chạy.
 */
class EligibleMappingBranchTest {

    private class NoCtx : PromotionRequestContextProvider
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun useCase(json: String): FindEligibleCampaignsUseCase {
        val client = HttpClient(MockEngine { respond(json, HttpStatusCode.OK, jsonHeaders) }) {
            with(PromotionHttpClient) { configure("https://api.example.com", NoCtx(), false) }
        }
        return FindEligibleCampaignsUseCase(
            PromotionRepositoryImpl(PromotionRemoteDataSource(KtorPromotionApiService(client))),
        )
    }

    private val request = FindEligibleCampaignsRequest(orderId = "O1", orderValue = "1000")

    @Test
    fun fullPayload_takesPreferredBranchOfEveryFallback() = runTest {
        val json = """
        {"status":200,"data":{
          "activeTab":"all",
          "expireWarningDate":7,
          "tabs":[{"code":"b","label":"B","count":2,"order":2,"default":false},
                  {"code":"a","label":"A","count":1,"order":1,"default":true}],
          "myOffers":{"content":[{
              "campaignId":"c1","campaignName":"Camp","campaignType":"VOUCHER","discountType":"PERCENT",
              "voucherId":"v1","voucherName":"Ten voucher","voucherCode":"CODE1",
              "logoUrl":"http://logo","partnerName":"Doi tac","usable":true,
              "expiresAt":"2099-01-01",
              "discountPreview":{"estimatedDiscount":"1000","discountPercentage":"10","maxDiscount":"5000","minOrderValue":"100"},
              "validity":{"startDate":"2020-01-01","endDate":"2098-01-01","remainingRedemptions":3},
              "budgetStatus":{"available":true},
              "eligibilityDetails":{"unmatchedRules":["r1"]}
          }],"last":false,"totalElements":42},
          "otherOffers":{"content":[],"last":true,"totalElements":9}
        }}
        """.trimIndent()

        val r = useCase(json).invoke(request)!!
        val o = r.myOffers.single()

        assertEquals("v1", o.id)                      // voucherId thắng campaignId
        assertEquals("Ten voucher", o.campaignName)   // voucherName thắng campaignName
        assertEquals("VOUCHER", o.objectType)         // có voucherId → VOUCHER (KHÔNG lấy campaignType)
        assertEquals("2099-01-01", o.expireDate)      // expiresAt thắng validity.endDate
        assertEquals("1000", o.estimatedDiscount)
        assertEquals("5000", o.maxDiscount)
        assertEquals("2020-01-01", o.startDate)
        assertEquals(3, o.remainingRedemptions)
        assertEquals(true, o.budgetAvailable)
        assertEquals(listOf("r1"), o.unmatchedRules)
        assertTrue(o.usable)

        assertEquals(42, r.myTotalElements)
        assertEquals(9, r.otherTotalElements)
        assertEquals(false, r.myIsLastPage)
        assertEquals(true, r.otherIsLastPage)
        assertEquals(7, r.expireWarningDate)
        assertEquals("all", r.activeTab)
        // tabs sắp theo `order`, không theo thứ tự server trả.
        assertEquals(listOf("a", "b"), r.tabs.map { it.code })
        assertEquals(true, r.tabs.first().isDefault)
    }

    @Test
    fun minimalPayload_takesFallbackBranchOfEveryElvis() = runTest {
        val json = """
        {"status":200,"data":{
          "myOffers":{"content":[{"campaignId":"c9","campaignName":"ChiCampaign"}]}
        }}
        """.trimIndent()

        val r = useCase(json).invoke(request)!!
        val o = r.myOffers.single()

        assertEquals("c9", o.id)                 // không voucherId → lùi campaignId
        assertEquals("ChiCampaign", o.campaignName)
        assertEquals("CAMPAIGN", o.objectType)   // không voucherId → CAMPAIGN
        assertTrue(o.usable)                     // usable null → mặc định dùng được
        assertNull(o.expireDate)                 // không expiresAt lẫn validity
        assertNull(o.estimatedDiscount)
        assertNull(o.startDate)
        assertNull(o.budgetAvailable)
        assertTrue(o.unmatchedRules.isEmpty())

        // Page vắng mặt → mặc định "hết trang", tổng 0.
        assertEquals(0, r.myTotalElements)
        assertEquals(0, r.otherTotalElements)
        assertEquals(true, r.myIsLastPage)
        assertEquals(true, r.otherIsLastPage)
        assertNull(r.expireWarningDate)
        assertNull(r.activeTab)
        assertTrue(r.tabs.isEmpty())
        assertTrue(r.otherOffers.isEmpty())
    }

    /**
     * `objectType` phải thuộc enum **CAMPAIGN / COUPON / VOUCHER** của BFF và khớp loại `objectId`
     * gửi kèm — `objectId` là `voucherId ?: campaignId` nên suy theo quyền sở hữu.
     *
     * `campaignType` là **phân loại campaign** (DISCOUNT/COUPON/…), KHÔNG phải `objectType`. Trước
     * đây mapper gán thẳng nó vào, nên campaign `campaignType = "DISCOUNT"` khiến
     * `validateStackableDiscounts` trả 400 INVALID_PARAMS
     * (`discountRequests[0].objectType: định dạng không hợp lệ`) — bấm "Áp dụng" là hỏng.
     */
    @Test
    fun objectType_derivesFromOwnership_notCampaignType() = runTest {
        val json = """
        {"status":200,"data":{
          "myOffers":{"content":[
            {"campaignId":"c1","voucherId":"v1","campaignType":"DISCOUNT"}
          ]},
          "otherOffers":{"content":[
            {"campaignId":"c2","campaignType":"DISCOUNT"}
          ]}
        }}
        """.trimIndent()

        val r = useCase(json).invoke(request)!!

        // Có voucherId → objectId là voucher ⇒ VOUCHER. Không có → objectId là campaign ⇒ CAMPAIGN.
        assertEquals("VOUCHER", r.myOffers.single().objectType)
        assertEquals("CAMPAIGN", r.otherOffers.single().objectType)
        // `campaignType` vẫn giữ nguyên ở field riêng, không bị mất.
        assertEquals("DISCOUNT", r.myOffers.single().campaignType)
    }

    @Test
    fun offerWithoutAnyId_isDropped() = runTest {
        // Không có voucherId lẫn campaignId ⇒ không redeem được ⇒ mapper bỏ qua (compactMap).
        val json = """
        {"status":200,"data":{"myOffers":{"content":[
            {"campaignName":"khong-id"},
            {"campaignId":"c1"}
        ]}}}
        """.trimIndent()

        val r = useCase(json).invoke(request)!!
        assertEquals(listOf("c1"), r.myOffers.map { it.id })
    }

    @Test
    fun tabWithoutCode_isDropped_andMissingOrderSinksToEnd() = runTest {
        val json = """
        {"status":200,"data":{"tabs":[
            {"label":"khong-code"},
            {"code":"z","label":"Z"},
            {"code":"a","label":"A","order":1}
        ]}}
        """.trimIndent()

        val r = useCase(json).invoke(request)!!
        // Tab thiếu code bị loại; tab thiếu order xuống cuối (order = Int.MAX_VALUE).
        assertEquals(listOf("a", "z"), r.tabs.map { it.code })
        assertEquals("", r.tabs.first { it.code == "z" }.label.let { if (it == "Z") "" else it })
    }

    @Test
    fun usableFalse_isRespected() = runTest {
        val json = """
        {"status":200,"data":{"myOffers":{"content":[
            {"campaignId":"c1","usable":false,"eligibilityDetails":{"unmatchedRules":["MIN_ORDER"]}}
        ]}}}
        """.trimIndent()

        val o = useCase(json).invoke(request)!!.myOffers.single()
        assertEquals(false, o.usable)
        assertEquals(listOf("MIN_ORDER"), o.unmatchedRules)
    }
}
