package com.ttcn.promotionsdk.data.dto.redemption

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class RedemptionSessionResponse(
    @SerialName("sessionId") val sessionId: String,
    @SerialName("createdAt") val createdAt: String = "",
    @SerialName("expiresAt") val expiresAt: String = "",
    @SerialName("preview") val preview: SessionPreview? = null,
    @SerialName("budgetHolds") val budgetHolds: List<BudgetHold> = emptyList(),
    @SerialName("validationErrors") val validationErrors: List<RedemptionValidationErrorResponse> = emptyList(),
)

@Serializable
public data class SessionPreview(
    @SerialName("orderId") val orderId: String,
    @SerialName("originalAmount") val originalAmount: String,
    @SerialName("totalDiscount") val totalDiscount: String,
    @SerialName("finalAmount") val finalAmount: String,
    @SerialName("effectiveDiscountRate") val effectiveDiscountRate: Double = 0.0,
    @SerialName("stackingMode") val stackingMode: String = "",
    @SerialName("appliedDiscounts") val appliedDiscounts: List<AppliedDiscount> = emptyList(),
)

@Serializable
public data class AppliedDiscount(
    @SerialName("redeemableId") val redeemableId: String,
    @SerialName("redeemableType") val redeemableType: String,
    @SerialName("redeemableName") val redeemableName: String,
    @SerialName("discountType") val discountType: String,
    @SerialName("discountAmount") val discountAmount: String,
    @SerialName("discountPercentage") val discountPercentage: String,
    @SerialName("priority") val priority: Int,
    @SerialName("appliedTo") val appliedTo: String,
)

@Serializable
public data class BudgetHold(
    @SerialName("redeemableId") val redeemableId: String,
    @SerialName("holdId") val holdId: String,
    @SerialName("heldAmount") val heldAmount: String,
    @SerialName("campaignId") val campaignId: String,
    @SerialName("expiresAt") val expiresAt: String,
)

@Serializable
public data class RedemptionValidationErrorResponse(
    @SerialName("field") val field: String = "",
    @SerialName("code") val code: String,
    @SerialName("message") val message: String = "",
)
