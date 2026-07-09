package com.ttcn.promotionsdk.core

import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherDisplayState
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherItem
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherStatus
import com.ttcn.promotionsdk.core.domain.model.voucher.displayState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Khoá quy tắc trạng thái voucher. Trước khi gộp, Android dùng `status == ACTIVE` còn iOS dùng
 * `PromotionModel.displayState()` với tập giá trị rộng hơn — hai bên xử lý `AVAILABLE_TO_CLAIM`
 * khác nhau. Test này tồn tại để chúng không lệch lại.
 */
class VoucherStatusTest {

    private fun voucher(status: String?) = VoucherItem(voucherId = "v-1", status = status)

    @Test
    fun usableStatuses_mapToUsable() {
        listOf("ACTIVE", "AVAILABLE", "USABLE", "AVAILABLE_TO_CLAIM").forEach { raw ->
            assertEquals(VoucherDisplayState.USABLE, voucher(raw).displayState(), raw)
            assertTrue(voucher(raw).displayState().isUsable, raw)
        }
    }

    @Test
    fun availableToClaim_isUsable_onBothPlatforms() {
        // Trước đây: iOS cho dùng, Android khoá. Giờ cả hai đều cho dùng.
        assertEquals(VoucherDisplayState.USABLE, voucher("AVAILABLE_TO_CLAIM").displayState())
    }

    @Test
    fun usedStatuses_mapToUsed() {
        listOf("REDEEMED", "USED").forEach { raw ->
            assertEquals(VoucherDisplayState.USED, voucher(raw).displayState(), raw)
        }
    }

    @Test
    fun expired_mapsToExpired() {
        assertEquals(VoucherDisplayState.EXPIRED, voucher("EXPIRED").displayState())
    }

    @Test
    fun heldOrRevokedStatuses_mapToIneligible() {
        listOf("RESERVED", "REVOKED", "SUSPENDED", "INELIGIBLE", "NOT_ELIGIBLE").forEach { raw ->
            assertEquals(VoucherDisplayState.INELIGIBLE, voucher(raw).displayState(), raw)
            assertFalse(voucher(raw).displayState().isUsable, raw)
        }
    }

    @Test
    fun unknownStatus_failsClosed() {
        // Mã lạ hoặc null → KHÔNG cho dùng, tránh áp nhầm ưu đãi vào đơn hàng.
        // Đây là đổi hành vi so với iOS cũ (mã lạ được coi là dùng được).
        assertEquals(VoucherDisplayState.INELIGIBLE, voucher("SOMETHING_NEW").displayState())
        assertEquals(VoucherDisplayState.INELIGIBLE, voucher(null).displayState())
    }

    @Test
    fun from_isCaseInsensitive_andTrimsWhitespace() {
        assertEquals(VoucherStatus.ACTIVE, VoucherStatus.from("active"))
        assertEquals(VoucherStatus.ACTIVE, VoucherStatus.from("  Active "))
        assertEquals(VoucherStatus.UNKNOWN, VoucherStatus.from(null))
    }
}
