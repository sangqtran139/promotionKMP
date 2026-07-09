package com.ttcn.promotionsdk.core.domain.model.voucher

data class ApplicableProduct(
    val productId: String,
    val sku: String? = null,
    val name: String,
    val image: String? = null,
    val type: String,
)
