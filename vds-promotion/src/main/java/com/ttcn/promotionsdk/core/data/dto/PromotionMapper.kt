package com.ttcn.promotionsdk.core.data.dto

import com.ttcn.promotionsdk.core.data.dto.voucher.CustomerVoucherDetail
import com.ttcn.promotionsdk.core.data.dto.voucher.SearchCustomerVouchersData
import com.ttcn.promotionsdk.core.data.dto.voucher.VoucherListItem
import com.ttcn.promotionsdk.core.data.dto.voucher.VoucherPage
import com.ttcn.promotionsdk.core.data.dto.voucher.VoucherTabInfo
import com.ttcn.promotionsdk.core.domain.model.VoucherDetail
import com.ttcn.promotionsdk.core.domain.model.VoucherItem
import com.ttcn.promotionsdk.core.domain.model.VoucherListPage
import com.ttcn.promotionsdk.core.domain.model.VoucherSearchResult
import com.ttcn.promotionsdk.core.domain.model.VoucherTabItem

internal object PromotionMapper

internal fun SearchCustomerVouchersData.toVoucherSearchResult() = VoucherSearchResult(
    tabs = tabs.map { it.toVoucherTabItem() },
    defaultTab = defaultTab,
    selectedTab = selectedTab,
    myVouchers = myVouchers?.toVoucherListPage(),
    otherVouchers = otherVouchers?.toVoucherListPage(),
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
)

private fun VoucherTabInfo.toVoucherTabItem() = VoucherTabItem(
    code = code,
    label = label,
    count = count,
    order = order,
)

private fun VoucherPage.toVoucherListPage() = VoucherListPage(
    content = content.map { it.toVoucherItem() },
    number = number,
    size = size,
    last = last,
    totalElements = totalElements,
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
)
