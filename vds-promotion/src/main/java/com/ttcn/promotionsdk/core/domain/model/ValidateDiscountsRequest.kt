package com.ttcn.promotionsdk.core.domain.model

data class ValidateDiscountsRequest(
    val customerId: String,
    val orderId: String,
    val orderValue: String,
    val items: List<DiscountItemRequest>,
)

data class DiscountItemRequest(
    val objectId: String,
    val objectType: String = "CAMPAIGN",
)
