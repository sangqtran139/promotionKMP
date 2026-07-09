package com.ttcn.promotionsdk.core.data.dto.eligible

import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffersResult
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOrderItem
import com.ttcn.promotionsdk.core.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherTabItem

internal fun FindEligibleCampaignsRequest.toEligibleCampaignsRequest() = EligibleCampaignsRequest(
    customerInfo = EligibleCustomerInfo(
        customerId = customerId,
        customerType = customerType,
        segment = segment,
        tier = tier,
    ),
    orderInfo = EligibleOrderInfo(
        orderId = orderId,
        orderValue = orderValue,
        currency = currency,
        channel = channel,
        items = items.map { it.toDto() },
    ),
    filterOptions = EligibleFilterOptionsDto(
        includeExpired = filterOptions.includeExpired,
        checkBudgetAvailability = filterOptions.checkBudgetAvailability,
        includePreview = filterOptions.includePreview,
    ),
    pagination = EligiblePagination(
        myOffers = EligiblePageRequest(page = myPage, size = mySize),
        otherOffers = EligiblePageRequest(page = otherPage, size = otherSize),
        tabCode = tabCode?.takeIf { it.isNotBlank() },
        sectionCode = section?.code,
    ),
)

private fun EligibleOrderItem.toDto() = EligibleOrderItemDto(
    skuId = skuId,
    quantity = quantity,
    unitPrice = unitPrice,
    orderItemId = orderItemId,
    productId = productId,
    productName = productName,
    productCategory = productCategory,
)

internal fun EligibleCampaignsResponse.toEligibleOffersResult() = EligibleOffersResult(
    myOffers = myOffers?.content.orEmpty().mapNotNull { it.toEligibleOffer() },
    otherOffers = otherOffers?.content.orEmpty().mapNotNull { it.toEligibleOffer() },
    tabs = tabs.mapNotNull { it.toVoucherTabItem() }.sortedBy { it.order ?: Int.MAX_VALUE },
    activeTab = activeTab,
    myIsLastPage = myOffers?.last ?: true,
    otherIsLastPage = otherOffers?.last ?: true,
    myTotalElements = myOffers?.totalElements ?: 0,
    otherTotalElements = otherOffers?.totalElements ?: 0,
)

/**
 * Offer không có cả `voucherId` lẫn `campaignId` thì không định danh được để redeem — bỏ qua,
 * giống `compactMap` ở bản iOS.
 */
private fun EligibleOfferDto.toEligibleOffer(): EligibleOffer? {
    val id = voucherId ?: campaignId ?: return null
    return EligibleOffer(
        id = id,
        campaignId = campaignId,
        voucherId = voucherId,
        campaignName = campaignName,
        campaignType = campaignType,
        objectType = campaignType ?: "CAMPAIGN",
        discountType = discountType,
        usable = usable ?: true,
        estimatedDiscount = discountPreview?.estimatedDiscount,
        discountPercentage = discountPreview?.discountPercentage,
        maxDiscount = discountPreview?.maxDiscount,
        minOrderValue = discountPreview?.minOrderValue,
        startDate = validity?.startDate,
        expireDate = validity?.endDate,
        remainingRedemptions = validity?.remainingRedemptions,
        budgetAvailable = budgetStatus?.available,
        unmatchedRules = eligibilityDetails?.unmatchedRules.orEmpty(),
    )
}

private fun EligibleTabConfig.toVoucherTabItem(): VoucherTabItem? {
    val code = code ?: return null
    return VoucherTabItem(
        code = code,
        label = label.orEmpty(),
        count = count,
        order = order,
        isDefault = default ?: false,
    )
}
