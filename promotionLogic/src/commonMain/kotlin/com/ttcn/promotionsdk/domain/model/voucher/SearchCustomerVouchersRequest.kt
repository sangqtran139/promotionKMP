package com.ttcn.promotionsdk.domain.model.voucher

public data class SearchCustomerVouchersRequest(
    val keyword: String? = null,
    val serviceCode: String? = null,
    val tab: String? = null,
    val page: Int? = null,
    val size: Int? = null,
)
