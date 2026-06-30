package com.ttcn.promotionsdk.core.data.dto.voucher

import com.google.gson.annotations.SerializedName

/**
 * Payload trả về của API chi tiết voucher (`GET .../customer-vouchers/{voucherId}`).
 * Map sang domain `VoucherDetail` qua [VoucherMapper].
 * Phiên bản v1.1+: thêm campaignId, campaignType, campaignStatus, applicableProducts.
 */
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
    @SerializedName("campaignId") val campaignId: String? = null,
    @SerializedName("campaignType") val campaignType: String? = null,
    @SerializedName("campaignStatus") val campaignStatus: String? = null,
    @SerializedName("applicableProducts") val applicableProducts: List<ApplicableProductDto> = emptyList(),
    @SerializedName("discountType") val discountType: DiscountInfo? = null,
    @SerializedName("conditions") val conditions: VoucherConditions? = null,
)

data class DiscountInfo(
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
