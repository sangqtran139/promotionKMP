package com.ttcn.promotionsdk.core.data.dto.stackablediscount

import com.google.gson.annotations.SerializedName

data class StackableDiscountsRequest(
    @SerializedName("idempotencyKey") val idempotencyKey: String = "",
    @SerializedName("customerInfo") val customerInfo: StackableCustomerInfo,
    @SerializedName("orderInfo") val orderInfo: StackableOrderInfo,
    @SerializedName("discountRequests") val discountRequests: List<DiscountRequest> = emptyList(),
    @SerializedName("validationOptions") val validationOptions: ValidationOptions = ValidationOptions(),
)

data class StackableCustomerInfo(
    @SerializedName("customerId") val customerId: String,
    @SerializedName("customerType") val customerType: String = "",
    @SerializedName("segment") val segment: String = "",
    @SerializedName("tier") val tier: String = "",
)

data class StackableOrderInfo(
    @SerializedName("orderId") val orderId: String,
    @SerializedName("orderValue") val orderValue: String,
    @SerializedName("currency") val currency: String = "VND",
    @SerializedName("orderDate") val orderDate: String = "",
    @SerializedName("channel") val channel: String = "MOBILE",
    @SerializedName("location") val location: String = "",
    @SerializedName("items") val items: List<StackableOrderItem> = emptyList(),
)

data class StackableOrderItem(
    @SerializedName("sku") val sku: String,
    @SerializedName("productId") val productId: String,
    @SerializedName("collectionIds") val collectionIds: List<String> = emptyList(),
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("price") val price: String,
    @SerializedName("category") val category: String = "",
)

data class DiscountRequest(
    @SerializedName("objectType") val objectType: String,
    @SerializedName("objectId") val objectId: String,
    @SerializedName("priority") val priority: Int,
    @SerializedName("expectedDiscount") val expectedDiscount: String = "",
    @SerializedName("maxDiscountCap") val maxDiscountCap: String = "",
)

data class ValidationOptions(
    @SerializedName("checkBudgetAvailability") val checkBudgetAvailability: Boolean = true,
    @SerializedName("optimizeOrder") val optimizeOrder: Boolean = true,
    @SerializedName("explainLevel") val explainLevel: String = "BASIC",
    @SerializedName("includeAlternatives") val includeAlternatives: Boolean = false,
    @SerializedName("dryRun") val dryRun: Boolean = true,
)

