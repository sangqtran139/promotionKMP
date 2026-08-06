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
    expireWarningDate = expireWarningDate?.toInt(),
)

internal fun CustomerVoucherDetail.toVoucherDetail() = VoucherDetail(
    voucherId = voucher.id,
    merchantName = voucher.brand?.name,
    logo = voucher.brand?.logo?.firstOrNull(),
    banner = voucher.image,
    title = voucher.title,
    description = voucher.content,
    guideline = voucher.guideline,
    startDate = startDate,
    expirationDate = endDate,
    status = metadata.toStatusRaw(),
    displayStatusLabel = voucher.displayLabelOrReason(metadata),
    // `voucher.id` chính là campaign_id sinh ra voucher (spec §6.2).
    campaignId = voucher.id,
    campaignType = null,
    campaignStatus = null,
    applicableProducts = applicableProducts.ifEmpty { voucher.applicableProducts }.toApplicableProducts(),
    expireWarningDate = expireWarningDate?.toInt(),
    codes = codes.mapNotNull { it.codex?.takeIf { c -> c.isNotBlank() } },
    usageGuideUrl = metadata?.usageGuideUrl,
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
    displayStatusLabel = voucher.displayLabelOrReason(metadata),
    campaignId = voucher.id,
    campaignType = null,
    objectType = "CAMPAIGN",
    // Spec mới không còn `isAutoApplied` — mặc định không tự áp.
    isAutoApplied = false,
    applicableProducts = applicableProducts.ifEmpty { voucher.applicableProducts }.toApplicableProducts(),
)

/**
 * Nhãn hiển thị của voucher: ưu tiên `voucher.displayStatusLabel` của server ("Sử dụng", …), server
 * không gửi thì rơi về `metadata.disabledReason` — **mã enum** (`EXPIRED`, `REDEEMED`,
 * `SERVICE_NOT_APPLICABLE`), không phải chuỗi hiển thị.
 */
private fun VoucherInfoDto.displayLabelOrReason(metadata: VoucherMetadataDto?): String? =
    displayStatusLabel?.takeIf { it.isNotBlank() } ?: metadata?.disabledReason

/**
 * `applicableProducts` → domain. Bỏ phần tử thiếu `productId` vì đó chính là khoá khớp với
 * `PromotionAvailableService.serviceCode`; không có id thì không lọc dịch vụ được.
 *
 * **Giữ nguyên cả `type = EXCLUDED`** — quyết định dùng hay loại thuộc về tầng lọc
 * (`servicesForApplicableProducts`), mapper không tự cắt dữ liệu server.
 */
private fun List<ApplicableProductDto>.toApplicableProducts(): List<ApplicableProduct> = mapNotNull { dto ->
    val id = dto.productId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
    ApplicableProduct(
        productId = id,
        sku = dto.sku,
        name = dto.name.orEmpty(),
        image = dto.image,
        type = dto.type.orEmpty(),
    )
}

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
