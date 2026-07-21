package com.ttcn.promotionsdk.core.data.dto.voucher

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
    voucherId = voucher.id,
    merchantName = voucher.brand?.name,
    logo = voucher.brand?.logo?.firstOrNull(),
    banner = voucher.image,
    title = voucher.title,
    description = voucher.description,
    // `guideline` (điều khoản/hướng dẫn) = nội dung ngắn `voucher.content`.
    guideline = voucher.content,
    startDate = startDate,
    expirationDate = endDate,
    status = metadata.toStatusRaw(),
    displayStatusLabel = metadata?.disabledReason,
    // `voucher.id` chính là campaign_id sinh ra voucher (spec §6.2).
    campaignId = voucher.id,
    campaignType = null,
    campaignStatus = null,
    applicableProducts = emptyList(),
)

private fun VoucherTabInfo.toVoucherTabItem() = VoucherTabItem(
    code = code,
    label = label,
    count = count,
    order = order,
    isDefault = default ?: false,
)

private fun VoucherListItem.toVoucherItem() = VoucherItem(
    voucherId = voucher.id,
    merchantName = voucher.brand?.name,
    title = voucher.title,
    description = voucher.description,
    logo = voucher.brand?.logo?.firstOrNull(),
    expirationDate = endDate,
    status = metadata.toStatusRaw(),
    displayStatusLabel = metadata?.disabledReason,
    campaignId = voucher.id,
    campaignType = null,
    objectType = "CAMPAIGN",
    // Spec mới không còn `isAutoApplied` — mặc định không tự áp.
    isAutoApplied = false,
    applicableProducts = emptyList(),
)

/**
 * Suy `status` (khuôn cũ, feed `VoucherStatus`) từ `metadata.usable` + `disabledReason`:
 * - `usable="false"` → `disabledReason` (EXPIRED/REDEEMED map thẳng enum; mã lạ → UNKNOWN → fail-closed).
 * - `usable="true"` → `USABLE`.
 * - null/khác → null (UI xử lý như không rõ trạng thái).
 */
private fun VoucherMetadataDto?.toStatusRaw(): String? = when {
    this == null -> null
    usable.equals("false", ignoreCase = true) -> disabledReason ?: "INELIGIBLE"
    usable.equals("true", ignoreCase = true) -> "USABLE"
    else -> null
}
