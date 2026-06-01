package com.ttcn.promotionsdk.core.data.dto.redemption

import com.google.gson.annotations.SerializedName

data class RedemptionSessionResponse(
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("expiresAt") val expiresAt: String,
    @SerializedName("preview") val preview: SessionPreview?,
    @SerializedName("budgetHolds") val budgetHolds: List<BudgetHold> = emptyList(),
    @SerializedName("validationErrors") val validationErrors: List<ValidationError> = emptyList(),
)

data class SessionPreview(
    @SerializedName("orderId") val orderId: String,
    @SerializedName("originalAmount") val originalAmount: String,
    @SerializedName("totalDiscount") val totalDiscount: String,
    @SerializedName("finalAmount") val finalAmount: String,
    @SerializedName("effectiveDiscountRate") val effectiveDiscountRate: Double,
    @SerializedName("stackingMode") val stackingMode: String,
    @SerializedName("appliedDiscounts") val appliedDiscounts: List<AppliedDiscount> = emptyList(),
)

data class AppliedDiscount(
    @SerializedName("redeemableId") val redeemableId: String,
    @SerializedName("redeemableType") val redeemableType: String,
    @SerializedName("redeemableName") val redeemableName: String,
    @SerializedName("discountType") val discountType: String,
    @SerializedName("discountAmount") val discountAmount: String,
    @SerializedName("discountPercentage") val discountPercentage: String,
    @SerializedName("priority") val priority: Int,
    @SerializedName("appliedTo") val appliedTo: String,
)

data class BudgetHold(
    @SerializedName("redeemableId") val redeemableId: String,
    @SerializedName("holdId") val holdId: String,
    @SerializedName("heldAmount") val heldAmount: String,
    @SerializedName("campaignId") val campaignId: String,
    @SerializedName("expiresAt") val expiresAt: String,
)

data class ValidationError(
    @SerializedName("field") val field: String,
    @SerializedName("code") val code: String,
    @SerializedName("message") val message: String,
)