package com.ttcn.promotionsdk.core.domain.model

data class CreateRedemptionRequest(
    val customerId: String,
    val orderId: String,
    val orderValue: String,
    val items: List<RedemptionItemRequest>,
)

data class RedemptionItemRequest(
    val objectId: String,
    val objectType: String,
    val expectedDiscount: String? = null,
)
