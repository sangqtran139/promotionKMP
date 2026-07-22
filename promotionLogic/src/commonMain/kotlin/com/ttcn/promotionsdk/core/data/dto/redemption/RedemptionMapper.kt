package com.ttcn.promotionsdk.core.data.dto.redemption

import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionResult
import com.ttcn.promotionsdk.core.domain.model.redemption.RedemptionValidationError
import com.ttcn.promotionsdk.core.util.randomUuidString

/**
 * Mapper giữa DTO redemption (data layer) và domain model.
 *
 * Nằm ở data layer để domain (`PromotionRepository`, use case) không phải biết DTO —
 * giữ đúng hướng phụ thuộc data → domain của Clean Architecture.
 */
internal fun CreateRedemptionRequest.toRedemptionSessionRequest() = RedemptionSessionRequest(
    idempotencyKey = randomUuidString(),
    customerInfo = RedemptionCustomerInfo(),
    orderInfo = RedemptionOrderInfo(orderId = orderId, orderValue = orderValue),
    selectedRedeemables = items.mapIndexed { index, item ->
        RedeemableRequest(
            objectType = item.objectType,
            objectId = item.objectId,
            priority = index + 1,
            expectedDiscount = item.expectedDiscount ?: "",
        )
    },
)

internal fun RedemptionSessionResponse.toCreateRedemptionResult() = CreateRedemptionResult(
    sessionId = sessionId,
    totalDiscount = preview?.totalDiscount.orEmpty(),
    finalAmount = preview?.finalAmount.orEmpty(),
    validationErrors = validationErrors.map { error ->
        RedemptionValidationError(
            code = error.code,
            message = error.message,
        )
    },
)
