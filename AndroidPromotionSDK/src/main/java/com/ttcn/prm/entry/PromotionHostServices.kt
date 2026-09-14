package com.ttcn.prm.entry

import com.ttcn.promotionsdk.data.local.PromotionPreferences as CorePromotionPreferences
import com.ttcn.promotionsdk.host.PromotionEvent as CorePromotionEvent
import com.ttcn.promotionsdk.host.PromotionHostServices as CorePromotionHostServices
import com.ttcn.promotionsdk.host.PromotionTracker as CorePromotionTracker

/**
 * **Gói năng lực do host cấp** — một chỗ duy nhất cho mọi thứ SDK không tự làm được vì nó nằm ở tầng
 * native của app: tracking, kho dữ liệu, và những cổng sẽ thêm về sau.
 * Đối ứng `PromotionHostServices` bên iOS (cùng field, cùng thứ tự).
 *
 * Đây là *cái gói*, không phải một tính năng. Nếu mỗi năng lực là một tham số riêng của
 * [PromotionSDKOptions] và [PromotionSDK.initialize] thì thêm năng lực thứ ba là sửa sáu chữ ký
 * public trên hai nền tảng. Với gói này, thêm năng lực = thêm **một field**.
 *
 * Mọi field đều **tuỳ chọn** — host bật đúng thứ mình cần:
 *
 * ```kotlin
 * PromotionSDK.initialize(
 *     context, tokenSource = AppTokenSource, baseUrl = BASE_URL,
 *     hostServices = PromotionHostServices(tracker = AppPromotionTracker),
 * )
 * ```
 *
 * **Không thuộc gói này:** [PromotionTokenSource]. Nó cũng là cổng do host cấp, nhưng đứng ở
 * [PromotionSessionConfig] vì token là **thông tin phiên** — gắn với lần đăng nhập, đổi theo user.
 * Gói này là **năng lực hạ tầng**, gắn với vòng đời app.
 *
 * Xem `docs/common/HostCapabilities.md`.
 */
data class PromotionHostServices(
    /** Nơi nhận event của SDK. `null` → SDK không bắn đi đâu. Xem [PromotionTracker]. */
    val tracker: PromotionTracker? = null,
    /** Kho khoá–giá trị của app thay cho kho mặc định của SDK. `null` → SDK tự lo. Xem [PromotionStorage]. */
    val storage: PromotionStorage? = null,
)

// ─── Tracking ───────────────────────────────────────────────────────────────

/**
 * Một sự kiện SDK bắn ra cho hệ thống tracking của host (Firebase, AppsFlyer, hệ log nội bộ…).
 *
 * [name] do SDK đặt và **giống hệt bên iOS** — tên event sinh ở tầng logic dùng chung, không phải ở
 * Fragment, nên phễu của hai nền tảng gộp được. Danh sách event xem `docs/common/HostCapabilities.md`.
 *
 * [params] chỉ chứa `String`. Số/boolean đã được SDK `toString()` sẵn để chuỗi gửi đi giống nhau
 * trên cả hai nền tảng; host tự parse nếu hệ tracking của mình cần kiểu khác.
 *
 * SDK **không bao giờ** đặt PII vào đây (token, số điện thoại, email, từ khoá người dùng gõ).
 */
data class PromotionEvent(
    val name: String,
    val params: Map<String, String> = emptyMap(),
)

/**
 * Nơi host nhận event của SDK. Đối ứng `PromotionTracker` bên iOS (cùng một hàm, cùng thứ tự).
 *
 * ```kotlin
 * object AppPromotionTracker : PromotionTracker {
 *     override fun track(event: PromotionEvent) {
 *         firebaseAnalytics.logEvent(event.name, event.params.toBundle())
 *     }
 * }
 * ```
 *
 * **Hợp đồng — bắt buộc đọc:**
 * - Gọi từ **thread nền**, ngay trong lúc màn hình đang cập nhật state. Phải **không chặn**: đẩy
 *   sang queue của hệ tracking rồi trả về ngay, đừng ghi DB đồng bộ hay gọi mạng tại chỗ.
 * - Ném ra thì SDK nuốt (có log cảnh báo) và **event đó mất** — nhưng màn hình không sao.
 * - **Vòng đời:** [PromotionSDK] giữ object này tới tận [PromotionSDK.release]. Cài đặt bằng
 *   anonymous object bên trong Fragment/Activity là bắt luôn màn hình đó vào SDK → **rò rỉ vĩnh
 *   viễn**. Trỏ vào một singleton **cấp app**, giống [PromotionTokenSource].
 */
