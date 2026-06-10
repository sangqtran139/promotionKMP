package com.ttcn.promotionsdk.core.domain.model

data class RedemptionSessionResult(
    val sessionId: String,
    val totalDiscount: String,
    val finalAmount: String,
    val validationErrors: List<RedemptionValidationError>,
) {
    val hasErrors: Boolean get() = validationErrors.isNotEmpty()
    val hasBudgetError: Boolean get() = validationErrors.any { it.code == "INSUFFICIENT_BUDGET" }
}

data class RedemptionValidationError(
    val code: String,
    val message: String,
)
