package com.ttcn.promotionsdk.domain.exception

public class PromotionException(
    public val errorCode: String?,
    override val message: String?,
    public val httpStatus: Int? = null,
) : RuntimeException(message)
