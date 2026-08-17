package com.ttcn.promotionsdk

import com.ttcn.promotionsdk.presentation.endow.EndowAppliedDiscount
import com.ttcn.promotionsdk.presentation.endow.EndowHostEvent
import com.ttcn.promotionsdk.presentation.endow.EndowHostNotifier
import com.ttcn.promotionsdk.presentation.endow.EndowState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Sau khi bỏ `onVoucherCountChanged` / `onAvailabilityChanged` khỏi `PromotionSDKCallback`, notifier
 * chỉ còn đúng một sự kiện: `VoucherApplied`. Các test đếm voucher và bật/tắt cờ đã xoá theo.
 *
 * Rule "khi nào bắn callback host" — trước đây mỗi nền tảng một bản chép tay và **không bên nào có
 * test**: Android ba biến trong `PRMEndowView`, iOS hai biến trong `PromotionSDKImpl.render`.
 */
class EndowHostNotifierTest {

    private fun applied(id: String) =
        EndowAppliedDiscount(
            objectId = id,
            objectType = "VOUCHER",
            valid = true,
            calculatedDiscount = "1000",
            eligibilityStatus = "",
        )

    // ─── Count ────────────────────────────────────────────────────────────────

            // ─── Applied ──────────────────────────────────────────────────────────────

    @Test
    fun applied_firesOnceOnTransitionIntoApplied() {
        val n = EndowHostNotifier()
        n.onState(EndowState(totalVoucherCount = 2))                       // NOT_APPLIED
        val st = EndowState(totalVoucherCount = 2, appliedDiscounts = listOf(applied("v1")))

        assertEquals(listOf(EndowHostEvent.VoucherApplied("v1")), n.onState(st))
        assertTrue(n.onState(st).isEmpty(), "vẫn APPLIED, render lại → không bắn nữa")
    }

    @Test
    fun applied_firesAgainAfterLeavingAppliedState() {
        val n = EndowHostNotifier()
        val st = EndowState(totalVoucherCount = 2, appliedDiscounts = listOf(applied("v1")))
        n.onState(st)
        n.onState(EndowState(totalVoucherCount = 2))                       // huỷ áp → NOT_APPLIED
        assertEquals(listOf(EndowHostEvent.VoucherApplied("v1")), n.onState(st), "áp lại → bắn lại")
    }

    @Test
    fun applied_unavailableIsNotApplied() {
        val n = EndowHostNotifier()
        n.onState(EndowState(totalVoucherCount = 2))
        val unavailable = EndowState(
            totalVoucherCount = 2,
            appliedDiscounts = listOf(applied("v1")),
            discountUnavailable = true,
        )
        // widgetState = UNAVAILABLE, dù appliedDiscounts không rỗng → KHÔNG phải "vừa áp xong".
        assertTrue(n.onState(unavailable).none { it is EndowHostEvent.VoucherApplied })
    }

        // ─── Availability ─────────────────────────────────────────────────────────

        }
