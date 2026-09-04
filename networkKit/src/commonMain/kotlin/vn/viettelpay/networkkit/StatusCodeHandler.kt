package vn.viettelpay.networkkit

/**
 * Xử lý một tập business status code cụ thể — tổng quát hoá `VDOResponseStatusCodeHandler` của
 * network-kit-android, nhưng không hardcode field/kiểu envelope nào.
 *
 * [onMatch] tự quyết định phản ứng — ném exception (dừng hẳn, lỗi domain của consumer) hoặc return
 * bình thường (coi như đã xử lý xong, không có gì thêm để làm). `suspend` để handler tự gọi được
 * side-effect bất đồng bộ (vd refresh token) trước khi quyết định.
 */
public class StatusCodeHandler private constructor(
    public val codes: Set<String>,
    private val onMatch: suspend (BusinessStatus) -> Unit,
) {
    public companion object {
        /**
         * Ánh xạ mặc định: code khớp → ném [BusinessError] mang đúng code/message của server. Đủ cho
         * phần lớn trường hợp — xem [BusinessError].
         */
        public fun of(vararg codes: String): StatusCodeHandler =
            StatusCodeHandler(codes.toSet()) { status -> throw BusinessError(status.code, status.message) }

        /** Ánh xạ tuỳ biến — dùng khi cần logic khác throw đơn thuần (refresh token, log, …). */
        public fun of(codes: Set<String>, onMatch: suspend (BusinessStatus) -> Unit): StatusCodeHandler =
            StatusCodeHandler(codes, onMatch)
    }

    /** `true` nếu `status.code` khớp — đã chạy [onMatch] (có thể đã throw). */
    internal suspend fun handle(status: BusinessStatus): Boolean {
        if (status.code !in codes) return false
        onMatch(status)
        return true
    }
}

/**
 * Chuỗi handler chạy tuần tự — handler đầu tiên có code khớp sẽ chạy, các handler sau không được xét
 * nữa (giống `VDOStatusCodeHandlerContainer`, nhưng không có state toàn cục nào để rò rỉ giữa các
 * chain khác nhau — xem phát hiện #2 ở §1 SharedNetworkKit.md).
 */
public class StatusCodeHandlerChain(private val handlers: List<StatusCodeHandler>) {
    public constructor(vararg handlers: StatusCodeHandler) : this(handlers.toList())

    /** Không handler nào khớp thì không làm gì — coi là thành công bình thường. */
    public suspend fun run(status: BusinessStatus) {
        for (handler in handlers) {
            if (handler.handle(status)) return
        }
    }
}

/**
 * Chạy [chain] lên `this` qua [toBusinessStatus] (cách đọc code/message từ envelope riêng của
 * consumer). Gọi sau [getJson]/[postJson], ở bất kỳ đâu có response trong tay — `chain` không cần
 * khai lúc tạo client, consumer tự dựng và gọi khi cần.
 */
public suspend fun <T> T.checkBusinessStatus(
    chain: StatusCodeHandlerChain,
    toBusinessStatus: (T) -> BusinessStatus?,
): T {
    toBusinessStatus(this)?.let { chain.run(it) }
    return this
}
