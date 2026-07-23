package com.ttcn.promotionsdk.core.domain.usecase

import com.ttcn.promotionsdk.core.domain.model.featureflag.PromotionFeatureFlag
import com.ttcn.promotionsdk.core.domain.model.featureflag.PromotionFeatureFlags
import kotlin.coroutines.cancellation.CancellationException

/**
 * Kill-switch: "tính năng này có được phép chạy không?".
 *
 * Đây là **luật nghiệp vụ**, không phải chuyện UI — nên nó nằm ở lõi Kotlin, dùng chung cho
 * `AndroidPromotionSDK` và `iosPromotionUI`. Hai nền tảng chỉ khác nhau ở cách **hiển thị** thông báo
 * khi bị chặn (Toast vs popup), không khác nhau ở chỗ *khi nào* bị chặn.
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
 * **Fail-open**: chưa `PromotionContainer.initialize(...)` hoặc chưa có cache → coi như bật hết,
 * đúng như [PromotionFeatureFlags.AllEnabled]. SDK không tự khoá tính năng chỉ vì chưa gọi được API.
 * Vì thế mọi hàm ở đây nuốt lỗi thay vì ném — gọi trước `initialize()` trả `true`.
 *
 * Chặn ở **hai tầng**, vì có hai đường vào khác nhau:
 *  - [PromotionUseCases] (facade headless) tự gác 5 hàm nghiệp vụ → `Failure(FEATURE_DISABLED)`.
 *  - UI native dựng thẳng use case đơn lẻ nên **không** qua facade; nó gác ở điểm điều hướng bằng
 *    các hàm `canOpen…` / `canShow…` dưới đây.
 *
 * TODO(feature-flag): lõi Kotlin và bản iOS cũ parse **hai schema khác nhau** cho cùng endpoint
 * `POST api/v1/vtm/feature-flag/list`:
 *
 *     iOS    → data.enableSdk + data.features[] { featureCode: "voucher_detail", allowed }
 *     Kotlin → data[] { flagName: "PROMOTION.VOUCHER_DETAIL", enabled }
 *
 * Nếu server dùng schema iOS thì Kotlin parse hỏng, [refresh] nuốt lỗi, cache giữ mặc định bật-hết
 * → gate cho qua (kill-switch **không hoạt động**, nhưng fail-open nên không ai thấy). Khi backend
 * xác nhận schema, sửa `FeatureFlagItemResponse`; file này không phải đổi.
 */
object PromotionFeatureGate {

    /**
     * Tra một cờ bất kỳ theo tên hằng trong [PromotionFeatureFlag].
     * `PROMOTION.ENABLE_ALL` gate ngầm mọi cờ con — xem [PromotionFeatureFlags.isEnabled].
     */
    fun isEnabled(flagName: String): Boolean =
        runCatching { PromotionFeatureFlagUseCases().isEnabled(flagName) }.getOrDefault(true)

    /**
     * Công tắc tổng `PROMOTION.ENABLE_ALL`. Tắt nó thì mọi hàm `canX()` dưới đây đều trả `false`.
     *
     * Hiện chưa có nơi gọi: cả hai nền tảng đều gác theo từng tính năng, và công tắc tổng đã được
     * áp ngầm bên trong [PromotionFeatureFlags.isEnabled]. Giữ lại để host/UI hỏi trạng thái SDK.
     */
    fun isSdkEnabled(): Boolean = isEnabled(PromotionFeatureFlag.ENABLE_ALL)

    /** Mở màn "Ưu đãi của tôi". */
    fun canOpenVoucherList(): Boolean = isEnabled(PromotionFeatureFlag.VOUCHER_LIST)

    /** Mở màn "Chi tiết ưu đãi". */
    fun canOpenVoucherDetail(): Boolean = isEnabled(PromotionFeatureFlag.VOUCHER_DETAIL)

    /** Hiện widget chọn ưu đãi ở màn thanh toán, và mở màn "Chọn ưu đãi". */
    fun canShowVoucherSelection(): Boolean = isEnabled(PromotionFeatureFlag.VOUCHER_SELECTION)

    /**
     * Áp voucher vào đơn hàng (`validateDiscounts`).
     * Chưa có nơi gọi: lời gọi nghiệp vụ đã được [PromotionUseCases] tự gác bằng cờ này.
     */
    fun canApplyVoucher(): Boolean = isEnabled(PromotionFeatureFlag.VOUCHER_APPLY)

    /**
     * Tạo phiên thanh toán (`createRedemption`).
     * Chưa có nơi gọi: lời gọi nghiệp vụ đã được [PromotionUseCases] tự gác bằng cờ này.
     */
    fun canRedeemVoucher(): Boolean = isEnabled(PromotionFeatureFlag.VOUCHER_REDEEM)

    /**
     * Làm mới cờ từ server. **Không ném**: `FeatureFlagRepositoryImpl.fetchFlags` đã tự nuốt lỗi
     * mạng và giữ nguyên cờ đang cache. Ở đây chỉ cần chắn thêm trường hợp chưa `initialize()`.
     *
     * `runCatching` chỉ bọc lúc **dựng** use case, không bọc lời gọi `suspend` — bọc cả lời gọi thì
     * nó sẽ nuốt luôn `CancellationException` và phá structured concurrency.
     *
     * `@Throws(CancellationException)` là bắt buộc cho iOS: hàm `suspend` xuất sang Swift dưới dạng
     * `async`, và Kotlin/Native `abort()` nếu một exception không khai báo thoát ra.
     */
    @Throws(CancellationException::class)
    suspend fun refresh() {
        val useCases = runCatching { PromotionFeatureFlagUseCases() }.getOrNull() ?: return
        useCases.refresh()
    }
}
