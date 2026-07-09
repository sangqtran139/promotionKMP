package com.ttcn.promotionsdk.ui.feature.promotion.endowview

import com.ttcn.promotionsdk.ui.entry.AppliedDiscount
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyVoucherListItem

data class PRMEndowUiState(
    val myVouchers: List<MyVoucherListItem> = emptyList(),
    val otherVouchers: List<MyVoucherListItem> = emptyList(),
    val discountDetails: List<AppliedDiscount> = emptyList(),
    val discountUnavailable: Boolean = false,
    val totalVoucherCount: Int = 0,
    val hasLoadedInitial: Boolean = false,
    val error: String? = null,
)

enum class EndowViewState {
    EMPTY,              // Chưa có voucher nào
    NOT_APPLIED,        // Có voucher nhưng chưa apply
    APPLIED,             // Đã apply voucher
    UNAVAILABLE,         // Voucher có nhưng không đủ điều kiện áp dụng (ví dụ: hết hạn, không hợp lệ với đơn hàng hiện tại, v.v.)
}