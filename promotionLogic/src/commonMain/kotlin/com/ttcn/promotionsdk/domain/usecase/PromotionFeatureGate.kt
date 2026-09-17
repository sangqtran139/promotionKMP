package com.ttcn.promotionsdk.domain.usecase

import com.ttcn.promotionsdk.di.PromotionContainer
import com.ttcn.promotionsdk.domain.model.featureflag.PromotionFeatureFlag
import com.ttcn.promotionsdk.domain.model.featureflag.PromotionFeatureFlags
import kotlin.coroutines.cancellation.CancellationException

/**
 * Kill-switch: "tính năng này có được phép chạy không?".
 *
 * Object Kotlin dùng chung cho `AndroidPromotionSDK` và `iosPromotionUI`; hai nền tảng chỉ khác ở
 * cách hiển thị thông báo khi bị chặn.
 *
 * ```kotlin
 * // Android
 * if (!PromotionFeatureGate.canOpenVoucherDetail()) { showToast(...); return }
 * ```
 * ```swift
 * // iOS
 * guard PromotionFeatureGate.shared.canOpenVoucherDetail() else { showDialog(); return }
 * ```
 *
 * Cờ đọc từ cache đồng bộ (`isEnabled` **không gọi mạng**). `PROMOTION.ENABLE_ALL` là công tắc
 * tổng: tắt nó thì mọi cờ con đều tắt.
 *
 * **Hai mặc định ngược nhau — đừng gộp làm một:**
 *
 * | Tình huống | Trả về | Vì sao |
 * |---|---|---|
 * | Chưa `PromotionContainer.initialize(...)` | `false` (**fail-closed**) | SDK chưa bật thì không có màn nào mở được. Host có thể **cố ý** không init để tắt tính năng, nên trả `true` là bảo host hiện entry point dẫn tới ngõ cụt. |
 * | Đã init, chưa có cache / API cờ hỏng | `true` (**fail-open**) | Mạng chậm hay server cờ chết không được phép khoá tính năng của user. Xem [PromotionFeatureFlags.AllEnabled]. |
 *
 * Trước 2026-09-17 cả hai ca cùng trả `true`: lookup ném `IllegalStateException` ("Dependency not
 * found") khi đồ thị DI rỗng và `runCatching` nuốt luôn thành fail-open. Hệ quả thật: kill-switch đã
 * tắt từ phiên trước vẫn trả `true` nếu host hỏi trước `initialize()` — cache cờ nằm sau DI nên
 * không ai đọc được. Nay tách bằng một lần hỏi [PromotionContainer.isInitialized] tường minh.
 *
 * Mọi hàm ở đây vẫn **nuốt lỗi thay vì ném**: cờ hỏng không được phép làm chết màn hình của host.
 *
 * Chặn ở **hai tầng**:
 *  - [PromotionUseCases] (facade headless) tự gác 5 hàm nghiệp vụ → `Failure(FEATURE_DISABLED)`.
 *  - UI native dựng thẳng use case đơn lẻ nên **không** qua facade; nó gác ở điểm điều hướng bằng
 *    các hàm `canOpen…` / `canShow…` dưới đây.
 *
 * **Schema `POST api/v1/vtm/feature-flag/list` — đã gọi thẳng server xác nhận (2026-08-08):**
 *
 *     "data": [ { "flagName": "PROMOTION.VOUCHER_DETAIL", "enabled": true }, … ]
 *
 * đúng như `FeatureFlagItemResponse` đang khai, nên kill-switch **có hoạt động**. (Trước đây nghi lõi
 * Kotlin và bản iOS cũ đọc hai schema khác nhau; server thật chỉ trả dạng trên.) Server còn trả cả cờ
 * ngoài danh mục của SDK (`PROMOTION.UUDAICUATOI.TIETKIEM`, `TEST`) — mapper bỏ qua, không sao.
 *
 * Nếu schema đổi, parse sẽ hỏng và `FeatureFlagRepositoryImpl.fetchFlags` **log cảnh báo** rồi giữ
 * cache; đừng bỏ dòng log đó, nó là thứ duy nhất báo kill-switch đang chết lặng.
 */
public object PromotionFeatureGate {

    /**
     * Tra một cờ bất kỳ theo tên hằng trong [PromotionFeatureFlag].
     * `PROMOTION.ENABLE_ALL` gate ngầm mọi cờ con — xem [PromotionFeatureFlags.isEnabled].
     */
    public fun isEnabled(flagName: String): Boolean {
        // Chưa init → fail-CLOSED. Không để `runCatching` bên dưới bắt hộ: nó không phân biệt được
        // "DI rỗng vì chưa init" với "cache chưa có", mà hai ca đó cần hai mặc định ngược nhau.
        if (!PromotionContainer.isInitialized()) return false
        // Đã init → fail-OPEN cho mọi trục trặc còn lại (cache rỗng, storage hỏng…).
        return runCatching { PromotionFeatureFlagUseCases().isEnabled(flagName) }.getOrDefault(true)
    }

    /**
     * Công tắc tổng `PROMOTION.ENABLE_ALL`. Tắt nó thì mọi hàm `canX()` dưới đây đều trả `false`.
     */
    public fun isSdkEnabled(): Boolean = isEnabled(PromotionFeatureFlag.ENABLE_ALL)

    /** Mở màn "Ưu đãi của tôi". */
    public fun canOpenVoucherList(): Boolean = isEnabled(PromotionFeatureFlag.VOUCHER_LIST)

    /** Mở màn "Chi tiết ưu đãi". */
    public fun canOpenVoucherDetail(): Boolean = isEnabled(PromotionFeatureFlag.VOUCHER_DETAIL)

    /** Hiện widget chọn ưu đãi ở màn thanh toán, và mở màn "Chọn ưu đãi". */
    public fun canShowVoucherSelection(): Boolean = isEnabled(PromotionFeatureFlag.VOUCHER_SELECTION)

    /** Áp voucher vào đơn hàng (`validateDiscounts`). */
    public fun canApplyVoucher(): Boolean = isEnabled(PromotionFeatureFlag.VOUCHER_APPLY)

    /** Tạo phiên thanh toán (`createRedemption`). */
    public fun canRedeemVoucher(): Boolean = isEnabled(PromotionFeatureFlag.VOUCHER_REDEEM)

    /**
     * Làm mới cờ từ server. **Không ném**: `FeatureFlagRepositoryImpl.fetchFlags` tự nuốt lỗi mạng và
     * giữ nguyên cờ đang cache; `runCatching` ở đây chỉ bọc bước dựng use case, còn lời gọi `suspend`
     * để ngoài nên `CancellationException` truyền tiếp lên caller.
     */
    @Throws(CancellationException::class)
    public suspend fun refresh() {
        // Chưa init thì không có baseUrl lẫn nguồn token để gọi API — về sớm, tường minh.
        if (!PromotionContainer.isInitialized()) return
        val useCases = runCatching { PromotionFeatureFlagUseCases() }.getOrNull() ?: return
        useCases.refresh()
    }
}
