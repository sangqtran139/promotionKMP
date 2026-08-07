package com.ttcn.promotionsdk

import com.ttcn.promotionsdk.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.data.dto.featureflag.FeatureFlagItemResponse
import com.ttcn.promotionsdk.data.dto.featureflag.toPromotionFeatureFlags
import com.ttcn.promotionsdk.data.remote.KtorPromotionApiService
import com.ttcn.promotionsdk.data.remote.PromotionHttpClient
import com.ttcn.promotionsdk.data.remote.PromotionRemoteDataSource
import com.ttcn.promotionsdk.data.repository.PromotionRepositoryImpl
import com.ttcn.promotionsdk.domain.model.featureflag.PromotionFeatureFlag
import com.ttcn.promotionsdk.domain.model.voucher.SearchCustomerVouchersRequest
import com.ttcn.promotionsdk.domain.model.voucher.VoucherDisplayState
import com.ttcn.promotionsdk.domain.model.voucher.VoucherStatus
import com.ttcn.promotionsdk.domain.model.voucher.displayState
import com.ttcn.promotionsdk.domain.usecase.GetCustomerVoucherDetailUseCase
import com.ttcn.promotionsdk.domain.usecase.SearchCustomerVouchersUseCase
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Phủ nhánh của `VoucherMapper` (suy `status` từ `metadata.usable`/`disabledReason`) và
 * `FeatureFlagDto`. Đây là hai chỗ quyết định **fail-open hay fail-closed** nên sai là hỏng nghiệp vụ.
 */
class VoucherMappingBranchTest {

    private class NoCtx : PromotionRequestContextProvider
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun repo(json: String) = PromotionRepositoryImpl(
        PromotionRemoteDataSource(
            KtorPromotionApiService(
                HttpClient(MockEngine { respond(json, HttpStatusCode.OK, jsonHeaders) }) {
                    with(PromotionHttpClient) { configure("https://api.example.com", NoCtx(), false) }
                },
            ),
        ),
    )

    private fun detailJson(metadata: String?) = """
        {"status":200,"data":{"voucher":{"id":"c1","name":"Ten"}
        ${if (metadata != null) ""","metadata":$metadata""" else ""}}}
    """.trimIndent()

    // ─── toStatusRaw — 4 nhánh ────────────────────────────────────────────────

    @Test
    fun status_usableTrue_becomesUsable() = runTest {
        val d = GetCustomerVoucherDetailUseCase(repo(detailJson("""{"usable":"true"}"""))).invoke("v", null)
        assertEquals(VoucherStatus.USABLE, VoucherStatus.from(d?.status))
        assertTrue(VoucherStatus.from(d?.status).displayState().isUsable)
    }

    @Test
    fun status_usableFalseWithReason_usesReason() = runTest {
        val d = GetCustomerVoucherDetailUseCase(repo(detailJson("""{"usable":"false","disabledReason":"EXPIRED"}"""))).invoke("v", null)
        assertEquals(VoucherStatus.EXPIRED, VoucherStatus.from(d?.status))
        assertEquals(VoucherDisplayState.EXPIRED, VoucherStatus.from(d?.status).displayState())
        // disabledReason cũng là nguồn nhãn hiển thị cho UI.
        assertEquals("EXPIRED", d?.displayStatusLabel)
    }

    @Test
    fun status_usableFalseWithoutReason_fallsBackToIneligible() = runTest {
        val d = GetCustomerVoucherDetailUseCase(repo(detailJson("""{"usable":"false"}"""))).invoke("v", null)
        assertEquals(VoucherStatus.INELIGIBLE, VoucherStatus.from(d?.status))
        assertFalse(VoucherStatus.from(d?.status).displayState().isUsable)
    }

    @Test
    fun status_unknownUsableValue_becomesNull() = runTest {
        val d = GetCustomerVoucherDetailUseCase(repo(detailJson("""{"usable":"maybe"}"""))).invoke("v", null)
        assertNull(d?.status)
        // Không rõ trạng thái ⇒ fail-closed, KHÔNG cho dùng.
        assertFalse(VoucherStatus.from(d?.status).displayState().isUsable)
    }

    @Test
    fun status_missingMetadata_becomesNull() = runTest {
        val d = GetCustomerVoucherDetailUseCase(repo(detailJson(null))).invoke("v", null)
        assertNull(d?.status)
    }

    @Test
    fun status_unknownDisabledReason_failsClosed() = runTest {
        val d = GetCustomerVoucherDetailUseCase(repo(detailJson("""{"usable":"false","disabledReason":"LY_DO_LA"}"""))).invoke("v", null)
        // Mã lạ → UNKNOWN → không dùng được (fail-closed), nhưng nhãn vẫn giữ nguyên để hiện.
        assertEquals(VoucherStatus.UNKNOWN, VoucherStatus.from(d?.status))
        assertFalse(VoucherStatus.from(d?.status).displayState().isUsable)
        assertEquals("LY_DO_LA", d?.displayStatusLabel)
    }

    // ─── displayStatusLabel — text nút / nhãn trạng thái ──────────────────────

    @Test
    fun displayStatusLabel_prefersServerLabelOverDisabledReason() = runTest {
        val json = """
            {"status":200,"data":{"voucher":{"id":"c1","status":"ACTIVE","displayStatusLabel":"Sử dụng"},
             "metadata":{"usable":"true","disabledReason":null}}}
        """.trimIndent()
        val d = GetCustomerVoucherDetailUseCase(repo(json)).invoke("v", null)
        assertEquals("Sử dụng", d?.displayStatusLabel)
    }

