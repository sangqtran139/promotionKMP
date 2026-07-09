package com.ttcn.promotionsdk.ui.entry

/**
 * Một ưu đãi đã được validate/áp dụng — kiểu **công khai** của SDK.
 *
 * Host nhận danh sách này ở [PromotionSDKCallback.onVoucherApplied] và truyền lại cho
 * [com.ttcn.promotionsdk.ui.feature.promotion.endowview.PRMEndowView.setDiscountDetails].
 * Thay cho DTO data-layer trước đây để không rò chi tiết tầng data ra public API.
 */
data class AppliedDiscount(
    val objectId: String,
    val objectType: String,
    val valid: Boolean,
    val calculatedDiscount: String,
    val eligibilityStatus: String,
)