interface PromotionTracker {
    fun track(event: PromotionEvent)
}

// ─── Kho dữ liệu ────────────────────────────────────────────────────────────

/**
 * Kho khoá–giá trị của host, dùng **thay** kho mặc định của SDK (`SharedPreferences`).
 *
 * Dành cho app đã có sẵn một kho — điển hình là **DB của bản SDK native cũ** — và muốn SDK đọc ghi
 * vào đúng chỗ đó thay vì mở thêm một kho thứ hai. Không cấp → SDK tự dùng `SharedPreferences` của
 * riêng nó (`promotion_sdk_prefs`), host không phải làm gì.
 *
 * Cấp rồi thì **toàn bộ** SDK dùng nó: cache cờ tính năng và theme đã lưu đều đi qua đây.
 * Đối ứng `PromotionStorage` bên iOS.
 *
 * **Hợp đồng — bắt buộc đọc:**
 * - **Đồng bộ**: [getString] phải trả giá trị ngay, [putString] phải thấy được ở lượt đọc kế tiếp.
 *   Có thể bị gọi từ **thread nền** → hiện thực phải thread-safe và **nhanh** (SDK gọi nó trên
 *   đường dựng màn). Bọc một `Room`/`SQLite` query đồng bộ ở đây là tự cắm một lần chặn vào mỗi
 *   lượt mở màn.
 * - [clear] xoá **kho** — nếu kho dùng chung với dữ liệu khác của app thì hãy thu hẹp phạm vi xoá
 *   về đúng phần của SDK, đừng xoá cả bảng.
 * - SDK **không** ghi token hay dữ liệu nhạy cảm vào đây (`docs/common/StorageGuide.md` §5.3), nên
 *   kho không cần mã hoá.
 * - **Vòng đời:** giống [PromotionTracker] — singleton cấp app.
 */
interface PromotionStorage {
    fun putBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putString(key: String, value: String)
    fun getString(key: String): String?
    fun contains(key: String): Boolean
    fun remove(key: String)
    fun clear()
}

// ─── Public → core ──────────────────────────────────────────────────────────

/**
 * Public (`com.ttcn.prm.entry`) → core (`com.ttcn.promotionsdk.host`).
 *
 * Các lớp bọc dưới đây tồn tại để kiểu của `:promotionLogic` **không** lọt vào chữ ký public của SDK
 * Android (`docs/common/PublicApi.md`) — cùng vai trò mà [PromotionMutableContext] đang làm cho
 * [PromotionTokenSource]. Đối ứng `PromotionHostServices.toCore()` bên iOS.
 */
internal fun PromotionHostServices.toCore(): CorePromotionHostServices = CorePromotionHostServices(
    tracker = tracker?.let(::PromotionTrackerAdapter),
    storage = storage?.let(::PromotionStorageAdapter),
)

internal class PromotionTrackerAdapter(
    private val delegate: PromotionTracker,
) : CorePromotionTracker {
    override fun track(event: CorePromotionEvent) {
        delegate.track(PromotionEvent(name = event.name, params = event.params))
    }
}

internal class PromotionStorageAdapter(
    private val delegate: PromotionStorage,
) : CorePromotionPreferences {
    override fun putBoolean(key: String, value: Boolean) = delegate.putBoolean(key, value)
    override fun getBoolean(key: String, default: Boolean): Boolean = delegate.getBoolean(key, default)
    override fun putString(key: String, value: String) = delegate.putString(key, value)
    override fun getString(key: String): String? = delegate.getString(key)
    override fun contains(key: String): Boolean = delegate.contains(key)
    override fun remove(key: String) = delegate.remove(key)
    override fun clear() = delegate.clear()
}
