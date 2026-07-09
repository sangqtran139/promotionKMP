package com.ttcn.promotionsdk.core.domain.model.voucher

data class SearchCustomerVouchersRequest(
    val customerId: String,
    val keyword: String? = null,
    val serviceCode: String? = null,
    val tab: String? = null,
    val page: Int? = null,
    val size: Int? = null,
)
