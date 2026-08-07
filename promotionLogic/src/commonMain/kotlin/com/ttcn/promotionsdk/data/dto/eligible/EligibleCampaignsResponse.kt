package com.ttcn.promotionsdk.data.dto.eligible

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Payload `data` của API Find Eligible Campaigns (v1.3).
 * [myOffers] null khi chỉ load-more nhóm khác, và ngược lại.
 */
@Serializable
internal data class EligibleCampaignsResponse(
    @SerialName("myOffers") val myOffers: EligibleOfferPage? = null,
    @SerialName("otherOffers") val otherOffers: EligibleOfferPage? = null,
    /** Ngưỡng cảnh báo sắp hết hạn (đơn vị ngày) — cấu hình tĩnh tại BFF. */
    @SerialName("expireWarningDate") val expireWarningDate: Double? = null,
    // v1.6 KHÔNG khôi phục thanh tab — giữ field để tương thích ngược (server không trả → rỗng).
    @SerialName("tabs") val tabs: List<EligibleTabConfig> = emptyList(),
    @SerialName("activeTab") val activeTab: String? = null,
)

@Serializable
internal data class EligibleOfferPage(
    @SerialName("content") val content: List<EligibleOfferDto> = emptyList(),
    @SerialName("totalElements") val totalElements: Long? = null,
    @SerialName("totalPages") val totalPages: Int? = null,
    @SerialName("first") val first: Boolean? = null,
    @SerialName("last") val last: Boolean? = null,
    @SerialName("size") val size: Int? = null,
    @SerialName("number") val number: Int? = null,
    @SerialName("numberOfElements") val numberOfElements: Int? = null,
    @SerialName("empty") val empty: Boolean? = null,
)

@Serializable
internal data class EligibleOfferDto(
    @SerialName("campaignId") val campaignId: String? = null,
    @SerialName("campaignName") val campaignName: String? = null,
    @SerialName("campaignType") val campaignType: String? = null,
    @SerialName("discountType") val discountType: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("discountPreview") val discountPreview: EligibleDiscountPreview? = null,
    @SerialName("budgetStatus") val budgetStatus: EligibleBudgetStatus? = null,
    @SerialName("eligibilityDetails") val eligibilityDetails: EligibleEligibilityDetails? = null,
    @SerialName("validity") val validity: EligibleValidity? = null,
    // Field hiển thị voucher (§6.2) — gating tách ra top-level.
    /** Chỉ nhóm myOffers (voucher khách sở hữu); null ở otherOffers. */
    @SerialName("voucherId") val voucherId: String? = null,
    /** Chỉ nhóm myOffers: trạng thái voucher đang sở hữu. */
    @SerialName("voucherStatus") val voucherStatus: String? = null,
    /** Tên voucher/ưu đãi hiển thị — có ở cả 2 nhóm. */
    @SerialName("voucherName") val voucherName: String? = null,
    /** Chỉ nhóm myOffers: mã code đã phát hành. */
    @SerialName("voucherCode") val voucherCode: String? = null,
    /** Logo voucher — có ở cả 2 nhóm. */
    @SerialName("logoUrl") val logoUrl: String? = null,
    /** Tên đối tác/merchant phát hành — có ở cả 2 nhóm. */
    @SerialName("partnerName") val partnerName: String? = null,
    @SerialName("usable") val usable: Boolean? = null,
    /** null nếu usable=true; "DISABLED" nếu hiển thị mờ. */
    @SerialName("displayMode") val displayMode: String? = null,
    /** Lý do không dùng được — chỉ khi usable=false. */
    @SerialName("disabledReason") val disabledReason: String? = null,
    /** Thời điểm hết hạn (ISO-8601). myOffers ưu tiên hạn voucher đang sở hữu. */
    @SerialName("expiresAt") val expiresAt: String? = null,
)

@Serializable
internal data class EligibleDiscountPreview(
    @SerialName("estimatedDiscount") val estimatedDiscount: String? = null,
    @SerialName("discountPercentage") val discountPercentage: String? = null,
    @SerialName("maxDiscount") val maxDiscount: String? = null,
    @SerialName("minOrderValue") val minOrderValue: String? = null,
)

@Serializable
internal data class EligibleBudgetStatus(
    @SerialName("available") val available: Boolean? = null,
    @SerialName("remainingBudget") val remainingBudget: String? = null,
    @SerialName("utilizationPercentage") val utilizationPercentage: Double? = null,
)

@Serializable
internal data class EligibleEligibilityDetails(
    @SerialName("eligible") val eligible: Boolean? = null,
    @SerialName("eligibilityScore") val eligibilityScore: String? = null,
    @SerialName("matchedRules") val matchedRules: List<String> = emptyList(),
    /** Rule không khớp — UI dùng để gợi ý lý do chưa đủ điều kiện. */
    @SerialName("unmatchedRules") val unmatchedRules: List<String> = emptyList(),
)

@Serializable
internal data class EligibleValidity(
    @SerialName("startDate") val startDate: String? = null,
    @SerialName("endDate") val endDate: String? = null,
    @SerialName("isActive") val isActive: Boolean? = null,
    @SerialName("remainingRedemptions") val remainingRedemptions: Int? = null,
    @SerialName("maxRedemptionsPerCustomer") val maxRedemptionsPerCustomer: Int? = null,
)

@Serializable
internal data class EligibleTabConfig(
    @SerialName("code") val code: String? = null,
    @SerialName("label") val label: String? = null,
    @SerialName("default") val default: Boolean? = null,
    @SerialName("order") val order: Int? = null,
    @SerialName("count") val count: Int? = null,
)
