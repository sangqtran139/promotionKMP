package com.ttcn.promotionsdk.domain.model.redemption

data class CreateRedemptionRequest(
    val orderId: String,
    val orderValue: String,
    val items: List<RedemptionItemRequest>,
)

data class RedemptionItemRequest(
    val objectId: String,
    val objectType: String,
    val expectedDiscount: String? = null,
)
