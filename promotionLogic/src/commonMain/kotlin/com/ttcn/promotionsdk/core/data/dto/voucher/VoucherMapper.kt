package com.ttcn.promotionsdk.core.data.dto.voucher

import com.ttcn.promotionsdk.core.domain.model.voucher.ApplicableProduct
import com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersResult
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherDetail
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherItem
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherTabItem

internal fun SearchCustomerVouchersResponse.toSearchCustomerVouchersResult() = SearchCustomerVouchersResult(
    keyword = keyword,
    serviceCode = serviceCode,
    tabs = tabs.map { it.toVoucherTabItem() },
    defaultTab = defaultTab,
    selectedTab = selectedTab,
    content = content.map { it.toVoucherItem() },
    number = number,
    size = size,
    last = last,
    totalElements = totalElements,
)

internal fun CustomerVoucherDetail.toVoucherDetail() = VoucherDetail(
    voucherId = voucherId,
    merchantName = merchantName,
    logo = logo,
    banner = banner,
    title = title,
    description = description,
    guideline = guideline,
    startDate = startDate,
    expirationDate = expirationDate,
    status = status,
    displayStatusLabel = displayStatusLabel,
    campaignId = campaignId,
    campaignType = campaignType,
    campaignStatus = campaignStatus,
    applicableProducts = applicableProducts.map { it.toApplicableProduct() },
)

private fun VoucherTabInfo.toVoucherTabItem() = VoucherTabItem(
    code = code,
    label = label,
    count = count,
    order = order,
    isDefault = default ?: false,
)

private fun VoucherListItem.toVoucherItem() = VoucherItem(
    voucherId = voucherId,
    merchantName = merchantName,
    title = title,
    description = description,
    logo = logo,
    expirationDate = expirationDate,
    status = status,
    displayStatusLabel = displayStatusLabel,
    campaignId = campaignId,
    campaignType = campaignType,
    objectType = campaignType ?: "CAMPAIGN",
    isAutoApplied = isAutoApplied ?: false,
    applicableProducts = applicableProducts.map { it.toApplicableProduct() },
)

private fun ApplicableProductDto.toApplicableProduct() = ApplicableProduct(
    productId = productId,
    sku = sku,
    name = name,
    image = image,
    type = type,
)
