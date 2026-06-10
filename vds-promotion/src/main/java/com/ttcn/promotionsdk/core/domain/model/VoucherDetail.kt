package com.ttcn.promotionsdk.core.domain.model

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
)
