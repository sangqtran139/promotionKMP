package com.ttcn.promotionsdk.ui.entry

/**
 * Một ưu đãi đã được validate/áp dụng — kiểu **công khai** của SDK.
 *
 * Host nhận danh sách này từ callback của widget
 * [com.ttcn.promotionsdk.ui.feature.promotion.endowview.PRMEndowView] và truyền lại cho
 * [com.ttcn.promotionsdk.ui.feature.promotion.endowview.PRMEndowView.setDiscountDetails].
 * Thay cho DTO data-layer trước đây để không rò chi tiết tầng data ra public API.
 *
 * Lưu ý: [PromotionSDKCallback.onVoucherApplied] (thống nhất với iOS) chỉ trả `voucherId`; chi tiết
 * giảm giá đi theo luồng widget ở trên, không qua callback.
 */
data class AppliedDiscount(
    val objectId: String,
    val objectType: String,
    val valid: Boolean,
    val calculatedDiscount: String,
    val eligibilityStatus: String,
)
