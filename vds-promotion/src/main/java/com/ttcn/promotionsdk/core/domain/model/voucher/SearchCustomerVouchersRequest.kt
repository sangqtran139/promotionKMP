package com.ttcn.promotionsdk.core.domain.model.voucher

data class SearchCustomerVouchersRequest(
    val customerId: String,
    val keyword: String? = null,
    val serviceCode: String? = null,
    val sectionCode: String? = null,
    val tab: String? = null,
    val myVouchersPage: Int? = null,
    val myVouchersSize: Int? = null,
    val otherVouchersPage: Int? = null,
    val otherVouchersSize: Int? = null,
)