package com.ttcn.prm.entry

/**
 * Dữ liệu trả về host khi user chọn 1 dịch vụ trong bottom sheet "Chọn dịch vụ".
 *
 * [productId] khớp `productId` host cấu hình / `productId` của voucher.
 * Đối ứng `PromotionServiceSelection` bên iOS (nằm cùng file callback — xem docs/InitParity.md §3).
 */
data class PromotionServiceSelection(
    val voucherId: String,
    val productId: String,
    val productName: String,
    val skuSourceId: String = "",
    val iconUrl: String,
)

/**
 * Callback sự kiện SDK — set qua [PromotionSDKOptions.callback].
 *
 * Tên method / tham số **trùng chữ** với `PromotionSDKCallback` bên iOS (xem docs/InitParity.md §3).
 * SDK là singleton nên **không** truyền `sdk` vào method. Mọi method có default rỗng → host chỉ
 * override cái cần.
 *
 * **Hợp đồng — bắt buộc đọc.** Giống [PromotionTokenSource] và [PromotionTracker], đây là một object
 * của host mà SDK giữ; ba mục dưới đây là cùng một hợp đồng, phát biểu cho cả ba cổng:
 *
 * 1. **Thread: luôn là main thread.** Cả ba sự kiện đều phát ra từ tầng UI của SDK (Fragment,
 *    [PRMOfferWidget], `PRMStoreViewModel.effects`), nên host **được phép** đụng View / điều hướng
 *    thẳng trong thân method, không cần `post`/`runOnUiThread`. Đối ứng iOS: bề mặt public bên đó là
 *    `@MainActor` nên cũng đúng main. Đây là **bảo đảm**, không phải tình cờ — đổi chỗ bắn sang
 *    thread nền là breaking change và phải ghi vào `CHANGELOG.md`.
 * 2. **Vòng đời — bẫy dễ dính nhất.** [PromotionSDK] là singleton `object`; nó giữ **strong
 *    reference** tới object này từ [PromotionSDK.initialize] đến [PromotionSDK.release]. Cho một
 *    `Fragment`/`Activity` implement interface này rồi truyền `this` là bắt luôn màn hình đó vào
 *    SDK → **rò rỉ vĩnh viễn**, và sau khi màn hình chết thì callback vẫn chạy trên một View đã
 *    detach. Trỏ vào một singleton **cấp app** (`object`, repository, DI singleton) rồi từ đó phát
 *    tiếp đi đâu cũng được — xem `DemoPromotionCallback` ở app demo.
 *
 *    SDK **cố ý giữ strong, không giữ weak**: weak thì host truyền một object cục bộ là callback
 *    lặng lẽ chết ngay lần GC đầu, không lỗi không log — hỏng theo kiểu khó lần hơn hẳn rò rỉ.
 * 3. **Sau [PromotionSDK.release] thì không còn sự kiện nào.** `release()` xoá tham chiếu này, nên
 *    host không phải tự huỷ đăng ký. Đổi callback = gọi lại [PromotionSDK.initialize].
 *
 * Ném exception ra khỏi các method này thì **SDK không cứu** — nó nổi lên đúng chỗ bắn, tức trong
 * luồng UI của màn SDK. Khác [PromotionTracker] (tracking là phụ trợ nên lõi nuốt + log): những sự
 * kiện ở đây là nghiệp vụ của host, nuốt đi thì host tưởng đã xử lý xong.
 */
interface PromotionSDKCallback {

    /** Gọi khi user chọn và bấm "Áp dụng" ưu đãi thành công. */
    fun onVoucherApplied(voucherId: String) {}

    /** Gọi khi user chọn 1 dịch vụ trong bottom sheet "Chọn dịch vụ" (Ưu đãi của tôi / Tìm kiếm / Chi tiết). */
    fun onServiceSelected(selection: PromotionServiceSelection) {}

    /**
     * Gọi khi 1 API bên trong màn hình SDK (Ưu đãi của tôi, Tìm kiếm, Chi tiết, Chọn ưu đãi,
     * widget ưu đãi) trả về HTTP 401 và **không cứu được** — tức
     * [PromotionTokenSource.refreshToken] đã báo `false`, hoặc host không cài đặt nó.
     *
     * Nghĩa là phiên đã chết thật: host nên điều hướng user về màn đăng nhập. Host **không** cần
     * đẩy token mới vào SDK — SDK tự đọc lại qua [PromotionTokenSource.currentToken] ở mỗi request.
     */
    fun onExpireToken() {}

}
