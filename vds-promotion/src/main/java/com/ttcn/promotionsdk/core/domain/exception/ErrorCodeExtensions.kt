package com.ttcn.promotionsdk.core.domain.exception

/**
 * Trích mã lỗi nghiệp vụ từ throwable bất kỳ — dùng chung cho UI-mode ViewModel
 * (qua `onError`) lẫn facade headless.
 *
 * [PromotionException]/[NetworkException] → `errorCode`; throwable khác → `message`;
 * fallback [ErrorCodes.GENERAL].
 */
internal fun Throwable.toErrorCode(): String =
    when (this) {
        is PromotionException -> errorCode ?: ErrorCodes.GENERAL
        is NetworkException -> errorCode
        else -> message ?: ErrorCodes.GENERAL
    }
