package vn.viettelpay.networkkit

/**
 * Lỗi transport đã phân loại — tổng quát hoá `apiCall {}` của `PromotionRemoteDataSource`, nhưng
 * **không** mang error code hay exception domain nào (đó là việc của consumer: map [NetworkError]
 * sang exception nghiệp vụ riêng của họ, vd `PromotionException`/`ErrorCodes`). Xem ranh giới
 * "cơ chế, không phải chính sách" ở SharedNetworkKit.md §2. Dựng qua [networkCall].
 */
public sealed class NetworkError(
    message: String?,
    cause: Throwable?,
) : RuntimeException(message, cause) {

    public class Timeout(message: String?, cause: Throwable?) : NetworkError(message, cause)

    public class NoConnection(message: String?, cause: Throwable?) : NetworkError(message, cause)

    /** HTTP trả về ngoài khoảng 2xx. [rawBody] để consumer tự bóc error code/message của server. */
    public class Http(
        public val status: Int,
        public val rawBody: String?,
        cause: Throwable?,
    ) : NetworkError("HTTP $status", cause)

    /** Response 2xx nhưng JSON lệch schema DTO consumer khai (thiếu field bắt buộc, sai kiểu…). */
    public class Serialization(cause: Throwable?) : NetworkError(cause?.message, cause)

    public class Unknown(cause: Throwable?) : NetworkError(cause?.message, cause)
}
