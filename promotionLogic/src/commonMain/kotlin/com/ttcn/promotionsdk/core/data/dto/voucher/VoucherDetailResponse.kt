package com.ttcn.promotionsdk.core.data.dto.voucher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Payload trả về của API chi tiết voucher (`GET .../customer-vouchers/{voucherId}`).
 * Map sang domain `VoucherDetail` qua [VoucherMapper].
 * Phiên bản v1.1+: thêm campaignId, campaignType, campaignStatus, applicableProducts.
 */
@Serializable
data class CustomerVoucherDetail(
    @SerialName("voucherId") val voucherId: String,
    @SerialName("customerId") val customerId: String? = null,
    @SerialName("merchantName") val merchantName: String? = null,
    @SerialName("logo") val logo: String? = null,
    @SerialName("banner") val banner: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("guideline") val guideline: String? = null,
    @SerialName("startDate") val startDate: String? = null,
    @SerialName("expirationDate") val expirationDate: String? = null,
    @SerialName("timeSlot") val timeSlot: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("displayStatusLabel") val displayStatusLabel: String? = null,
    @SerialName("campaignId") val campaignId: String? = null,
    @SerialName("campaignType") val campaignType: String? = null,
    @SerialName("campaignStatus") val campaignStatus: String? = null,
    @SerialName("applicableProducts") val applicableProducts: List<ApplicableProductDto> = emptyList(),
    @SerialName("discountType") val discountType: DiscountInfo? = null,
    @SerialName("conditions") val conditions: VoucherConditions? = null,
)

@Serializable
data class DiscountInfo(
    @SerialName("discountType") val discountType: String? = null,
    @SerialName("discountMethod") val discountMethod: String? = null,
    @SerialName("discountValue") val discountValue: String? = null,
    @SerialName("discountPercentage") val discountPercentage: String? = null,
    @SerialName("includedProducts") val includedProducts: List<String> = emptyList(),
    @SerialName("excludedProducts") val excludedProducts: List<String> = emptyList(),
)

@Serializable
data class VoucherConditions(
    @SerialName("validationRules") val validationRules: List<ValidationRule> = emptyList(),
)

@Serializable
data class ValidationRule(
    @SerialName("ruleCode") val ruleCode: String? = null,
    @SerialName("description") val description: String? = null,
)
