package com.ttcn.promotionsdk.core.data.dto.voucher

import com.google.gson.annotations.SerializedName

/**
 * Payload trả về của API tìm kiếm voucher khách hàng (`GET .../customer-vouchers`).
 * Map sang domain `SearchCustomerVouchersResult` qua [VoucherMapper].
 * Phiên bản v1.3+: flat Spring Page, không còn myVouchers/otherVouchers.
 */
data class SearchCustomerVouchersResponse(
    @SerializedName("keyword") val keyword: String? = null,
    @SerializedName("serviceCode") val serviceCode: String? = null,
    @SerializedName("tabs") val tabs: List<VoucherTabInfo> = emptyList(),
    @SerializedName("defaultTab") val defaultTab: String? = null,
    @SerializedName("selectedTab") val selectedTab: String? = null,
    @SerializedName("content") val content: List<VoucherListItem> = emptyList(),
    @SerializedName("pageable") val pageable: PageableInfo? = null,
    @SerializedName("totalElements") val totalElements: Long? = null,
    @SerializedName("totalPages") val totalPages: Int? = null,
    @SerializedName("first") val first: Boolean? = null,
    @SerializedName("last") val last: Boolean? = null,
    @SerializedName("number") val number: Int? = null,
    @SerializedName("size") val size: Int? = null,
    @SerializedName("numberOfElements") val numberOfElements: Int? = null,
    @SerializedName("empty") val empty: Boolean? = null,
    @SerializedName("sort") val sort: SortInfo? = null,
)

data class VoucherTabInfo(
    @SerializedName("code") val code: String,
    @SerializedName("label") val label: String,
    @SerializedName("labelI18n") val labelI18n: Map<String, String>? = null,
    @SerializedName("default") val default: Boolean? = null,
    @SerializedName("count") val count: Int? = null,
    @SerializedName("order") val order: Int? = null,
)

data class PageableInfo(
    @SerializedName("pageNumber") val pageNumber: Int? = null,
    @SerializedName("pageSize") val pageSize: Int? = null,
    @SerializedName("offset") val offset: Long? = null,
    @SerializedName("paged") val paged: Boolean? = null,
    @SerializedName("unpaged") val unpaged: Boolean? = null,
    @SerializedName("sort") val sort: SortInfo? = null,
)

data class SortInfo(
    @SerializedName("sorted") val sorted: Boolean? = null,
    @SerializedName("unsorted") val unsorted: Boolean? = null,
    @SerializedName("empty") val empty: Boolean? = null,
)

data class VoucherListItem(
    @SerializedName("voucherId") val voucherId: String,
    @SerializedName("merchantName") val merchantName: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("logo") val logo: String? = null,
    @SerializedName("startDate") val startDate: String? = null,
    @SerializedName("expirationDate") val expirationDate: String? = null,
    @SerializedName("timeSlot") val timeSlot: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("displayStatusLabel") val displayStatusLabel: String? = null,
    @SerializedName("campaignId") val campaignId: String? = null,
    @SerializedName("campaignType") val campaignType: String? = null,
    @SerializedName("applicableProducts") val applicableProducts: List<ApplicableProductDto> = emptyList(),
)

data class ApplicableProductDto(
    @SerializedName("productId") val productId: String,
    @SerializedName("sku") val sku: String? = null,
    @SerializedName("name") val name: String,
    @SerializedName("image") val image: String? = null,
    @SerializedName("type") val type: String,
)
