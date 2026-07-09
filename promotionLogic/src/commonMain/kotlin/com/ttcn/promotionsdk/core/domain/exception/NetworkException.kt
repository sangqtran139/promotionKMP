package com.ttcn.promotionsdk.core.domain.exception

/**
 * Lỗi tầng kết nối/transport (mất mạng, timeout, không phân giải host…) — phân biệt với
 * [PromotionException] (lỗi nghiệp vụ/HTTP có error code từ server).
 * [errorCode] dùng [ErrorCodes.NETWORK_ERROR] hoặc [ErrorCodes.TIMEOUT].
 */
class NetworkException(
    val errorCode: String,
    override val message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
