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

    // ─── Golden: DANH SÁCH CÓ THỨ TỰ, không phải "có chứa" ────────────────────
    //
    // CHANGELOG từng ghi một lần `PromotionSDKImpl.emit` đổi thứ tự phát callback (applied-trước →
    // count-trước-applied). **Không chữ ký nào đổi**, nên không gì bắt được: host nào dựa vào thứ tự
    // thì vỡ im lặng. Đây là loại thay đổi dễ xảy ra nhất khi ai đó sắp xếp lại một vòng `for` trông
    // như dọn dẹp.
    //
    // Vì vậy các test dưới đây so **`assertEquals` trên cả List**, không dùng `contains`/`any`:
    // `contains` vẫn xanh khi thứ tự đảo hoặc khi có sự kiện thừa chen vào.

    /** Một lượt áp voucher = **đúng một** sự kiện, không nhiều hơn. */
    @Test
    fun golden_appliedTransition_phatDungMotSuKien() {
        val n = EndowHostNotifier()

        assertEquals(
            listOf(EndowHostEvent.VoucherApplied("v1")),
            n.onState(EndowState(hasLoadedInitial = true, appliedDiscounts = listOf(applied("v1")))),
        )
    }

    /** Render lại cùng state → **danh sách rỗng**. Bắn theo mỗi lần render là host nhận hàng chục lần. */
    @Test
    fun golden_renderLaiCungState_khongPhatGiThem() {
        val n = EndowHostNotifier()
        val state = EndowState(hasLoadedInitial = true, appliedDiscounts = listOf(applied("v1")))
        n.onState(state)

        assertEquals(emptyList(), n.onState(state))
        assertEquals(emptyList(), n.onState(state))
    }

    /**
     * Chuỗi transition đầy đủ của một phiên: chưa áp → áp v1 → render lại → huỷ → áp v2.
     *
     * Gộp cả chuỗi vào **một** danh sách phẳng rồi so một lần: đó mới là thứ host thật sự nhận được
     * theo thời gian, và là thứ vỡ khi ai đó đổi thứ tự.
     */
    @Test
    fun golden_chuoiTransitionMotPhien_dungThuTuVaDungSoLan() {
        val n = EndowHostNotifier()
        val empty = EndowState(hasLoadedInitial = true)
        val v1 = EndowState(hasLoadedInitial = true, appliedDiscounts = listOf(applied("v1")))
        val v2 = EndowState(hasLoadedInitial = true, appliedDiscounts = listOf(applied("v2")))

        val phat = listOf(empty, v1, v1, empty, v2, v2).flatMap { n.onState(it) }

        assertEquals(
            listOf(
                EndowHostEvent.VoucherApplied("v1"),
                EndowHostEvent.VoucherApplied("v2"),
            ),
            phat,
        )
    }

    /**
     * Widget APPLIED nhưng `appliedDiscounts` chưa có id (một nhịp render trung gian) → không phát,
     * và **không** đụng bộ nhớ trong: nhịp sau có id thì phải phát **một** lần, không phải hai.
     */
    @Test
    fun golden_nhipTrungGianThieuId_khongLamPhatHaiLan() {
        val n = EndowHostNotifier()
        val khongId = EndowState(hasLoadedInitial = true, totalVoucherCount = 3)
        val coId = EndowState(hasLoadedInitial = true, appliedDiscounts = listOf(applied("v1")))

        val phat = listOf(khongId, coId, coId).flatMap { n.onState(it) }

        assertEquals(listOf(EndowHostEvent.VoucherApplied("v1")), phat)
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
