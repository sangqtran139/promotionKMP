package com.ttcn.promotionsdk.data.dto.stackablediscount

import com.ttcn.promotionsdk.domain.model.stackablediscount.DiscountItemResult
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsResult
import com.ttcn.promotionsdk.common.randomUuidString

/**
 * Mapper giữa DTO stackable-discount (data layer) và domain model.
 *
 * Nằm ở data layer để domain (`PromotionRepository`, use case) không phải biết DTO —
 * giữ đúng hướng phụ thuộc data → domain của Clean Architecture.
 */
internal fun ValidateDiscountsRequest.toStackableDiscountsRequest() = StackableDiscountsRequest(
    idempotencyKey = randomUuidString(),
    customerInfo = StackableCustomerInfo(),
    orderInfo = StackableOrderInfo(orderId = orderId, orderValue = orderValue),
    discountRequests = items.mapIndexed { index, item ->
        DiscountRequest(
            objectType = item.objectType,
            objectId = item.objectId,
            priority = index + 1,
        )
    },
)

internal fun StackableDiscountsResponse.toValidateDiscountsResult() = ValidateDiscountsResult(
    overallValid = validationResult.overallValid,
    totalDiscountAmount = validationResult.totalDiscountAmount,
    finalAmount = validationResult.finalAmount,
    items = discountDetails.map { detail ->
        DiscountItemResult(
            objectId = detail.objectId,
            objectType = detail.objectType,
            valid = detail.valid,
            calculatedDiscount = detail.calculatedDiscount,
            eligibilityStatus = detail.eligibilityStatus,
            tags = detail.tags,
        )
    },
)
