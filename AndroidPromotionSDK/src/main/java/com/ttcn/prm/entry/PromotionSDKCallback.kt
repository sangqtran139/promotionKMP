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
 */
interface PromotionSDKCallback {

    /** Gọi khi user chọn và bấm "Áp dụng" ưu đãi thành công. */
    fun onVoucherApplied(voucherId: String) {}

    /** Gọi khi user chọn 1 dịch vụ trong bottom sheet "Chọn dịch vụ" (Ưu đãi của tôi / Tìm kiếm / Chi tiết). */
    fun onServiceSelected(selection: PromotionServiceSelection) {}

    /**
     * Gọi khi 1 API bên trong màn hình SDK (Ưu đãi của tôi, Tìm kiếm, Chi tiết, Chọn ưu đãi, widget
     * Endow) trả về HTTP 401 và **không cứu được** — tức
     * [PromotionTokenSource.refreshToken] đã báo `false`, hoặc host không cài đặt nó.
     *
     * Nghĩa là phiên đã chết thật: host nên điều hướng user về màn đăng nhập. Host **không** cần
     * đẩy token mới vào SDK — SDK tự đọc lại qua [PromotionTokenSource.currentToken] ở mỗi request.
     */
    fun onExpireToken() {}

}
