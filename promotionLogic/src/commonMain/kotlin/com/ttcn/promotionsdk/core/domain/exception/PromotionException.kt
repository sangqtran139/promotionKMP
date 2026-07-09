package com.ttcn.promotionsdk.core.domain.exception

class PromotionException(
    val errorCode: String?,
    override val message: String?,
    val httpStatus: Int? = null,
) : RuntimeException(message)
