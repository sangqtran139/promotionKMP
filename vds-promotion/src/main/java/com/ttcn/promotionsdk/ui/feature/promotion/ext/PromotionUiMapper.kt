package com.ttcn.promotionsdk.ui.feature.promotion.ext

import com.ttcn.promotionsdk.ui.entry.AppliedDiscount
import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.DiscountItemRequest
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.DiscountItemResult
import com.ttcn.promotionsdk.core.domain.model.redemption.RedemptionItemRequest
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyVoucherListItem

/**
 * Mapper tầng presentation: dựng **domain request** từ model UI/[AppliedDiscount], và map
 * **domain result ([DiscountItemResult]) → [AppliedDiscount]** cho public surface (callback, [PRMEndowView]).
 *
 * [AppliedDiscount] là model **public** của SDK (ui/entry); domain (use case, repository)
 * chỉ làm việc với domain model, DTO data layer không rò lên đây.
 */

// ─── Build domain request ───────────────────────────────────────────────────

@JvmName("voucherItemsToValidateDiscountsRequest")
internal fun List<MyVoucherListItem>.toValidateDiscountsRequest(
    customerId: String,
    orderId: String,
    orderValue: String,
): ValidateDiscountsRequest = ValidateDiscountsRequest(
    customerId = customerId,
    orderId = orderId,
    orderValue = orderValue,
    items = map { voucher ->
        DiscountItemRequest(objectId = voucher.voucherId, objectType = voucher.objectType)
    },
)

@JvmName("discountDetailsToValidateDiscountsRequest")
internal fun List<AppliedDiscount>.toValidateDiscountsRequest(
    customerId: String,
    orderId: String,
    orderValue: String,
): ValidateDiscountsRequest = ValidateDiscountsRequest(
    customerId = customerId,
    orderId = orderId,
    orderValue = orderValue,
    items = map { detail ->
        DiscountItemRequest(objectId = detail.objectId, objectType = detail.objectType)
    },
)

internal fun List<AppliedDiscount>.toCreateRedemptionRequest(
    customerId: String,
    orderId: String,
    orderValue: String,
): CreateRedemptionRequest = CreateRedemptionRequest(
    customerId = customerId,
    orderId = orderId,
    orderValue = orderValue,
    items = map { detail ->
        RedemptionItemRequest(
            objectId = detail.objectId,
            objectType = detail.objectType,
            expectedDiscount = detail.calculatedDiscount,
        )
    },
)

// ─── Map domain result → AppliedDiscount (model public) ──────────────────────

internal fun DiscountItemResult.toAppliedDiscount(): AppliedDiscount = AppliedDiscount(
    objectId = objectId,
    objectType = objectType,
    valid = valid,
    calculatedDiscount = calculatedDiscount,
    eligibilityStatus = eligibilityStatus,
)

internal fun List<DiscountItemResult>.toAppliedDiscounts(): List<AppliedDiscount> =
    map { it.toAppliedDiscount() }
