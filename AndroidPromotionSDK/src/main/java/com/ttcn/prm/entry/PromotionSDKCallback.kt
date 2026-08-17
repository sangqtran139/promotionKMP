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

    /** Gọi khi user bấm "Hủy" để bỏ chọn ưu đãi trên widget. */
    fun onVoucherCleared() {}

    /** Gọi khi widget load xong và biết tổng số voucher khả dụng. */
    fun onVoucherCountChanged(count: Int) {}

    /** Gọi khi user chọn 1 dịch vụ trong bottom sheet "Chọn dịch vụ" (Ưu đãi của tôi / Tìm kiếm / Chi tiết). */
    fun onServiceSelected(selection: PromotionServiceSelection) {}

    /**
     * Gọi khi biết chắc trạng thái bật/tắt SDK qua feature flag (Unleash).
     * `enabled == false` → host nên ẩn toàn bộ điểm vào ưu đãi (entry point, widget).
     */
    fun onAvailabilityChanged(enabled: Boolean) {}

    /** Gọi khi màn hình SDK được đóng (user back hoặc SDK release). */
    fun onClosed() {}
}
