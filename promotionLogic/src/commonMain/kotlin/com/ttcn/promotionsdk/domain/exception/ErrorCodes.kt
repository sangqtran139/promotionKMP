package com.ttcn.promotionsdk.domain.exception

/**
 * Mã lỗi công khai, dùng chung cho `AndroidPromotionSDK` và `iosPromotionUI`.
 *
 * `PromotionResult.Failure.errorCode` luôn là một trong các mã này, hoặc mã nghiệp vụ do server trả.
 * UI tra chuỗi hiển thị theo mã — **lõi không chứa chuỗi tiếng Việt**.
 */
public object PromotionErrorCodes {
    public const val MISSING_CUSTOMER_ID: String = "missing_customer_id"
    public const val NO_RESULT: String = "no_result"
    public const val INSUFFICIENT_BUDGET: String = "INSUFFICIENT_BUDGET"
    public const val GENERAL: String = "error_general"
    public const val NETWORK_ERROR: String = "network_error"
    public const val TIMEOUT: String = "timeout"

    /** Tính năng đang TẮT qua feature flag. Trùng mã popup `PRM_MOB_021` của iOS. */
    public const val FEATURE_DISABLED: String = "PRM_MOB_021"

    /** API trả HTTP 401 — token hết hạn/không hợp lệ. Tầng native map mã này ra `onExpireToken()`. */
    public const val TOKEN_EXPIRED: String = "TOKEN_EXPIRED"
}

/**
 * Tên cũ, giữ để `AndroidPromotionSDK` và `iosPromotionUI` không phải sửa import khi bê UI sang.
 * Code mới nên dùng [PromotionErrorCodes].
 */
public typealias ErrorCodes = PromotionErrorCodes
