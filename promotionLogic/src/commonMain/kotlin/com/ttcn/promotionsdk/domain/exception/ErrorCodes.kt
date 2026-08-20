package com.ttcn.promotionsdk.domain.exception

/**
 * Mã lỗi công khai, dùng chung cho `AndroidPromotionSDK` và `iosPromotionUI`.
 *
 * `PromotionResult.Failure.errorCode` luôn là một trong các mã này, hoặc mã nghiệp vụ do server trả.
 * UI tra chuỗi hiển thị theo mã — **lõi không chứa chuỗi tiếng Việt**.
 */
object PromotionErrorCodes {
    const val MISSING_CUSTOMER_ID = "missing_customer_id"
    const val NO_RESULT = "no_result"
    const val INSUFFICIENT_BUDGET = "INSUFFICIENT_BUDGET"
    const val GENERAL = "error_general"
    const val NETWORK_ERROR = "network_error"
    const val TIMEOUT = "timeout"

    /** Tính năng đang TẮT qua feature flag. Trùng mã popup `PRM_MOB_021` của iOS. */
    const val FEATURE_DISABLED = "PRM_MOB_021"

    /** API trả HTTP 401 — token hết hạn/không hợp lệ. Tầng native map mã này ra `onExpireToken()`. */
    const val TOKEN_EXPIRED = "TOKEN_EXPIRED"
}

/**
 * Tên cũ, giữ để `AndroidPromotionSDK` và `iosPromotionUI` không phải sửa import khi bê UI sang.
 * Code mới nên dùng [PromotionErrorCodes].
 */
typealias ErrorCodes = PromotionErrorCodes
