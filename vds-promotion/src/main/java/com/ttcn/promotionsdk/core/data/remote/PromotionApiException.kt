package com.ttcn.promotionsdk.core.data.remote

class PromotionApiException(
    val errorCode: String?,
    override val message: String?,
    val status: Int? = null,
) : RuntimeException(message)
