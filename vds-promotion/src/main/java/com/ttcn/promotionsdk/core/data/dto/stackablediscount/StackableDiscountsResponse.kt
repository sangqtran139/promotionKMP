package com.ttcn.promotionsdk.core.data.dto.stackablediscount

import com.google.gson.annotations.SerializedName

data class StackableDiscountsResponse(
    @SerializedName("validationResult") val validationResult: ValidationResult,
    @SerializedName("decisionToken") val decisionToken: String? = null,
    @SerializedName("sessionId") val sessionId: String? = null,
    @SerializedName("stackingAnalysis") val stackingAnalysis: StackingAnalysis? = null,
    @SerializedName("discountDetails") val discountDetails: List<DiscountDetail> = emptyList(),
    @SerializedName("optimization") val optimization: Optimization? = null,
    @SerializedName("warnings") val warnings: List<String> = emptyList(),
    @SerializedName("businessRuleViolations") val businessRuleViolations: List<BusinessRuleViolation> = emptyList(),
)

data class ValidationResult(
    @SerializedName("overallValid") val overallValid: Boolean,
    @SerializedName("canStack") val canStack: Boolean,
    @SerializedName("totalDiscountAmount") val totalDiscountAmount: String = "",
    @SerializedName("finalAmount") val finalAmount: String = "",
    @SerializedName("effectiveDiscountRate") val effectiveDiscountRate: Double = 0.0,
    @SerializedName("validationSummary") val validationSummary: String = "",
)

data class StackingAnalysis(
    @SerializedName("stackableGroups") val stackableGroups: List<StackableGroup> = emptyList(),
    @SerializedName("conflicts") val conflicts: List<StackingConflict> = emptyList(),
    @SerializedName("exclusions") val exclusions: List<StackingExclusion> = emptyList(),
)

data class StackableGroup(
    @SerializedName("groupId") val groupId: String,
    @SerializedName("discounts") val discounts: List<String> = emptyList(),
    @SerializedName("stackingRule") val stackingRule: String = "",
    @SerializedName("groupDiscount") val groupDiscount: String = "",
    @SerializedName("description") val description: String = "",
)

data class StackingConflict(
    @SerializedName("conflictType") val conflictType: String,
    @SerializedName("discounts") val discounts: List<String> = emptyList(),
    @SerializedName("reason") val reason: String = "",
)

data class StackingExclusion(
    @SerializedName("excludedObjectId") val excludedObjectId: String,
    @SerializedName("reason") val reason: String = "",
)

data class DiscountDetail(
    @SerializedName("objectId") val objectId: String,
    @SerializedName("objectType") val objectType: String,
    @SerializedName("valid") val valid: Boolean,
    @SerializedName("calculatedDiscount") val calculatedDiscount: String = "",
    @SerializedName("eligibilityStatus") val eligibilityStatus: String = "",
    @SerializedName("budgetStatus") val budgetStatus: String = "",
    @SerializedName("validationMessages") val validationMessages: List<String> = emptyList(),
    @SerializedName("metadata") val metadata: Map<String, Any> = emptyMap(),
)

data class Optimization(
    @SerializedName("recommendedOrder") val recommendedOrder: List<String> = emptyList(),
    @SerializedName("alternativeStacks") val alternativeStacks: List<Any> = emptyList(),
    @SerializedName("maxPossibleDiscount") val maxPossibleDiscount: String = "",
    @SerializedName("optimizationNotes") val optimizationNotes: List<String> = emptyList(),
)

data class BusinessRuleViolation(
    @SerializedName("ruleCode") val ruleCode: String,
    @SerializedName("message") val message: String = "",
)