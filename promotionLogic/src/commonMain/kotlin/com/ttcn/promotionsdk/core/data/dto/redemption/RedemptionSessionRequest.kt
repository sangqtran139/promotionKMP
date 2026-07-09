package com.ttcn.promotionsdk.core.data.dto.redemption

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class RedemptionSessionRequest(
    @SerialName("idempotencyKey") val idempotencyKey: String = "",
    @SerialName("customerInfo") val customerInfo: RedemptionCustomerInfo,
    @SerialName("orderInfo") val orderInfo: RedemptionOrderInfo,
    @SerialName("selectedRedeemables") val selectedRedeemables: List<RedeemableRequest> = emptyList(),
    @SerialName("sessionOptions") val sessionOptions: SessionOptions = SessionOptions(),
)

@Serializable
data class RedemptionCustomerInfo(
    @SerialName("customerId") val customerId: String,
    @SerialName("customerType") val customerType: String = "",
    @SerialName("segment") val segment: String = "",
    @SerialName("tier") val tier: String = "",
)

@Serializable
data class RedemptionOrderInfo(
    @SerialName("orderId") val orderId: String,
    @SerialName("orderValue") val orderValue: String,
    @SerialName("currency") val currency: String = "VND",
    @SerialName("orderDate") val orderDate: String = "",
    @SerialName("channel") val channel: String = "MOBILE",
    @SerialName("location") val location: String = "",
    @SerialName("items") val items: List<RedemptionOrderItem> = emptyList(),
)

@Serializable
data class RedemptionOrderItem(
    @SerialName("skuId") val skuId: String,
    @SerialName("productId") val productId: String,
    @SerialName("collectionIds") val collectionIds: List<String> = emptyList(),
    @SerialName("quantity") val quantity: Int,
    @SerialName("unitPrice") val unitPrice: String,
    @SerialName("subTotal") val subTotal: String,
)

@Serializable
data class RedeemableRequest(
    @SerialName("objectType") val objectType: String,
    @SerialName("objectId") val objectId: String,
    @SerialName("priority") val priority: Int,
    @SerialName("expectedDiscount") val expectedDiscount: String,
    /**
     * Gson serialize `Map<String, Any>` bằng reflection lúc runtime; kotlinx.serialization cần kiểu
     * tĩnh, nên payload tự do này chuyển sang [JsonObject]. Server nhận cùng một JSON.
     */
    @SerialName("metadata") val metadata: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class SessionOptions(
    @SerialName("timeoutSeconds") val timeoutSeconds: Int = 300,
    @SerialName("holdBudget") val holdBudget: Boolean = true,
    @SerialName("validateOnly") val validateOnly: Boolean = false,
    @SerialName("autoConfirm") val autoConfirm: Boolean = false,
)
