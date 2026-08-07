package com.ttcn.promotionsdk.domain.exception

class PromotionException(
    val errorCode: String?,
    override val message: String?,
    val httpStatus: Int? = null,
) : RuntimeException(message)
