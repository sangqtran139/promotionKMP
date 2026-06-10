package com.ttcn.promotionsdk.core.domain.model

data class DiscountValidationResult(
    val overallValid: Boolean,
    val totalDiscountAmount: String,
    val finalAmount: String,
    val items: List<DiscountItemResult>,
) {
    val validItems: List<DiscountItemResult> get() = items.filter { it.valid }
    val invalidItems: List<DiscountItemResult> get() = items.filter { !it.valid }
}

data class DiscountItemResult(
    val objectId: String,
    val objectType: String,
    val valid: Boolean,
    val calculatedDiscount: String,
    val eligibilityStatus: String,
)
