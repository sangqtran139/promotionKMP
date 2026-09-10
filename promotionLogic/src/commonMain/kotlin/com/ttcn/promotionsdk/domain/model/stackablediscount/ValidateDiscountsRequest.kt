package com.ttcn.promotionsdk.domain.model.stackablediscount

public data class ValidateDiscountsRequest(
    val orderId: String,
    val orderValue: String,
    val items: List<DiscountItemRequest>,
)

public data class DiscountItemRequest(
    val objectId: String,
    val objectType: String = "CAMPAIGN",
)
