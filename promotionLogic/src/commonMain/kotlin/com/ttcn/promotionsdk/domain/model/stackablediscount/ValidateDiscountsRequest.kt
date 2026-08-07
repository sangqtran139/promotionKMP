package com.ttcn.promotionsdk.domain.model.stackablediscount

data class ValidateDiscountsRequest(
    val orderId: String,
    val orderValue: String,
    val items: List<DiscountItemRequest>,
)

data class DiscountItemRequest(
    val objectId: String,
    val objectType: String = "CAMPAIGN",
)
