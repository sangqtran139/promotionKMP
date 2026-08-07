package com.ttcn.promotionsdk.domain.exception

/**
 * Trích mã lỗi nghiệp vụ từ throwable bất kỳ — dùng chung cho UI-mode ViewModel
 * (qua `onError`) lẫn facade headless.
 *
 * Cần cho UI native khi gọi thẳng use case đơn lẻ: use case ném [PromotionException] /
 * [NetworkException], còn `PromotionUseCases` thì bọc sẵn thành `PromotionResult`.
 *
 * [PromotionException]/[NetworkException] → `errorCode`; throwable khác → `message`;
 * fallback [PromotionErrorCodes.GENERAL].
 */
fun Throwable.toErrorCode(): String =
    when (this) {
        is PromotionException -> errorCode ?: PromotionErrorCodes.GENERAL
        is NetworkException -> errorCode
        else -> message ?: PromotionErrorCodes.GENERAL
    }