    @Test
    fun displayStatusLabel_fallsBackToDisabledReasonWhenServerLabelMissing() = runTest {
        val d = GetCustomerVoucherDetailUseCase(repo(detailJson("""{"usable":"false","disabledReason":"EXPIRED"}""")))
            .invoke("v", null)
        // Không có nhãn server → giữ mã lý do (native tự quyết hiện chuỗi nào).
        assertEquals("EXPIRED", d?.displayStatusLabel)
    }

    // ─── applicableProducts — nguồn lọc bottom sheet "Chọn dịch vụ" ───────────
    //
    // Field này từng KHÔNG được khai trong DTO: `ignoreUnknownKeys` nuốt im lặng, mapper trả
    // `emptyList()` → sheet luôn rỗng dù host khai đủ `availableServices`. Hai test dưới khoá lại.

    @Test
    fun detail_applicableProducts_parsedFromItemLevel() = runTest {
        val json = """
            {"status":200,"data":{"voucher":{"id":"c1"},
             "applicableProducts":[
               {"productId":"p-11","sku":"SKU-VM-BHOTO-2C","name":"BH 2 chiều","type":"INCLUDED","itemType":"SKU"},
               {"sku":"SKU-KHONG-ID","name":"Thiếu productId","type":"INCLUDED"}
             ]}}
        """.trimIndent()
        val d = GetCustomerVoucherDetailUseCase(repo(json)).invoke("v", null)
        // Phần tử thiếu `productId` bị loại — đó là khoá khớp `availableServices.serviceCode`.
        assertEquals(listOf("p-11"), d?.applicableProducts?.map { it.productId })
        assertEquals("SKU-VM-BHOTO-2C", d?.applicableProducts?.first()?.sku)
    }

    @Test
    fun detail_applicableProducts_fallsBackToVoucherLevel() = runTest {
        val json = """
            {"status":200,"data":{"voucher":{"id":"c1",
              "applicableProducts":[{"productId":"p-13","name":"Combo đồ uống","type":"EXCLUDED"}]}}}
        """.trimIndent()
        val d = GetCustomerVoucherDetailUseCase(repo(json)).invoke("v", null)
        assertEquals(listOf("p-13"), d?.applicableProducts?.map { it.productId })
        // EXCLUDED vẫn giữ nguyên: cắt hay không là việc của tầng lọc, không phải mapper.
        assertEquals("EXCLUDED", d?.applicableProducts?.first()?.type)
    }

    @Test
    fun searchVouchers_applicableProductsParsed() = runTest {
        val json = """
            {"status":200,"data":{"content":[{"voucher":{"id":"v1"},
              "applicableProducts":[{"productId":"p-3","sku":"SKU-VM-GC-V120","name":"V120","type":"INCLUDED"}]}]}}
        """.trimIndent()
        val r = SearchCustomerVouchersUseCase(repo(json)).invoke(SearchCustomerVouchersRequest())
        assertEquals(listOf("p-3"), r?.content?.first()?.applicableProducts?.map { it.productId })
    }

    // ─── Danh sách voucher: tab default true/false ────────────────────────────

    @Test
    fun searchVouchers_tabDefaultFlagBothBranches() = runTest {
        val json = """
        {"status":200,"data":{"tabs":[
            {"code":"a","label":"A","default":true},
            {"code":"b","label":"B","default":false},
            {"code":"c","label":"C"}
        ],"content":[]}}
        """.trimIndent()
        val r = SearchCustomerVouchersUseCase(repo(json)).invoke(
            com.ttcn.promotionsdk.domain.model.voucher.SearchCustomerVouchersRequest(),
        )
        val by = r?.tabs?.associateBy { it.code }!!
        assertTrue(by["a"]!!.isDefault)
        assertFalse(by["b"]!!.isDefault)
        assertFalse(by["c"]!!.isDefault)   // vắng field → false
    }

    // ─── FeatureFlag DTO — có / thiếu từng cờ ────────────────────────────────

    @Test
    fun featureFlags_presentFlagsAreRead() {
        val flags = listOf(
            FeatureFlagItemResponse(PromotionFeatureFlag.ENABLE_ALL, true),
            FeatureFlagItemResponse(PromotionFeatureFlag.VOUCHER_LIST, true),
            FeatureFlagItemResponse(PromotionFeatureFlag.VOUCHER_DETAIL, false),
        ).toPromotionFeatureFlags()

        assertTrue(flags.enableAll)
        assertTrue(flags.voucherList)
        assertFalse(flags.voucherDetail)
        // Cờ server không trả ⇒ false (fail-closed ở tầng DTO; gate mới là nơi quyết fail-open).
        assertFalse(flags.voucherApply)
        assertFalse(flags.voucherRedeem)
        assertFalse(flags.voucherSelection)
    }

    @Test
    fun featureFlags_emptyListGivesAllFalse() {
        val flags = emptyList<FeatureFlagItemResponse>().toPromotionFeatureFlags()
        assertFalse(flags.enableAll)
        assertFalse(flags.voucherList)
        assertFalse(flags.voucherDetail)
        assertFalse(flags.voucherApply)
        assertFalse(flags.voucherRedeem)
        assertFalse(flags.voucherSelection)
    }

    @Test
    fun featureFlags_allTrue() {
        val flags = listOf(
            PromotionFeatureFlag.ENABLE_ALL, PromotionFeatureFlag.VOUCHER_APPLY,
            PromotionFeatureFlag.VOUCHER_REDEEM, PromotionFeatureFlag.VOUCHER_SELECTION,
            PromotionFeatureFlag.VOUCHER_DETAIL, PromotionFeatureFlag.VOUCHER_LIST,
        ).map { FeatureFlagItemResponse(it, true) }.toPromotionFeatureFlags()

        assertTrue(flags.enableAll && flags.voucherApply && flags.voucherRedeem)
        assertTrue(flags.voucherSelection && flags.voucherDetail && flags.voucherList)
    }
}
