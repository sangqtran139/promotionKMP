package com.ttcn.promotionsdk.core.data.dto.redemption

import com.google.gson.annotations.SerializedName

data class RedemptionSessionRequest(
    @SerializedName("idempotencyKey") val idempotencyKey: String = "",
    @SerializedName("customerInfo") val customerInfo: RedemptionCustomerInfo,
    @SerializedName("orderInfo") val orderInfo: RedemptionOrderInfo,
    @SerializedName("selectedRedeemables") val selectedRedeemables: List<RedeemableRequest> = emptyList(),
    @SerializedName("sessionOptions") val sessionOptions: SessionOptions = SessionOptions(),
)

data class RedemptionCustomerInfo(
    @SerializedName("customerId") val customerId: String,
    @SerializedName("customerType") val customerType: String = "",
    @SerializedName("segment") val segment: String = "",
    @SerializedName("tier") val tier: String = "",
)

data class RedemptionOrderInfo(
    @SerializedName("orderId") val orderId: String,
    @SerializedName("orderValue") val orderValue: String,
    @SerializedName("currency") val currency: String = "VND",
    @SerializedName("orderDate") val orderDate: String = "",
    @SerializedName("channel") val channel: String = "MOBILE",
    @SerializedName("location") val location: String = "",
    @SerializedName("items") val items: List<RedemptionOrderItem> = emptyList(),
)

data class RedemptionOrderItem(
    @SerializedName("skuId") val skuId: String,
    @SerializedName("productId") val productId: String,
    @SerializedName("collectionIds") val collectionIds: List<String> = emptyList(),
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("unitPrice") val unitPrice: String,
    @SerializedName("subTotal") val subTotal: String,
)

data class RedeemableRequest(
    @SerializedName("objectType") val objectType: String,
    @SerializedName("objectId") val objectId: String,
    @SerializedName("priority") val priority: Int,
    @SerializedName("expectedDiscount") val expectedDiscount: String,
    @SerializedName("metadata") val metadata: Map<String, Any> = emptyMap(),
)

data class SessionOptions(
    @SerializedName("timeoutSeconds") val timeoutSeconds: Int = 300,
    @SerializedName("holdBudget") val holdBudget: Boolean = true,
    @SerializedName("validateOnly") val validateOnly: Boolean = false,
    @SerializedName("autoConfirm") val autoConfirm: Boolean = false,
)