package com.ttcn.promotionsdk.ui.feature.promotion.ext

import com.ttcn.promotionsdk.core.data.dto.stackablediscount.DiscountDetail
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.DiscountRequest
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableCustomerInfo
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableDiscountsRequest
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableOrderInfo
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyVoucherListItem
import java.util.UUID

@JvmName("voucherItemsToStackableDiscountsRequest")
internal fun List<MyVoucherListItem>.toStackableDiscountsRequest(
    customerId: String,
    orderId: String,
    orderValue: String,
): StackableDiscountsRequest = StackableDiscountsRequest(
    idempotencyKey = UUID.randomUUID().toString(),
    customerInfo = StackableCustomerInfo(customerId = customerId),
    orderInfo = StackableOrderInfo(orderId = orderId, orderValue = orderValue),
    discountRequests = mapIndexed { index, voucher ->
        DiscountRequest(
            objectType = voucher.objectType,
            objectId = voucher.voucherId,
            priority = index + 1,
        )
    },
)

@JvmName("discountDetailsToStackableDiscountsRequest")
internal fun List<DiscountDetail>.toStackableDiscountsRequest(
    customerId: String,
    orderId: String,
    orderValue: String,
): StackableDiscountsRequest = StackableDiscountsRequest(
    idempotencyKey = UUID.randomUUID().toString(),
    customerInfo = StackableCustomerInfo(customerId = customerId),
    orderInfo = StackableOrderInfo(orderId = orderId, orderValue = orderValue),
    discountRequests = mapIndexed { index, detail ->
        DiscountRequest(
            objectType = detail.objectType,
            objectId = detail.objectId,
            priority = index + 1,
        )
    },
)
