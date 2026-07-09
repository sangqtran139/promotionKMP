package com.ttcn.promotionsdk.core.domain.model.eligible

import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherTabItem

data class EligibleOffersResult(
    val myOffers: List<EligibleOffer> = emptyList(),
    val otherOffers: List<EligibleOffer> = emptyList(),
    /** Server đã sắp xếp theo tab đang chọn; giữ nguyên thứ tự, chỉ tabs được sort theo `order`. */
    val tabs: List<VoucherTabItem> = emptyList(),
    val activeTab: String? = null,
    val myIsLastPage: Boolean = true,
    val otherIsLastPage: Boolean = true,
    val myTotalElements: Long = 0,
    val otherTotalElements: Long = 0,
)

/**
 * Một ưu đãi đủ/không đủ điều kiện cho đơn hàng.
 *
 * [id] ưu tiên `voucherId` (nhóm của tôi → mở được màn chi tiết voucher), fallback `campaignId`
 * (nhóm khác). Dùng [id] cho `validateDiscounts` / `createRedemption`.
 *
 * [usable] = false nghĩa là server trả `displayMode = "DISABLED"`: hiển thị mờ, không cho chọn.
 * Lý do hiển thị lấy từ [minOrderValue] hoặc [unmatchedRules] — SDK không dựng sẵn câu tiếng Việt
 * vì đó là copy của tầng UI.
 */
data class EligibleOffer(
    val id: String,
    val campaignId: String? = null,
    val voucherId: String? = null,
    val campaignName: String? = null,
    val campaignType: String? = null,
    val objectType: String = "CAMPAIGN",
    val discountType: String? = null,
    val usable: Boolean = true,
    val estimatedDiscount: String? = null,
    val discountPercentage: String? = null,
    val maxDiscount: String? = null,
    val minOrderValue: String? = null,
    val startDate: String? = null,
    val expireDate: String? = null,
    val remainingRedemptions: Int? = null,
    val budgetAvailable: Boolean? = null,
    val unmatchedRules: List<String> = emptyList(),
) {
    /** Ưu đãi thuộc nhóm "của tôi" khi server trả kèm `voucherId`. */
    val isOwnedVoucher: Boolean get() = voucherId != null
}
