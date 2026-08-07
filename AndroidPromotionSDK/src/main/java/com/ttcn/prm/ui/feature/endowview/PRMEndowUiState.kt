package com.ttcn.prm.ui.feature.endowview

import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.presentation.endow.EndowWidgetState

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
    val widgetState: EndowWidgetState = EndowWidgetState.EMPTY,
)

/**
 * Một ưu đãi đã được validate/áp dụng — kiểu **công khai** của SDK.
 *
 * Host nhận danh sách này từ callback của widget
 * [PRMEndowView] và truyền lại cho
 * [PRMEndowView.setDiscountDetails].
 *
 * Lưu ý: [com.ttcn.prm.entry.PromotionSDKCallback.onVoucherApplied] (thống nhất với iOS) chỉ trả `voucherId`; chi tiết
 * giảm giá đi theo luồng widget ở trên, không qua callback.
 */
data class AppliedDiscount(
    val objectId: String,
    val objectType: String,
    val valid: Boolean,
    val calculatedDiscount: String,
    val eligibilityStatus: String,
)