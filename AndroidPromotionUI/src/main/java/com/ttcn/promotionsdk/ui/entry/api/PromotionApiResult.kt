package com.ttcn.promotionsdk.ui.entry.api

/**
 * Kết quả của [PromotionSDKApi]. Đối ứng `Result<T, PromotionSDKError>` bên Swift.
 *
 * API **không ném lỗi nghiệp vụ**: mọi thất bại về [Failure]. Chỉ `CancellationException` thoát ra,
 * để structured concurrency của host còn hoạt động.
 */
sealed interface PromotionApiResult<out T> {

    data class Success<out T>(val data: T) : PromotionApiResult<T>

    data class Failure(val error: PromotionSDKError) : PromotionApiResult<Nothing>
}

/** Lỗi trả về từ SDK. Host `when` trên type này để xử lý từng loại. */
sealed class PromotionSDKError(message: String) : Exception(message) {

    /** Lỗi từ server: [code] là HTTP status nếu có, [serverMessage] là mô tả của server. */
    data class NetworkFailure(
        val code: Int?,
        val serverMessage: String,
    ) : PromotionSDKError(serverMessage)

    /** Token hết hạn — host refresh token rồi gọi lại. */
    data object SessionExpired :
        PromotionSDKError("Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.")

    data object Timeout : PromotionSDKError("Yêu cầu bị timeout, vui lòng thử lại.")

    /** Server trả `data: null` ở nơi bắt buộc phải có dữ liệu (chi tiết / validate / redemption). */
    data object ParseFailed : PromotionSDKError("Có lỗi xảy ra với dữ liệu trả về.")

    /** Tính năng đang TẮT qua feature flag. Tương ứng mã nghiệp vụ `PRM_MOB_021`. */
    data object FeatureDisabled :
        PromotionSDKError("Tính năng hiện đang tạm thời không khả dụng. Vui lòng thử lại sau.")

    data class Unknown(val throwable: Throwable) :
        PromotionSDKError(throwable.message ?: "Đã có lỗi xảy ra.")

    /** HTTP status nếu có — chỉ với [NetworkFailure]. */
    val serverCode: Int? get() = (this as? NetworkFailure)?.code
}
