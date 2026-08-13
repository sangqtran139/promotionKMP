package com.ttcn.promotionsdk.domain.model.eligible

import com.ttcn.promotionsdk.domain.model.voucher.VoucherTabItem

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
    /** Ngưỡng cảnh báo sắp hết hạn (đơn vị ngày) — cấu hình tĩnh BFF; null nếu không trả. */
    val expireWarningDate: Int? = null,
)

/**
 * Một ưu đãi đủ/không đủ điều kiện cho đơn hàng.
 *
 * [id] ưu tiên `voucherId` (nhóm của tôi → mở được màn chi tiết voucher), fallback `campaignId`
 * (nhóm khác). Dùng [id] cho `validateDiscounts` / `createRedemption`.
 *
 * [usable] = false nghĩa là server trả `displayMode = "DISABLED"`: hiển thị mờ, không cho chọn.
 * Lý do hiển thị lấy từ [minOrderValue] hoặc [unmatchedRules]; SDK trả rule thô, không dựng sẵn câu.
 */
data class EligibleOffer(
    val id: String,
    val campaignId: String? = null,
    val voucherId: String? = null,
    val campaignName: String? = null,
    /**
     * Tên voucher/ưu đãi do server trả (`voucherName`, v1.6) — **có ở cả hai nhóm**, không riêng
     * `myOffers`. Giữ RIÊNG với [campaignName] thay vì hợp nhất từ tầng mapper như trước
     * (`campaignName = voucherName ?: campaignName`): gộp rồi thì không ai phân biệt được đang cầm
     * tên voucher hay tên campaign. Chỗ nào cần "tên để hiển thị" thì dùng [displayName].
     */
    val voucherName: String? = null,
    val campaignType: String? = null,
    val objectType: String = "CAMPAIGN",
    val discountType: String? = null,
    val usable: Boolean = true,
    /** Logo voucher/ưu đãi (có ở cả 2 nhóm). */
    val logoUrl: String? = null,
    /** Tên đối tác/merchant phát hành (có ở cả 2 nhóm). */
    val partnerName: String? = null,
    /** Mã code đã phát hành — chỉ nhóm "của tôi" (myOffers). */
    val voucherCode: String? = null,
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

    /**
     * Tên đem đi hiển thị: ưu tiên [voucherName], thiếu thì [campaignName].
     *
     * **Một nơi duy nhất** quyết định thứ tự ưu tiên đó — trước đây nó nằm ở tầng mapper DTO nên mọi
     * consumer (card màn Chọn, `PromotionEligibleOffer.name` của API headless, seed màn Chi tiết bên
     * iOS) đều vô tình phụ thuộc vào việc `campaignName` đã bị ghi đè. Nay các chỗ đó gọi thẳng
     * hàm này, và [campaignName] giữ đúng nghĩa "tên campaign".
     */
    val displayName: String? get() = voucherName ?: campaignName
}
