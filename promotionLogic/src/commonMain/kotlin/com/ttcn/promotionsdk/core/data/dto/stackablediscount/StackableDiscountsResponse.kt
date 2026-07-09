package com.ttcn.promotionsdk.core.data.dto.stackablediscount

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class StackableDiscountsResponse(
    @SerialName("validationResult") val validationResult: StackingValidationResult,
    @SerialName("decisionToken") val decisionToken: String? = null,
    @SerialName("sessionId") val sessionId: String? = null,
    @SerialName("stackingAnalysis") val stackingAnalysis: StackingAnalysis? = null,
    @SerialName("discountDetails") val discountDetails: List<DiscountDetail> = emptyList(),
    @SerialName("optimization") val optimization: StackingOptimization? = null,
    @SerialName("warnings") val warnings: List<String> = emptyList(),
    @SerialName("businessRuleViolations") val businessRuleViolations: List<BusinessRuleViolation> = emptyList(),
)

@Serializable
data class StackingValidationResult(
    @SerialName("overallValid") val overallValid: Boolean,
    @SerialName("canStack") val canStack: Boolean = false,
    @SerialName("totalDiscountAmount") val totalDiscountAmount: String = "",
    @SerialName("finalAmount") val finalAmount: String = "",
    @SerialName("effectiveDiscountRate") val effectiveDiscountRate: Double = 0.0,
    @SerialName("validationSummary") val validationSummary: String = "",
)

@Serializable
data class StackingAnalysis(
    @SerialName("stackableGroups") val stackableGroups: List<StackableGroup> = emptyList(),
    @SerialName("conflicts") val conflicts: List<StackingConflict> = emptyList(),
    @SerialName("exclusions") val exclusions: List<StackingExclusion> = emptyList(),
)

@Serializable
data class StackableGroup(
    @SerialName("groupId") val groupId: String,
    @SerialName("discounts") val discounts: List<String> = emptyList(),
    @SerialName("stackingRule") val stackingRule: String = "",
    @SerialName("groupDiscount") val groupDiscount: String = "",
    @SerialName("description") val description: String = "",
)

@Serializable
data class StackingConflict(
    @SerialName("conflictType") val conflictType: String,
    @SerialName("discounts") val discounts: List<String> = emptyList(),
    @SerialName("reason") val reason: String = "",
)

@Serializable
data class StackingExclusion(
    @SerialName("excludedObjectId") val excludedObjectId: String,
    @SerialName("reason") val reason: String = "",
)

@Serializable
data class DiscountDetail(
    @SerialName("objectId") val objectId: String,
    @SerialName("objectType") val objectType: String,
    @SerialName("valid") val valid: Boolean,
    @SerialName("calculatedDiscount") val calculatedDiscount: String = "",
    @SerialName("eligibilityStatus") val eligibilityStatus: String = "",
    @SerialName("budgetStatus") val budgetStatus: String = "",
    @SerialName("validationMessages") val validationMessages: List<String> = emptyList(),
    /** Payload tự do — xem ghi chú ở `RedeemableRequest.metadata`. */
    @SerialName("metadata") val metadata: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class StackingOptimization(
    @SerialName("recommendedOrder") val recommendedOrder: List<String> = emptyList(),
    /** Server trả mảng object hình dạng chưa cố định; giữ nguyên dạng JSON thay vì `List<Any>`. */
    @SerialName("alternativeStacks") val alternativeStacks: List<JsonElement> = emptyList(),
    @SerialName("maxPossibleDiscount") val maxPossibleDiscount: String = "",
    @SerialName("optimizationNotes") val optimizationNotes: List<String> = emptyList(),
)

@Serializable
data class BusinessRuleViolation(
    @SerialName("ruleCode") val ruleCode: String,
    @SerialName("message") val message: String = "",
)
