package com.ttcn.prm.ui.feature.promotion.endowview

import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffer
import com.ttcn.prm.entry.AppliedDiscount

internal data class PRMEndowUiState(
    /** Ưu đãi từ `findEligible`, truyền thẳng sang màn "Chọn ưu đãi" để khỏi gọi API hai lần. */
    val myVouchers: List<EligibleOffer> = emptyList(),
    val otherVouchers: List<EligibleOffer> = emptyList(),
    /** Cờ phân trang của lần `findEligible` đã nạp — màn "Chọn ưu đãi" nhận lại qua `forEndowView`. */
    val myIsLastPage: Boolean = true,
    val otherIsLastPage: Boolean = true,
    val discountDetails: List<AppliedDiscount> = emptyList(),
    val discountUnavailable: Boolean = false,
    val totalVoucherCount: Int = 0,
    val hasLoadedInitial: Boolean = false,
    val error: String? = null,
    /** Trạng thái widget do store (`EndowStore.widgetState`) quyết định — View chỉ render. */
    val widgetState: EndowViewState = EndowViewState.EMPTY,
)

enum class EndowViewState {
    EMPTY,              // Chưa có voucher nào
    NOT_APPLIED,        // Có voucher nhưng chưa apply
    APPLIED,             // Đã apply voucher
    UNAVAILABLE,         // Voucher có nhưng không đủ điều kiện áp dụng (ví dụ: hết hạn, không hợp lệ với đơn hàng hiện tại, v.v.)
}