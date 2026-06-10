package com.ttcn.promotionsdk.core.domain.model

data class VoucherSearchResult(
    val tabs: List<VoucherTabItem> = emptyList(),
    val defaultTab: String? = null,
    val selectedTab: String? = null,
    val myVouchers: VoucherListPage? = null,
    val otherVouchers: VoucherListPage? = null,
)

data class VoucherTabItem(
    val code: String,
    val label: String,
    val count: Int? = null,
    val order: Int? = null,
)

data class VoucherListPage(
    val content: List<VoucherItem> = emptyList(),
    val number: Int? = null,
    val size: Int? = null,
    val last: Boolean? = null,
    val totalElements: Long? = null,
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
    val campaignType: String? = null,
    val objectType: String = "CAMPAIGN",
)
