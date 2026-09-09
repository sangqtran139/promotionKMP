package com.ttcn.prm.entry.api

import com.ttcn.promotionsdk.domain.exception.PromotionErrorCodes

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
 * Bên iOS là `enum PromotionSDKError: Error, LocalizedError` với cùng 8 nhánh; [message] ở đây tương
 * ứng với `errorDescription` bên đó. Sửa một bên thì sửa cả hai.
 *
 * ⚠️ **Thêm nhánh là breaking change.** Host `when` trên type này; Kotlin đòi `when` trên `sealed`
 * phải liệt kê đủ, nên mỗi nhánh mới làm hỏng biên dịch của host (bên iOS enum không `@frozen` cũng
 * vậy). Chốt xong danh sách trước go-live; sau đó thêm nhánh = major bump.
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
        override val message: String = "Tính năng ưu đãi hiện đang tạm thời không khả dụng. Vui lòng thử lại sau."
    }

    /**
     * **Lỗi nghiệp vụ của server**, không phải lỗi mạng: [code] là mã server trả (vd
     * `VOUCHER_EXPIRED`), [message] là câu server soạn cho người dùng — `null` khi server không kèm.
     *
     * Trước đây nhánh này bị nhét vào [NetworkFailure] với `message = errorCode`: host nhận một "lỗi
     * mạng" mà thật ra là rule nghiệp vụ, và **mã lỗi thô nằm đúng chỗ đáng lẽ là câu hiển thị cho
     * người dùng** — hiện thẳng lên UI là ra chữ `VOUCHER_EXPIRED`.
     */
    data class BusinessRule(
        val code: String,
        val serverMessage: String?,
    ) : PromotionSDKError() {
        override val message: String get() = serverMessage ?: "Đã có lỗi xảy ra."
    }

    /**
     * Gọi API khi chưa [PromotionSDK.initialize]. Trước đây bên iOS ca này là `preconditionFailure`
     * — SDK làm **crash app của host** vì lỗi thứ tự khởi tạo của host.
     */
    data object NotInitialized : PromotionSDKError() {
        override val message: String = "PromotionSDK chưa được khởi tạo."
    }

    data class Unknown(val error: Throwable) : PromotionSDKError() {
        override val message: String get() = error.message ?: "Đã có lỗi xảy ra."
    }

    /** HTTP status nếu có — chỉ với [NetworkFailure]. */
    val serverCode: Int? get() = (this as? NetworkFailure)?.code

    companion object {
        /**
         * Mã lỗi **thô** (chuỗi) → kiểu lỗi công khai, để host bắt **tường minh** thay vì so chuỗi.
         *
         * Cần hàm này vì SDK có hai bề mặt lỗi không giống nhau:
         * - Headless [PromotionSDKApi] đã trả sẵn [PromotionSDKError] — bắt bằng `is` là xong.
         * - Callback của widget ([com.ttcn.prm.ui.feature.endowview.PRMEndowView.onError],
         *   [PromotionSDK.confirmRedemption]) chỉ trả `String`, mà hằng số mã lỗi nằm trong
         *   `promotionLogic` — module khai `implementation` nên **không có** trên compile classpath
         *   của host. Host không tham chiếu được hằng số, đành hardcode `"PRM_MOB_021"`.
         *
         * Với hàm này host viết được:
         * ```kotlin
         * endowView.onError = { code ->
         *     when (PromotionSDKError.from(code)) {
         *         is PromotionSDKError.FeatureDisabled -> stopCheckout()  // SDK đã tự hiện popup
         *         else -> showMyOwnError()
         *     }
         * }
         * ```
         */
        /**
         * [serverMessage] / [httpStatus]: phần thông tin chỉ có ở bề mặt headless
         * ([PromotionSDKApi] cầm cả `PromotionResult.Failure`). Callback của widget chỉ có mã lỗi
         * nên gọi hàm này với một tham số.
         *
         * **Đây là đường map DUY NHẤT.** Trước đây [PromotionSDKApi] có bản map thứ hai của riêng
         * nó: cùng một lỗi ra hai kết quả khác nhau tuỳ host đi vào đường nào — bản này trả
         * `message = ""`, bản kia trả `failure.message` và cả `httpStatus`.
         */
        @JvmStatic
        @JvmOverloads
        fun from(
            errorCode: String,
            serverMessage: String? = null,
            httpStatus: Int? = null,
        ): PromotionSDKError = when (errorCode) {
            PromotionErrorCodes.FEATURE_DISABLED -> FeatureDisabled
            PromotionErrorCodes.TIMEOUT -> Timeout
            PromotionErrorCodes.NO_RESULT -> ParseFailed
            // Lùi về câu tiếng Việt của SDK khi server không nói gì: đây là case HAY XẢY RA NHẤT, và
            // trước đây nó là case DUY NHẤT trả `message = ""` — host hiện `error.message` thì ra
            // popup trắng không chữ. Chuỗi trùng `R.string.prm_error_network` để hai bề mặt (headless
            // và UI của SDK) không nói hai câu khác nhau cho cùng một sự cố.
            PromotionErrorCodes.NETWORK_ERROR -> NetworkFailure(
                code = httpStatus,
                message = serverMessage?.takeIf { it.isNotBlank() } ?: NETWORK_ERROR_MESSAGE,
            )
            // Mã nghiệp vụ của server (vd VOUCHER_EXPIRED) — KHÔNG phải lỗi mạng.
            else -> BusinessRule(code = errorCode, serverMessage = serverMessage?.takeIf { it.isNotBlank() })
        }

        /** Trùng `R.string.prm_error_network`; để ở đây vì tầng entry không có `Context`. */
        private const val NETWORK_ERROR_MESSAGE =
            "Không có kết nối mạng. Vui lòng kiểm tra rồi thử lại"
    }
}
