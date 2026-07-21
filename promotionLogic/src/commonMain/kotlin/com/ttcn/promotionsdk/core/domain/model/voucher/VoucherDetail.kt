package com.ttcn.promotionsdk.core.domain.model.voucher

data class VoucherDetail(
    val voucherId: String,
    val merchantName: String? = null,
    val logo: String? = null,
    val banner: String? = null,
    val title: String? = null,
    val description: String? = null,
    val guideline: String? = null,
    val startDate: String? = null,
    val expirationDate: String? = null,
    val status: String? = null,
    val displayStatusLabel: String? = null,
    val campaignId: String? = null,
    val campaignType: String? = null,
    val campaignStatus: String? = null,
    val applicableProducts: List<ApplicableProduct> = emptyList(),
    /** Danh sách mã (codex) đã cấp cho khách. */
    val codes: List<String> = emptyList(),
    /** Link hướng dẫn sử dụng (metadata.usageGuideUrl). */
    val usageGuideUrl: String? = null,
)
