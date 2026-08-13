package com.ttcn.promotionsdk

import com.ttcn.promotionsdk.presentation.endow.EndowAppliedDiscount
import com.ttcn.promotionsdk.presentation.endow.EndowHostEvent
import com.ttcn.promotionsdk.presentation.endow.EndowHostNotifier
import com.ttcn.promotionsdk.presentation.endow.EndowState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
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

    @Test
    fun count_firstStateAlwaysNotifies_evenWhenZero() {
        // `lastCount` khởi tạo -1 chính là để ca này bắn: host cần biết "đang có 0 ưu đãi".
        assertEquals(
            listOf(EndowHostEvent.VoucherCountChanged(0)),
            EndowHostNotifier().onState(EndowState()),
        )
    }

    @Test
    fun count_notifiesOnlyWhenChanged() {
        val n = EndowHostNotifier()
        n.onState(EndowState(totalVoucherCount = 3))
        assertTrue(n.onState(EndowState(totalVoucherCount = 3)).isEmpty(), "render lại cùng count → im")
        assertEquals(
            listOf(EndowHostEvent.VoucherCountChanged(5)),
            n.onState(EndowState(totalVoucherCount = 5)),
        )
    }

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

    @Test
    fun countEventComesBeforeAppliedEvent() {
        // Thứ tự chốt theo bản Android cũ; iOS trước đây bắn ngược lại.
        val events = EndowHostNotifier().onState(
            EndowState(totalVoucherCount = 2, appliedDiscounts = listOf(applied("v1")))
        )
        assertEquals(
            listOf(EndowHostEvent.VoucherCountChanged(2), EndowHostEvent.VoucherApplied("v1")),
            events,
        )
    }

    // ─── Availability ─────────────────────────────────────────────────────────

    @Test
    fun availability_firstCallAlwaysNotifies_thenOnlyOnChange() {
        val n = EndowHostNotifier()
        // `lastAvailability` khởi tạo null: lần áp cờ đầu bắn kể cả khi cờ đang BẬT.
        assertEquals(listOf(EndowHostEvent.AvailabilityChanged(true)), n.onAvailability(true))
        // `applyFlag` chạy hai lần (cache rồi server) — lần hai cùng giá trị thì phải im.
        assertTrue(n.onAvailability(true).isEmpty())
        assertEquals(listOf(EndowHostEvent.AvailabilityChanged(false)), n.onAvailability(false))
    }

    @Test
    fun availability_isIndependentOfStateStream() {
        val n = EndowHostNotifier()
        n.onAvailability(true)
        assertTrue(
            n.onState(EndowState()).none { it is EndowHostEvent.AvailabilityChanged },
            "state đổi không được sinh lại sự kiện cờ",
        )
    }
}
