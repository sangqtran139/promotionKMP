package com.ttcn.promotionsdk.core.data.dto.voucher

import com.google.gson.annotations.SerializedName

data class ApiResponseTemplate<T>(
    @SerializedName("status") val status: Int? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("timestamp") val timestamp: String? = null,
    @SerializedName("metadata") val metadata: ResponseMetadata? = null,
    @SerializedName("data") val data: T? = null,
)

data class ResponseMetadata(
    @SerializedName("requestId") val requestId: String? = null,
    @SerializedName("partial") val partial: Boolean? = null,
)

data class SearchCustomerVouchersData(
    @SerializedName("mode") val mode: String? = null,
    @SerializedName("keyword") val keyword: String? = null,
    @SerializedName("serviceCode") val serviceCode: String? = null,
    @SerializedName("tabs") val tabs: List<VoucherTabInfo> = emptyList(),
    @SerializedName("defaultTab") val defaultTab: String? = null,
    @SerializedName("selectedTab") val selectedTab: String? = null,
    @SerializedName("myVouchers") val myVouchers: VoucherPage? = null,
    @SerializedName("otherVouchers") val otherVouchers: VoucherPage? = null,
)

data class VoucherTabInfo(
    @SerializedName("code") val code: String,
    @SerializedName("label") val label: String,
    @SerializedName("count") val count: Int? = null,
    @SerializedName("order") val order: Int? = null,
)

data class VoucherPage(
    @SerializedName("content") val content: List<VoucherListItem> = emptyList(),
    @SerializedName("pageable") val pageable: PageableInfo? = null,
    @SerializedName("totalElements") val totalElements: Long? = null,
    @SerializedName("totalPages") val totalPages: Int? = null,
    @SerializedName("first") val first: Boolean? = null,
    @SerializedName("last") val last: Boolean? = null,
    @SerializedName("numberOfElements") val numberOfElements: Int? = null,
    @SerializedName("empty") val empty: Boolean? = null,
    @SerializedName("number") val number: Int? = null,
    @SerializedName("size") val size: Int? = null,
    @SerializedName("sort") val sort: SortInfo? = null,
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
)

data class CustomerVoucherDetail(
    @SerializedName("voucherId") val voucherId: String,
    @SerializedName("customerId") val customerId: String? = null,
    @SerializedName("merchantName") val merchantName: String? = null,
    @SerializedName("logo") val logo: String? = null,
    @SerializedName("banner") val banner: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("guideline") val guideline: String? = null,
    @SerializedName("startDate") val startDate: String? = null,
    @SerializedName("expirationDate") val expirationDate: String? = null,
    @SerializedName("timeSlot") val timeSlot: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("displayStatusLabel") val displayStatusLabel: String? = null,
    @SerializedName("discountType") val discountType: DiscountType? = null,
    @SerializedName("conditions") val conditions: VoucherConditions? = null,
)

data class DiscountType(
    @SerializedName("discountType") val discountType: String? = null,
    @SerializedName("discountMethod") val discountMethod: String? = null,
    @SerializedName("discountValue") val discountValue: String? = null,
    @SerializedName("discountPercentage") val discountPercentage: String? = null,
    @SerializedName("includedProducts") val includedProducts: List<String> = emptyList(),
    @SerializedName("excludedProducts") val excludedProducts: List<String> = emptyList(),
)

data class VoucherConditions(
    @SerializedName("validationRules") val validationRules: List<ValidationRule> = emptyList(),
)

data class ValidationRule(
    @SerializedName("ruleCode") val ruleCode: String? = null,
    @SerializedName("description") val description: String? = null,
)
