package com.ttcn.promotionsdk.domain.exception

/**
 * Trích mã lỗi nghiệp vụ từ throwable bất kỳ — dùng chung cho UI-mode ViewModel
 * (qua `onError`) lẫn facade headless.
 *
 * Cần cho UI native khi gọi thẳng use case đơn lẻ: use case ném [PromotionException] /
 * [NetworkException], còn `PromotionUseCases` thì bọc sẵn thành `PromotionResult`.
 *
 * HTTP 401 → [PromotionErrorCodes.TOKEN_EXPIRED] (bất kể server trả `errorCode` gì) — tầng
 * native dựa vào mã này để bắn `onExpireToken()`. 403 là **không đủ quyền** (token vẫn hợp lệ),
 * khác nghĩa với hết hạn nên đi theo nhánh `errorCode`/`GENERAL` phía dưới, không bắn
 * `onExpireToken()`. Còn lại: [PromotionException]/[NetworkException] → `errorCode`; throwable
 * khác → `message`; fallback [PromotionErrorCodes.GENERAL].
 */
public fun Throwable.toErrorCode(): String =
    when {
        this is PromotionException && httpStatus == 401 -> PromotionErrorCodes.TOKEN_EXPIRED
        this is PromotionException -> errorCode ?: PromotionErrorCodes.GENERAL
        this is NetworkException -> errorCode
        else -> message ?: PromotionErrorCodes.GENERAL
    }
