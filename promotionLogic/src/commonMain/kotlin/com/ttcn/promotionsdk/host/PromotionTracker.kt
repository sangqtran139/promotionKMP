package com.ttcn.promotionsdk.host

/**
 * Một sự kiện SDK bắn ra cho hệ thống tracking của host.
 *
 * [name] lấy từ [com.ttcn.promotionsdk.common.PromotionEvents] — **một nguồn duy nhất** cho cả hai
 * nền tảng. Đó là lý do tồn tại của kiểu này: nếu Fragment và ViewController mỗi bên tự gọi tracker
 * của mình thì tên event sẽ lệch, và BI không gộp được hai nền tảng vào cùng một phễu.
 *
 * [params] cố ý chỉ nhận `String`: map `Map<String, Any>` xuống Swift thành `[String: Any]` rồi
 * xuống Firebase/AppsFlyer thì mỗi hệ ép kiểu một khác. Số/boolean thì `toString()` ở chỗ bắn —
 * host tự parse nếu cần, nhưng chuỗi gửi đi giống nhau trên Android và iOS.
 *
 * **Không được mang PII** (token, số điện thoại, email, địa chỉ): tracker của host thường đẩy thẳng
 * ra bên thứ ba. Cùng lý do với quy tắc "không lưu token vào `PromotionPreferences`"
 * (`docs/common/StorageGuide.md` §5.3).
 */
public data class PromotionEvent(
    val name: String,
    val params: Map<String, String> = emptyMap(),
)

/**
 * Cổng tracking do **host cấp** — SDK bắn event, host quyết định đẩy đi đâu (Firebase, AppsFlyer,
 * hệ log nội bộ…). Truyền qua [PromotionHostServices.tracker].
 *
 * Đây là một **port**, không phải `expect/actual`. Tracker là thứ nằm sẵn ở tầng native của host và
 * mỗi app một khác, nên SDK không thể có `actual` cho nó: `actual` nằm trong SDK sẽ bắt
 * `:promotionLogic` phải biết Firebase, và chỉ cho đúng **một** hiện thực mỗi nền tảng. Xem
 * `docs/common/HostCapabilities.md` §1.
 *
 * Host không cài đặt → [NoOpPromotionTracker], SDK chạy y nguyên.
 *
 * **Hợp đồng — bắt buộc đọc:**
 * - Gọi từ **thread nền**, trong lúc store đang cập nhật state. Hiện thực phải **không chặn**:
 *   không I/O đồng bộ, không `runBlocking`, không nhảy sang main thread rồi chờ. Đẩy sang queue
 *   của hệ tracking rồi trả về ngay.
 * - **Không ném.** SDK đã bọc `runCatching` ở [com.ttcn.promotionsdk.common.PromotionAnalytics] nên
 *   một exception không làm hỏng màn hình, nhưng event đó mất.
 * - **Vòng đời:** SDK giữ object này tới tận `release()`. Trỏ vào một singleton cấp app, đừng trỏ
 *   vào Activity/Fragment/UIViewController — cùng cái bẫy đã ghi ở `PromotionTokenSource`.
 */
public interface PromotionTracker {
    public fun track(event: PromotionEvent)
}

/**
 * Mặc định khi host không cấp tracker. Đối ứng
 * [com.ttcn.promotionsdk.config.EmptyPromotionRequestContextProvider].
 */
public class NoOpPromotionTracker : PromotionTracker {
    override fun track(event: PromotionEvent) {
        // Không làm gì — host không quan tâm tracking.
    }
}
