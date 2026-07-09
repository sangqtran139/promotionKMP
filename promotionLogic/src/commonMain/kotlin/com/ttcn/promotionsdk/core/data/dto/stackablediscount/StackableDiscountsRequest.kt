package com.ttcn.promotionsdk.core.data.dto.stackablediscount

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StackableDiscountsRequest(
    @SerialName("idempotencyKey") val idempotencyKey: String = "",
    @SerialName("customerInfo") val customerInfo: StackableCustomerInfo,
    @SerialName("orderInfo") val orderInfo: StackableOrderInfo,
    @SerialName("discountRequests") val discountRequests: List<DiscountRequest> = emptyList(),
    @SerialName("validationOptions") val validationOptions: ValidationOptions = ValidationOptions(),
)

@Serializable
data class StackableCustomerInfo(
    @SerialName("customerId") val customerId: String,
    @SerialName("customerType") val customerType: String = "",
    @SerialName("segment") val segment: String = "",
    @SerialName("tier") val tier: String = "",
)

@Serializable
data class StackableOrderInfo(
    @SerialName("orderId") val orderId: String,
    @SerialName("orderValue") val orderValue: String,
    @SerialName("currency") val currency: String = "VND",
    @SerialName("orderDate") val orderDate: String = "",
    @SerialName("channel") val channel: String = "MOBILE",
    @SerialName("location") val location: String = "",
    @SerialName("items") val items: List<StackableOrderItem> = emptyList(),
)

@Serializable
data class StackableOrderItem(
    @SerialName("sku") val sku: String,
    @SerialName("productId") val productId: String,
    @SerialName("collectionIds") val collectionIds: List<String> = emptyList(),
    @SerialName("quantity") val quantity: Int,
    @SerialName("price") val price: String,
    @SerialName("category") val category: String = "",
)

@Serializable
data class DiscountRequest(
    @SerialName("objectType") val objectType: String,
    @SerialName("objectId") val objectId: String,
    @SerialName("priority") val priority: Int,
    @SerialName("expectedDiscount") val expectedDiscount: String = "",
    @SerialName("maxDiscountCap") val maxDiscountCap: String = "",
)

@Serializable
data class ValidationOptions(
    @SerialName("checkBudgetAvailability") val checkBudgetAvailability: Boolean = true,
    @SerialName("optimizeOrder") val optimizeOrder: Boolean = true,
    @SerialName("explainLevel") val explainLevel: String = "BASIC",
    @SerialName("includeAlternatives") val includeAlternatives: Boolean = false,
    @SerialName("dryRun") val dryRun: Boolean = true,
)
