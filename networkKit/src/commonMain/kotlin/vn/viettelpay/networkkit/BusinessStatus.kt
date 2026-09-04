package vn.viettelpay.networkkit

/**
 * Hợp đồng cấu trúc tối thiểu để [StatusCodeHandlerChain] đọc "code nghiệp vụ" từ response — mỗi
 * consumer tự ánh xạ envelope riêng của họ (`ApiResponseTemplate<T>` của Promotion,
 * `VDOBaseResponse`-style của SDK khác…) sang đây bằng một hàm nhỏ (`toBusinessStatus`), module
 * không cần biết field nào của DTO nào. Xem ranh giới "cơ chế, không phải chính sách" ở
 * SharedNetworkKit.md §2.
 */
public interface BusinessStatus {
    public val code: String?
    public val message: String?
}

/**
 * Lỗi nghiệp vụ — response HTTP thành công (2xx) nhưng envelope mang code lỗi. Khác [NetworkError]:
 * đó là lỗi *transport*, đây là lỗi *nội dung*. Đủ dùng cho trường hợp phổ biến "khớp code, ném lỗi
 * kèm code+message của server" — chính là phần dùng chung đang bị chép tay giữa
 * `EkycStatusCodeHandler`/`ApiStatusHandler` của `network-kit-android` (khác nhau đúng một dòng, xem
 * SharedNetworkKit.md §1). Cần hành vi khác thì tự cung cấp `onMatch` riêng khi đăng ký
 * [StatusCodeHandler].
 */
public class BusinessError(
    public val code: String?,
    message: String?,
) : RuntimeException(message)
