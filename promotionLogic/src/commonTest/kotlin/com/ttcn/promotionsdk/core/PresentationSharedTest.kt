package com.ttcn.promotionsdk.core

import com.ttcn.promotionsdk.core.config.AvailableService
import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
import com.ttcn.promotionsdk.core.config.SdkEnvironment
import com.ttcn.promotionsdk.core.domain.exception.NetworkException
import com.ttcn.promotionsdk.core.domain.exception.PromotionErrorCodes
import com.ttcn.promotionsdk.core.domain.exception.PromotionException
import com.ttcn.promotionsdk.core.domain.exception.toErrorCode
import com.ttcn.promotionsdk.core.domain.model.voucher.ApplicableProduct
import com.ttcn.promotionsdk.presentation.PromotionCancellable
import com.ttcn.promotionsdk.presentation.serviceselector.servicesForApplicableProducts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Các mảnh dùng chung nhỏ nhưng nằm trên đường đi của cả hai nền tảng:
 * lọc dịch vụ cho bottom sheet, handle huỷ subscription, chuẩn hoá mã lỗi, default của config.
 */
class PresentationSharedTest {

    // ─── servicesForApplicableProducts (bottom sheet "Chọn dịch vụ") ───────────

    private fun product(id: String) = ApplicableProduct(productId = id, name = id, type = "INCLUDED")
    private fun service(code: String) = AvailableService(code, "ten-$code", "type", "icon")

    @Test
    fun services_returnsIntersectionOnly() {
        val out = servicesForApplicableProducts(
            applicableProducts = listOf(product("A"), product("B")),
            availableServices = listOf(service("A"), service("C")),
        )
        assertEquals(listOf("A"), out.map { it.serviceCode })
    }

    @Test
    fun services_keepsHostOrderNotVoucherOrder() {
        val out = servicesForApplicableProducts(
            applicableProducts = listOf(product("B"), product("A")),
            availableServices = listOf(service("A"), service("B")),
        )
        assertEquals(listOf("A", "B"), out.map { it.serviceCode })
    }

    @Test
    fun services_deduplicatesByServiceCode() {
        val out = servicesForApplicableProducts(
            applicableProducts = listOf(product("A")),
            availableServices = listOf(service("A"), service("A")),
        )
        assertEquals(1, out.size)
    }

    @Test
    fun services_emptyWhenNoOverlap() {
        assertTrue(
            servicesForApplicableProducts(listOf(product("A")), listOf(service("Z"))).isEmpty(),
        )
    }

    @Test
    fun services_emptyInputsGiveEmptyOutput() {
        assertTrue(servicesForApplicableProducts(emptyList(), listOf(service("A"))).isEmpty())
        assertTrue(servicesForApplicableProducts(listOf(product("A")), emptyList()).isEmpty())
    }

    // ─── PromotionCancellable ─────────────────────────────────────────────────

    @Test
    fun cancellable_runsBlockOnCancel() {
        var called = 0
        val c = PromotionCancellable { called++ }
        assertEquals(0, called)
        c.cancel()
        assertEquals(1, called)
    }

    @Test
    fun cancellable_cancelTwiceRunsTwice_noInternalGuard() {
        // Khoá hành vi hiện tại: handle KHÔNG tự chống gọi lại — nơi dùng phải tự lo.
        var called = 0
        val c = PromotionCancellable { called++ }
        c.cancel(); c.cancel()
        assertEquals(2, called)
    }

    // ─── toErrorCode — chuẩn hoá lỗi cho UI tra chuỗi ─────────────────────────

    @Test
    fun toErrorCode_promotionException_usesItsCode() {
        assertEquals("PRM_MOB_021", PromotionException("PRM_MOB_021", "x").toErrorCode())
    }

    @Test
    fun toErrorCode_promotionExceptionWithoutCode_fallsBackToGeneral() {
        assertEquals(PromotionErrorCodes.GENERAL, PromotionException(null, "x").toErrorCode())
    }

    @Test
    fun toErrorCode_networkException_usesItsCode() {
        assertEquals("NETWORK_TIMEOUT", NetworkException("NETWORK_TIMEOUT", "timeout").toErrorCode())
    }

    @Test
    fun toErrorCode_unknownThrowable_usesMessageThenGeneral() {
        assertEquals("boom", IllegalStateException("boom").toErrorCode())
        assertEquals(PromotionErrorCodes.GENERAL, IllegalStateException().toErrorCode())
    }

    // ─── PromotionSDKConfig — default phải ổn định (host dựa vào) ─────────────

    @Test
    fun config_defaults() {
        val c = PromotionSDKConfig(baseUrl = "https://a.example.com")
        assertNull(c.requestContextProvider)
        assertEquals(SdkEnvironment.PROD, c.environment)
        assertTrue(c.availableServices.isEmpty())
        assertFalse(c.isDebug)
    }

    @Test
    fun contextProvider_defaultsAreNullAndEmpty() {
        // Host chỉ override cái mình cần — phần còn lại phải an toàn, không ném.
        val p = object : PromotionRequestContextProvider {}
        assertNull(p.getAccessToken())
        assertNull(p.getLanguage())
        assertNull(p.getOrderId())
        assertNull(p.getOrderValue())
        assertNull(p.getService())
        assertNull(p.getMetaData())
        assertTrue(p.getOrderItems().isEmpty())
    }
}
