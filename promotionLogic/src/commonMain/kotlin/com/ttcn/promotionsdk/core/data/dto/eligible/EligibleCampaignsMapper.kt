package com.ttcn.promotionsdk.core.data.dto.eligible

import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffersResult
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOrderItem
import com.ttcn.promotionsdk.core.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherTabItem

internal fun FindEligibleCampaignsRequest.toEligibleCampaignsRequest() = EligibleCampaignsRequest(
    // Định danh khách lấy từ JWT-sub → không gửi customerId. customerType/segment/tier đưa vào
    // customerInfo.metadata (free map) để không mất input đánh giá ưu đãi.
    customerInfo = EligibleCustomerInfo(
        metadata = buildMap {
            customerType?.let { put("customerType", it) }
            segment?.let { put("segment", it) }
            tier?.let { put("tier", it) }
        }.ifEmpty { null },
    ),
    orderInfo = EligibleOrderInfo(
        orderId = orderId,
        orderValue = orderValue,
        currency = currency,
        items = items.map { it.toDto() },
    ),
    filterOptions = EligibleFilterOptionsDto(
        includeExpired = filterOptions.includeExpired,
        checkBudgetAvailability = filterOptions.checkBudgetAvailability,
        includePreview = filterOptions.includePreview,
    ),
    // sectionCode nay top-level; tabCode/channel bị bỏ khỏi request v1.6.
    sectionCode = section?.code,
    pagination = EligiblePagination(
        myOffers = EligiblePageRequest(page = myPage, size = mySize),
        otherOffers = EligiblePageRequest(page = otherPage, size = otherSize),
    ),
)

private fun EligibleOrderItem.toDto() = EligibleOrderItemDto(
    orderItemId = orderItemId,
    // skuId của host = skuSourceId đối tác (BE resolve skuId nội bộ).
    skuSourceId = skuId,
    productId = productId,
    quantity = quantity,
    unitPrice = unitPrice,
    metadata = buildMap {
        productName?.let { put("productName", it) }
        productCategory?.let { put("productCategory", it) }
    }.ifEmpty { null },
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
        // Ưu tiên tên hiển thị voucher (v1.6), fallback tên campaign.
        campaignName = voucherName ?: campaignName,
        campaignType = campaignType,
        objectType = campaignType ?: "CAMPAIGN",
        discountType = discountType,
        usable = usable ?: true,
        estimatedDiscount = discountPreview?.estimatedDiscount,
        discountPercentage = discountPreview?.discountPercentage,
        maxDiscount = discountPreview?.maxDiscount,
        minOrderValue = discountPreview?.minOrderValue,
        startDate = validity?.startDate,
        // Ưu tiên `expiresAt` (hạn voucher đang sở hữu), fallback hạn campaign.
        expireDate = expiresAt ?: validity?.endDate,
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
