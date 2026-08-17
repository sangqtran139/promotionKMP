package com.ttcn.promotionsdk

import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.presentation.choosepromotion.ChooseOffer
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionState
import com.ttcn.promotionsdk.presentation.choosepromotion.canApply
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Nút "Áp dụng" trong lúc/sau khi kéo-để-tải-lại.
 *
 * Rule dùng chung nên chỉ cần test ở đây — Android (`binding.btnApply.isEnabled`) và iOS
 * (`Display.canApply`) đều đọc đúng hàm này.
 */
class CanApplyDuringRefreshTest {

    private fun offer(id: String, usable: Boolean = true) = ChooseOffer(
        source = EligibleOffer(id = id, voucherId = id, objectType = "VOUCHER", usable = usable),
        isUsable = usable,
        expiringInDays = null,
        isExpired = false,
    )

    private fun state(
        offers: List<ChooseOffer>,
        selected: List<String>,
        refreshing: Boolean = false,
    ) = ChoosePromotionState(
        myOffers = offers,
        selectedIds = selected,
        isRefreshing = refreshing,
    )

    @Test
    fun selectedAndIdle_enabled() {
        assertTrue(state(listOf(offer("v1")), listOf("v1")).canApply())
    }

    @Test
    fun nothingSelected_disabled() {
        assertFalse(state(listOf(offer("v1")), emptyList()).canApply())
    }

    // ─── 1. Đang refresh → tắt tạm ────────────────────────────────────────────

    @Test
    fun refreshingWithSelection_disabledTemporarily() {
        val s = state(listOf(offer("v1")), listOf("v1"), refreshing = true)
        assertFalse(s.canApply(), "đang nạp lại thì chưa biết voucher còn hợp lệ không")
        // Nạp xong, voucher vẫn dùng được → bật lại.
        assertTrue(s.copy(isRefreshing = false).canApply())
    }

    // ─── 2. Nạp lại xong, voucher thành không dùng được → tắt ─────────────────

    @Test
    fun selectedBecameUnusable_disabled() {
        val s = state(listOf(offer("v1", usable = false)), listOf("v1"))
        assertFalse(s.canApply(), "server trả về usable=false thì không cho áp")
    }

    @Test
    fun selectedDisappearedAfterRefresh_disabled() {
        // Voucher đang tick biến mất hẳn khỏi danh sách mới.
        val s = state(listOf(offer("v2")), listOf("v1"))
        assertFalse(s.canApply(), "id đang tick không còn trong danh sách thì không cho áp")
    }

    @Test
    fun oneOfManyUnusable_disabled() {
        val s = state(listOf(offer("v1"), offer("v2", usable = false)), listOf("v1", "v2"))
        assertFalse(s.canApply(), "một cái hỏng là chặn cả lượt")
    }
}
