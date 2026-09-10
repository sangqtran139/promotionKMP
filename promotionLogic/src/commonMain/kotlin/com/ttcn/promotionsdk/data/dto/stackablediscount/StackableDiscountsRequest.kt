package com.ttcn.promotionsdk.data.dto.stackablediscount

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class StackableDiscountsRequest(
    @SerialName("idempotencyKey") val idempotencyKey: String = "",
    @SerialName("customerInfo") val customerInfo: StackableCustomerInfo,
    @SerialName("orderInfo") val orderInfo: StackableOrderInfo,
    @SerialName("discountRequests") val discountRequests: List<DiscountRequest> = emptyList(),
    @SerialName("validationOptions") val validationOptions: ValidationOptions = ValidationOptions(),
)

@Serializable
public data class StackableCustomerInfo(
    @SerialName("customerType") val customerType: String = "",
    @SerialName("segment") val segment: String = "",
    @SerialName("tier") val tier: String = "",
)

@Serializable
public data class StackableOrderInfo(
    @SerialName("orderId") val orderId: String,
    @SerialName("orderValue") val orderValue: String,
    @SerialName("currency") val currency: String = "VND",
    @SerialName("orderDate") val orderDate: String = "",
    @SerialName("channel") val channel: String = "MOBILE",
    @SerialName("location") val location: String = "",
    @SerialName("items") val items: List<StackableOrderItem> = emptyList(),
)

@Serializable
public data class StackableOrderItem(
    @SerialName("sku") val sku: String,
    @SerialName("productId") val productId: String,
    @SerialName("collectionIds") val collectionIds: List<String> = emptyList(),
    @SerialName("quantity") val quantity: Int,
    @SerialName("price") val price: String,
    @SerialName("category") val category: String = "",
)

@Serializable
public data class DiscountRequest(
    @SerialName("objectType") val objectType: String,
    @SerialName("objectId") val objectId: String,
    @SerialName("priority") val priority: Int,
    @SerialName("expectedDiscount") val expectedDiscount: String = "",
    @SerialName("maxDiscountCap") val maxDiscountCap: String = "",
)

@Serializable
public data class ValidationOptions(
    @SerialName("checkBudgetAvailability") val checkBudgetAvailability: Boolean = true,
    @SerialName("optimizeOrder") val optimizeOrder: Boolean = true,
    @SerialName("explainLevel") val explainLevel: String = "BASIC",
    @SerialName("includeAlternatives") val includeAlternatives: Boolean = false,
    @SerialName("dryRun") val dryRun: Boolean = true,
)
