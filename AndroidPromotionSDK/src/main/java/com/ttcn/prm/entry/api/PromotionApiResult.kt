package com.ttcn.prm.entry.api

/**
 * Kết quả của [PromotionSDKApi]. Đối ứng 1-1 với `PromotionApiResult.swift` bên iOS, nơi nó là
 * `typealias PromotionApiResult<T> = Result<T, PromotionSDKError>` — Swift đã có `Result` sẵn.
 *
 * API **không ném lỗi nghiệp vụ**: mọi thất bại về [Failure]. Chỉ `CancellationException` thoát ra,
 * để structured concurrency của host còn hoạt động.
 */
sealed interface PromotionApiResult<out T> {

    data class Success<out T>(val data: T) : PromotionApiResult<T>

    data class Failure(val error: PromotionSDKError) : PromotionApiResult<Nothing>
}

/**
 * Lỗi trả về từ SDK. Host `when` trên type này để xử lý từng loại.
 *
 * Bên iOS là `enum PromotionSDKError: Error, LocalizedError` với cùng 6 nhánh; [message] ở đây tương
 * ứng với `errorDescription` bên đó. Sửa một bên thì sửa cả hai.
 */
sealed class PromotionSDKError : Exception() {

    abstract override val message: String

    /** Lỗi từ server: [code] là HTTP status nếu có, [message] là mô tả của server. */
    data class NetworkFailure(
        val code: Int?,
        override val message: String,
    ) : PromotionSDKError()

    /** Token hết hạn — host refresh token rồi gọi lại. */
    data object SessionExpired : PromotionSDKError() {
        override val message: String = "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại."
    }

    data object Timeout : PromotionSDKError() {
        override val message: String = "Yêu cầu bị timeout, vui lòng thử lại."
    }

    /** Server trả `data: null` ở nơi bắt buộc phải có dữ liệu (chi tiết / validate / redemption). */
    data object ParseFailed : PromotionSDKError() {
        override val message: String = "Có lỗi xảy ra với dữ liệu trả về."
    }

    /** Tính năng đang TẮT qua feature flag. Tương ứng mã nghiệp vụ `PRM_MOB_021`. */
    data object FeatureDisabled : PromotionSDKError() {
        override val message: String = "Tính năng hiện đang tạm thời không khả dụng. Vui lòng thử lại sau."
    }

    data class Unknown(val error: Throwable) : PromotionSDKError() {
        override val message: String get() = error.message ?: "Đã có lỗi xảy ra."
    }

    /** HTTP status nếu có — chỉ với [NetworkFailure]. */
    val serverCode: Int? get() = (this as? NetworkFailure)?.code
}
