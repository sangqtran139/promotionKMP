package com.ttcn.promotionsdk.domain.model.redemption

public data class CreateRedemptionRequest(
    val orderId: String,
    val orderValue: String,
    val items: List<RedemptionItemRequest>,
)

public data class RedemptionItemRequest(
    val objectId: String,
    val objectType: String,
    val expectedDiscount: String? = null,
)
