package com.ttcn.promotionsdk.data.dto.voucher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Payload `data` của API tìm kiếm voucher khách hàng (`GET .../customer-vouchers`), v1.8.
 * Map sang domain `SearchCustomerVouchersResult` qua [VoucherMapper].
 *
 * `data` là 1 Spring `Page<VoucherItem>` phẳng + thanh tab động (`tabs[]`). Breaking v1.7: mỗi phần
 * tử `content[]` là item phẳng (khuôn giống Detail §6, trừ `codes`). v1.8: thêm `expireWarningDate`.
 */
@Serializable
public data class SearchCustomerVouchersResponse(
    @SerialName("keyword") val keyword: String? = null,
    @SerialName("serviceCode") val serviceCode: String? = null,
    /** Ngưỡng cảnh báo sắp hết hạn (đơn vị ngày) — cấu hình tĩnh tại BFF. */
    @SerialName("expireWarningDate") val expireWarningDate: Double? = null,
    @SerialName("tabs") val tabs: List<VoucherTabInfo> = emptyList(),
    @SerialName("defaultTab") val defaultTab: String? = null,
    @SerialName("selectedTab") val selectedTab: String? = null,
    @SerialName("content") val content: List<VoucherListItem> = emptyList(),
    @SerialName("pageable") val pageable: PageableInfo? = null,
    @SerialName("totalElements") val totalElements: Long? = null,
    @SerialName("totalPages") val totalPages: Int? = null,
    @SerialName("first") val first: Boolean? = null,
    @SerialName("last") val last: Boolean? = null,
    @SerialName("number") val number: Int? = null,
    @SerialName("size") val size: Int? = null,
    @SerialName("numberOfElements") val numberOfElements: Int? = null,
    @SerialName("empty") val empty: Boolean? = null,
    @SerialName("sort") val sort: SortInfo? = null,
)

@Serializable
public data class VoucherTabInfo(
    @SerialName("code") val code: String,
    @SerialName("label") val label: String,
    @SerialName("labelI18n") val labelI18n: Map<String, String>? = null,
    @SerialName("default") val default: Boolean? = null,
    @SerialName("count") val count: Int? = null,
    @SerialName("order") val order: Int? = null,
)

@Serializable
public data class PageableInfo(
    @SerialName("pageNumber") val pageNumber: Int? = null,
    @SerialName("pageSize") val pageSize: Int? = null,
    @SerialName("offset") val offset: Long? = null,
    @SerialName("paged") val paged: Boolean? = null,
    @SerialName("unpaged") val unpaged: Boolean? = null,
    @SerialName("sort") val sort: SortInfo? = null,
)

@Serializable
public data class SortInfo(
    @SerialName("sorted") val sorted: Boolean? = null,
    @SerialName("unsorted") val unsorted: Boolean? = null,
    @SerialName("empty") val empty: Boolean? = null,
)

/**
 * Một phần tử `content[]` — item phẳng (khuôn giống `data` của API Detail §6, trừ `codes`).
 * Gồm object [VoucherInfoDto] + các field ngang hàng.
 */
@Serializable
public data class VoucherListItem(
    @SerialName("voucher") val voucher: VoucherInfoDto,
    @SerialName("quantity") val quantity: Int? = null,
    @SerialName("value") val value: Double? = null,
    @SerialName("amount") val amount: Double? = null,
    @SerialName("startDate") val startDate: String? = null,
    @SerialName("endDate") val endDate: String? = null,
    @SerialName("expiredTimeNumber") val expiredTimeNumber: Int? = null,
    @SerialName("priority") val priority: Int? = null,
    /** `1` = voucher thuộc sở hữu của chính khách. API này luôn `1`. */
    @SerialName("isYourself") val isYourself: Int? = null,
    @SerialName("metadata") val metadata: VoucherMetadataDto? = null,
    /** Sản phẩm/SKU voucher áp dụng — nguồn của bottom sheet "Chọn dịch vụ". */
    @SerialName("applicableProducts") val applicableProducts: List<ApplicableProductDto> = emptyList(),
)
