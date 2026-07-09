package com.ttcn.promotionsdk.core.data.dto.eligible

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
    /** Chỉ có ở nhóm myOffers (voucher khách sở hữu); null ở otherOffers. */
    @SerialName("voucherId") val voucherId: String? = null,
    @SerialName("campaignName") val campaignName: String? = null,
    @SerialName("campaignType") val campaignType: String? = null,
    @SerialName("discountType") val discountType: String? = null,
    @SerialName("usable") val usable: Boolean? = null,
    /** null nếu usable=true; "DISABLED" nếu hiển thị mờ. */
    @SerialName("displayMode") val displayMode: String? = null,
    @SerialName("discountPreview") val discountPreview: EligibleDiscountPreview? = null,
    @SerialName("budgetStatus") val budgetStatus: EligibleBudgetStatus? = null,
    @SerialName("eligibilityDetails") val eligibilityDetails: EligibleEligibilityDetails? = null,
    @SerialName("validity") val validity: EligibleValidity? = null,
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
