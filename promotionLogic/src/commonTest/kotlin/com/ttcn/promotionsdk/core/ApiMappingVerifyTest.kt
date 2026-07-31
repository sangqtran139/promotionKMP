package com.ttcn.promotionsdk.core

import com.ttcn.promotionsdk.core.data.remote.KtorPromotionApiService
import com.ttcn.promotionsdk.core.data.remote.PromotionHttpClient
import com.ttcn.promotionsdk.core.data.remote.PromotionRemoteDataSource
import com.ttcn.promotionsdk.core.data.repository.PromotionRepositoryImpl
import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.domain.model.PromotionResult
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffersResult
import com.ttcn.promotionsdk.core.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersResult
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherDetail
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherDisplayState
import com.ttcn.promotionsdk.core.domain.model.voucher.displayState
import com.ttcn.promotionsdk.core.domain.usecase.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private class NoCtx : PromotionRequestContextProvider
private val jh = headersOf(HttpHeaders.ContentType, "application/json")
private fun uc(json: String): PromotionUseCases {
    val client = HttpClient(MockEngine { respond(json, HttpStatusCode.OK, jh) }) {
        with(PromotionHttpClient) { configure("https://api.example.com", NoCtx(), false) }
    }
    val repo = PromotionRepositoryImpl(PromotionRemoteDataSource(KtorPromotionApiService(client)))
    return PromotionUseCases(
        SearchCustomerVouchersUseCase(repo), GetCustomerVoucherDetailUseCase(repo),
        ValidateStackableDiscountsUseCase(repo), CreateRedemptionSessionUseCase(repo),
        FindEligibleCampaignsUseCase(repo),
    )
}

/** Chạy JSON Success Example CHÍNH THỨC (copy từ PDF) qua DTO+mapper thật → in domain data. */
class ApiMappingVerifyTest {

    @Test
    fun voucherDetail_officialExample_mapsAllChangedFields() = runTest {
        // Nguyên văn §6.5 Success Example của API 3.5.9 Get Customer Voucher Detail (v1.3).
        val json = """
        {"status":200,"code":"SUCCESS","success":true,"message":"OK",
         "data":{
           "voucher":{"id":"0192abcd-0001-7000-8000-0000000c0001",
             "brand":{"name":"Vietcombank","logo":["https://cdn.pp/vcb-logo.png"]},
             "image":"https://cdn.pp/vcb-banner.png",
             "title":"Hoàn tiền 20% gửi tiết kiệm Vietcombank",
             "remainingQty":100,"usedQty":0,"discountType":2,"discountValue":20.0,
             "maxDiscount":150000,"minOrder":300000,
             "content":"Hoàn 20% giá trị sổ tiết kiệm, tối đa 150.000đ",
             "description":"Chương trình hoàn tiền gửi tiết kiệm Vietcombank Q3/2026",
             "tags":["tiết kiệm"],"endDate":"31-07-2026","campaignEndDate":"31-07-2026",
             "expiredTime":"17","unlimitedQty":false},
           "codes":[{"phone":"84901234567","codex":"SAV84901234567","expiredAt":"31/07/2026"}],
           "quantity":100,"value":100000.0,"amount":150000,
           "startDate":"2026-07-01T00:00:00","endDate":"2026-07-31T23:59:59",
           "expiredTimeNumber":17,"priority":95,"isYourself":1,
           "metadata":{"usable":"true","displayMode":null,"disabledReason":null}}}
        """.trimIndent()

        val d = assertIs<PromotionResult.Success<VoucherDetail>>(
            uc(json).getVoucherDetail(voucherId = "x")
        ).data


        assertEquals("0192abcd-0001-7000-8000-0000000c0001", d.voucherId)
        assertEquals("Vietcombank", d.merchantName)
        assertEquals("https://cdn.pp/vcb-logo.png", d.logo)
        assertEquals("https://cdn.pp/vcb-banner.png", d.banner)
        assertEquals("Hoàn 20% giá trị sổ tiết kiệm, tối đa 150.000đ", d.description)
        assertEquals(null, d.guideline)   // JSON không có `guideline` (field BE thêm sau v1.3)
        assertEquals("2026-07-31T23:59:59", d.expirationDate)
        assertEquals(VoucherDisplayState.USABLE, d.displayState())
    }

    @Test
    fun search_flatItem_mapsVoucherAndMetadata() = runTest {
        val json = """
        {"status":200,"success":true,"data":{
          "content":[{
            "voucher":{"id":"v-9","brand":{"name":"Grab","logo":["https://cdn/grab.png"]},
              "image":"https://cdn/grab-banner.png","title":"Giảm 30k Grab"},
            "metadata":{"usable":"false","disabledReason":"EXPIRED"},
            "startDate":"2026-07-01T00:00:00","endDate":"2026-07-31T23:59:59"}],
          "expireWarningDate":7,"last":true,"totalElements":1,"number":0,"size":10}}
        """.trimIndent()

        val r = assertIs<PromotionResult.Success<SearchCustomerVouchersResult>>(
            uc(json).searchVouchers(com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersRequest("c"))
        ).data
        val it = r.content.single()
        assertEquals("v-9", it.voucherId)
        assertEquals("Grab", it.merchantName)
        assertEquals(VoucherDisplayState.EXPIRED, it.displayState())
    }

    @Test
    fun findEligible_v16_mapsDisplayFields() = runTest {
        val json = """
        {"success":true,"data":{
          "myOffers":{"content":[{
            "campaignId":"camp-1","voucherId":"v-1","voucherName":"Ưu đãi Grab 50k",
            "campaignName":"Camp Grab","logoUrl":"https://cdn/g.png","partnerName":"Grab",
            "usable":true,"discountPreview":{"estimatedDiscount":"50000","minOrderValue":"200000"},
            "validity":{"endDate":"2026-12-31"},"expiresAt":"2026-08-15T23:59:59"}],
            "last":false,"totalElements":3},
          "otherOffers":{"content":[],"last":true,"totalElements":0},
          "expireWarningDate":7}}
        """.trimIndent()

        val r = assertIs<PromotionResult.Success<EligibleOffersResult>>(
            uc(json).findEligible(FindEligibleCampaignsRequest("o", "500000"))
        ).data
        val o = r.myOffers.single()
        assertEquals("v-1", o.id)
        assertEquals("Ưu đãi Grab 50k", o.campaignName)
        assertEquals("2026-08-15T23:59:59", o.expireDate)
    }
}
