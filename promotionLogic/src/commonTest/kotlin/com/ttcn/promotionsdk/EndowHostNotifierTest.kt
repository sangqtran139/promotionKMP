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
    fun applied_firesAgainWhenVoucherIdChangesWithoutLeavingApplied() {
        // "Chọn lại" khi widget đang APPLIED (host tự gọi openChoosePromotion từ nút riêng, không qua
        // "Hủy" trên widget): appliedDiscounts đổi thẳng sang voucher khác, widgetState không rời
        // APPLIED ở bất kỳ thời điểm nào. Theo dõi bằng transition trạng thái (bản cũ) sẽ bỏ lọt case
        // này — phải so theo id.
        val n = EndowHostNotifier()
        val v1 = EndowState(totalVoucherCount = 2, appliedDiscounts = listOf(applied("v1")))
        val v2 = EndowState(totalVoucherCount = 2, appliedDiscounts = listOf(applied("v2")))

        assertEquals(listOf(EndowHostEvent.VoucherApplied("v1")), n.onState(v1))
        assertEquals(listOf(EndowHostEvent.VoucherApplied("v2")), n.onState(v2), "đổi voucher dù vẫn APPLIED → phải bắn lại")
        assertTrue(n.onState(v2).isEmpty(), "vẫn v2, render lại → không bắn nữa")
    }

    @Test
    fun unavailable_firesTooSoHostAlwaysGetsTheVoucherId() {
        // Host cần biết id voucher đã áp dù server trả valid = false (hết ngân sách/hết hạn giữa
        // chừng) — không phải chỉ khi APPLIED. widgetState = UNAVAILABLE vẫn phải bắn.
        val n = EndowHostNotifier()
        n.onState(EndowState(totalVoucherCount = 2))                       // NOT_APPLIED
        val unavailable = EndowState(
            totalVoucherCount = 2,
            appliedDiscounts = listOf(applied("v1")),
            discountUnavailable = true,
        )
        assertEquals(listOf(EndowHostEvent.VoucherApplied("v1")), n.onState(unavailable))
        assertTrue(n.onState(unavailable).isEmpty(), "vẫn UNAVAILABLE cùng id, render lại → không bắn nữa")
    }

    @Test
    fun unavailable_doesNotRefireWhenSameIdJustTurnsInvalid() {
        // v1 đã báo lúc còn valid (APPLIED); server revalidate ra invalid nhưng vẫn cùng id v1 → id
        // không đổi nên không bắn lại, đúng rule "so theo id".
        val n = EndowHostNotifier()
        val appliedState = EndowState(totalVoucherCount = 2, appliedDiscounts = listOf(applied("v1")))
        val nowInvalid = appliedState.copy(discountUnavailable = true)

        assertEquals(listOf(EndowHostEvent.VoucherApplied("v1")), n.onState(appliedState))
        assertTrue(n.onState(nowInvalid).isEmpty())
    }

        // ─── Availability ─────────────────────────────────────────────────────────

        }
