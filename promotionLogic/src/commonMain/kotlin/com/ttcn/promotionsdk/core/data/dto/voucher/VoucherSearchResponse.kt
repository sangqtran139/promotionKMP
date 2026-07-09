package com.ttcn.promotionsdk.core.data.dto.voucher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Payload trả về của API tìm kiếm voucher khách hàng (`GET .../customer-vouchers`).
 * Map sang domain `SearchCustomerVouchersResult` qua [VoucherMapper].
 * Phiên bản v1.3+: flat Spring Page, không còn myVouchers/otherVouchers.
 */
@Serializable
data class SearchCustomerVouchersResponse(
    @SerialName("keyword") val keyword: String? = null,
    @SerialName("serviceCode") val serviceCode: String? = null,
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
data class VoucherTabInfo(
    @SerialName("code") val code: String,
    @SerialName("label") val label: String,
    @SerialName("labelI18n") val labelI18n: Map<String, String>? = null,
    @SerialName("default") val default: Boolean? = null,
    @SerialName("count") val count: Int? = null,
    @SerialName("order") val order: Int? = null,
)

@Serializable
data class PageableInfo(
    @SerialName("pageNumber") val pageNumber: Int? = null,
    @SerialName("pageSize") val pageSize: Int? = null,
    @SerialName("offset") val offset: Long? = null,
    @SerialName("paged") val paged: Boolean? = null,
    @SerialName("unpaged") val unpaged: Boolean? = null,
    @SerialName("sort") val sort: SortInfo? = null,
)

@Serializable
data class SortInfo(
    @SerialName("sorted") val sorted: Boolean? = null,
    @SerialName("unsorted") val unsorted: Boolean? = null,
    @SerialName("empty") val empty: Boolean? = null,
)

@Serializable
data class VoucherListItem(
    @SerialName("voucherId") val voucherId: String,
    @SerialName("merchantName") val merchantName: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("logo") val logo: String? = null,
    @SerialName("startDate") val startDate: String? = null,
    @SerialName("expirationDate") val expirationDate: String? = null,
    @SerialName("timeSlot") val timeSlot: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("displayStatusLabel") val displayStatusLabel: String? = null,
    @SerialName("campaignId") val campaignId: String? = null,
    @SerialName("campaignType") val campaignType: String? = null,
    @SerialName("isAutoApplied") val isAutoApplied: Boolean? = null,
    @SerialName("applicableProducts") val applicableProducts: List<ApplicableProductDto> = emptyList(),
)

@Serializable
data class ApplicableProductDto(
    @SerialName("productId") val productId: String,
    @SerialName("sku") val sku: String? = null,
    @SerialName("name") val name: String,
    @SerialName("image") val image: String? = null,
    @SerialName("type") val type: String,
)
