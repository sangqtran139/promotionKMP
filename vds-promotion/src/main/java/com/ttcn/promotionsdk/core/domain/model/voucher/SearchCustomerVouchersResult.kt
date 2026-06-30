package com.ttcn.promotionsdk.core.domain.model.voucher

data class SearchCustomerVouchersResult(
    val keyword: String? = null,
    val serviceCode: String? = null,
    val tabs: List<VoucherTabItem> = emptyList(),
    val defaultTab: String? = null,
    val selectedTab: String? = null,
    val content: List<VoucherItem> = emptyList(),
    val number: Int? = null,
    val size: Int? = null,
    val last: Boolean? = null,
    val totalElements: Long? = null,
)

data class VoucherTabItem(
    val code: String,
    val label: String,
    val count: Int? = null,
    val order: Int? = null,
    val isDefault: Boolean = false,
)

data class VoucherItem(
    val voucherId: String,
    val merchantName: String? = null,
    val title: String? = null,
    val description: String? = null,
    val logo: String? = null,
    val expirationDate: String? = null,
    val status: String? = null,
    val displayStatusLabel: String? = null,
    val campaignId: String? = null,
    val campaignType: String? = null,
    val objectType: String = "CAMPAIGN",
    val applicableProducts: List<ApplicableProduct> = emptyList(),
)
