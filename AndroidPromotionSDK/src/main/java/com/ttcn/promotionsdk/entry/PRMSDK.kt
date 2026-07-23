package com.ttcn.promotionsdk.entry

// Extension ở androidMain của promotionLogic: nạp applicationContext + suy ra isDebug.
import android.content.Context
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.core.di.PromotionContainer
import com.ttcn.promotionsdk.core.di.initialize
import com.ttcn.promotionsdk.core.domain.model.featureflag.PromotionFeatureFlag
import com.ttcn.promotionsdk.core.domain.usecase.PromotionFeatureGate
import com.ttcn.promotionsdk.entry.api.PRMOrderItem
import com.ttcn.promotionsdk.entry.api.PRMSDKApi
import com.ttcn.promotionsdk.promotionsdkui.feature.promotion.mypromotion.PRMMyPromotionFragment
import com.ttcn.promotionsdk.promotionsdkui.feature.promotion.promotiondetail.PRMDetailFragment
import com.ttcn.promotionsdk.promotionsdkui.theme.PRMSDKTheme
import com.ttcn.promotionsdk.promotionsdkui.theme.PromotionThemeRegistry
import com.ttcn.promotionsdk.promotionsdkui.theme.PromotionThemeStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Điểm vào SDK — singleton `object`, đối ứng 1:1 `PRMSDK` bên iOS (thứ tự thành viên khớp nhau:
 * xem docs/InitParity.md §1). Cấu hình một lần qua [initialize], cập nhật đơn hàng/dịch vụ qua [updateContext].
 */
object PRMSDK {

    private const val TAG_MY_PROMOTION = "prm_my_promotion"
    private const val TAG_PROMOTION_DETAIL = "prm_promotion_detail"

    private var callback: PRMSDKCallback? = null
    private var mutableContext: PromotionMutableContext? = null
    private var sdkScope: CoroutineScope? = null

    // ─── Init ────────────────────────────────────────────────────────────────

    /**
     * Khởi tạo SDK. Đối ứng `PRMSDK.initialize(options:)` bên iOS (iOS không cần `context`).
     *
     * Gọi lại [initialize] = dựng lại đồ thị DI với session mới (vd refresh token → truyền session mới).
     * Host truyền theme → ghi đè và lưu; không truyền → khôi phục theme đã lưu lần trước.
     */
    @JvmStatic
    fun initialize(context: Context, options: PRMSDKOptions) {
        callback = options.callback
        mutableContext = PromotionMutableContext(options.session)
        PromotionContainer.initialize(context, options.toCoreConfig(mutableContext))
        // Host truyền theme → ghi đè và lưu. Không truyền → khôi phục theme đã lưu lần trước.
        // Nhờ vậy host cấu hình một lần; các lần mở app sau chỉ cần initialize(config), theme tự sống lại.
        val resolved = options.theme
        if (resolved != null) PromotionThemeStore.save(resolved)
        PromotionThemeRegistry.configure(resolved ?: PromotionThemeStore.load())
        sdkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        // Nạp cờ tính năng từ server. `refresh()` không ném lỗi: hỏng thì giữ cache (fail-open).
        sdkScope?.launch { PromotionFeatureGate.refresh() }
    }

    /**
     * Giải phóng SDK. Đối ứng `PRMSDK.release()` bên iOS. Gọi khi chưa init là vô hại.
     * **Không** xoá theme đã lưu — nó sống qua release/init.
     */
    @JvmStatic
    fun release() {
        sdkScope?.cancel()
        sdkScope = null
        PromotionContainer.clear()
        // Reset registry trong bộ nhớ; **không** xoá theme đã lưu — nó sống qua release/init.
        PromotionThemeRegistry.configure(null)
        callback = null
        mutableContext = null
    }

    /** `true` sau [initialize] và trước [release]. Gọi [release] khi chưa init là vô hại. */
    @JvmStatic
    fun isInitialized(): Boolean = PromotionContainer.isInitialized()

    /** Callback đã truyền lúc [initialize] (`null` nếu chưa init / không truyền). Đối ứng `getCallback()` iOS. */
    @JvmStatic
    fun getCallback(): PRMSDKCallback? = callback

    // ─── Headless API ────────────────────────────────────────────────────────

    /**
     * Bề mặt headless cho host tự dựng UI. Phải gọi [initialize] trước.
     *
     * ```kotlin
     * when (val r = PRMSDK.api.getVouchers()) {
     *     is PRMApiResult.Success -> render(r.data.vouchers)
     *     is PRMApiResult.Failure -> showError(r.error)
     * }
     * ```
     *
     * Thay cho `useCases: PromotionUseCases` trước đây — kiểu đó thuộc `promotionLogic`, mà host
     * chỉ tích hợp `AndroidPromotionSDK` nên không resolve được. Xem [PRMSDKApi].
     *
     * Dựng mới mỗi lần đọc: sau [release] + [initialize] lại, instance cũ vẫn giữ use case của đồ thị DI đã bị huỷ.
     *
     * @throws IllegalStateException khi chưa gọi [initialize].
     */
    @JvmStatic
    val api: PRMSDKApi
        get() {
            check(PromotionContainer.isInitialized()) {
                "PRMSDK.initialize() must be called before api."
            }
            return PRMSDKApi()
        }

    // ─── Context ─────────────────────────────────────────────────────────────

    /** Session đã truyền lúc [initialize]. Null khi chưa [initialize] hoặc không truyền [PRMSessionConfig]. */
    @JvmStatic
    val session: PRMSessionConfig? get() = mutableContext?.session

    /** Giá trị dynamic hiện tại được ghi qua [updateContext]. Null khi chưa [updateContext]. */
    @JvmStatic
    val currentOrderId: String? get() = mutableContext?.orderId
    @JvmStatic
    val currentOrderValue: String? get() = mutableContext?.orderValue
    @JvmStatic
    val currentServiceCode: String? get() = mutableContext?.serviceCode
    @JvmStatic
    val currentMetaData: String? get() = mutableContext?.metaData

    /**
     * Cập nhật context đơn hàng / dịch vụ — gọi mỗi khi host vào màn có voucher (checkout, dịch vụ…).
     *
     * Ghi vào [PromotionMutableContext] đang sống; không cần [initialize] lại. SDK đọc lại các giá trị này
     * ở **mỗi** request, nên gọi trước khi mở màn hoặc gọi API là đủ.
     *
     * @param orderItems Dòng sản phẩm của đơn — cần khi muốn lấy campaign theo SKU; bỏ trống thì
     * chỉ nhận campaign cấp đơn. Đối ứng `PRMSDK.updateContext(orderItems:)` bên iOS.
     *
     * @throws IllegalStateException nếu [initialize] chưa được gọi.
     */
    @JvmStatic
    @JvmOverloads
    fun updateContext(
        orderId: String? = null,
        orderValue: String? = null,
        serviceCode: String? = null,
        metaData: String? = null,
        orderItems: List<PRMOrderItem> = emptyList(),
    ) {
        val ctx = checkNotNull(mutableContext) {
            "PRMSDK.initialize() must be called before updateContext()."
        }
        ctx.orderId = orderId
        ctx.orderValue = orderValue
        ctx.serviceCode = serviceCode
        ctx.metaData = metaData
        ctx.orderItems = orderItems
    }

    // ─── Theming ─────────────────────────────────────────────────────────────

    /**
     * Đổi theme **và lưu lại** sau khi đã [initialize]. `null` = xoá theme đã lưu, về mặc định SDK.
     * Đối ứng `PRMSDK.configure(theme:)` bên iOS.
     *
     * View đã render có thể chỉ cập nhật khi được dựng lại (rebind/đẩy màn mới) — nên cấu hình theme
     * **một lần** lúc khởi tạo là tốt nhất.
     */
    @JvmStatic
    fun configure(theme: PRMSDKTheme?) {
        PromotionThemeRegistry.configure(theme)
        if (theme != null) PromotionThemeStore.save(theme) else PromotionThemeStore.clear()
    }

    /** Theme đang áp (`null` nếu đang dùng mặc định). Đối ứng `PRMSDK.currentTheme()` bên iOS. */
    @JvmStatic
    fun currentTheme(): PRMSDKTheme? = PromotionThemeRegistry.currentConfig()

    // ─── Feature flag ────────────────────────────────────────────────────────
    //
    // SDK **không** phơi API hỏi feature flag ra host. Host không cần biết cờ nào đang bật: mọi
    // điểm vào đều tự gác qua `PromotionFeatureGate` của `promotionLogic` — `openMyPromotion`,
    // `PRMBaseFragment.openPromotionDetail`, `PRMEndowView` — và hiện thông báo PRM_MOB_021 khi bị
    // chặn. Trước đây có `PRMSDK.featureFlags`, nhưng không nơi nào dùng.

    // ─── Screens ─────────────────────────────────────────────────────────────

    /**
     * Hiển thị màn "Ưu đãi của tôi" ([PRMMyPromotionFragment]).
     *
     * Gác bởi cờ [PromotionFeatureFlag.VOUCHER_LIST]: TẮT → hiện thông báo PRM_MOB_021 và **không**
     * mở màn, y như `PRMSDK.openMyPromotion` bên iOS.
     *
     * @param activity Activity host (FragmentActivity / AppCompatActivity).
     * @param containerViewId Nếu khác null, dùng FragmentTransaction.replace trên container này;
     * nếu null, fragment được add lên android.R.id.content.
     *
     * @throws IllegalStateException khi chưa gọi [initialize].
     */
    @JvmStatic
    @JvmOverloads
    fun openMyPromotion(activity: FragmentActivity, containerViewId: Int? = null) {
        check(PromotionContainer.isInitialized()) {
            "PRMSDK.initialize() must be called before openMyPromotion()."
        }
        if (!PromotionFeatureGate.canOpenVoucherList()) {
            Toast.makeText(activity, R.string.prm_feature_disabled, Toast.LENGTH_SHORT).show()
            callback?.onAvailabilityChanged(false)
            return
        }
        val fm = activity.supportFragmentManager
        if (fm.findFragmentByTag(TAG_MY_PROMOTION) != null) return
        val fragment = PRMMyPromotionFragment()
        fm.beginTransaction()
            .setReorderingAllowed(true)
            .apply {
                if (containerViewId != null) {
                    replace(containerViewId, fragment, TAG_MY_PROMOTION)
                } else {
                    add(android.R.id.content, fragment, TAG_MY_PROMOTION)
                }
            }
            .addToBackStack(TAG_MY_PROMOTION)
            .commit()
    }

    /**
     * Mở thẳng màn "Chi tiết ưu đãi" theo [voucherId], **không** qua danh sách.
     *
     * Dùng khi host đã biết id — vd bấm vào push notification, hoặc deeplink từ banner ngoài SDK.
     * Đối ứng `PRMSDK.openPromotionDetail(voucherId:from:)` bên iOS.
     *
     * Gác bởi cờ [PromotionFeatureFlag.VOUCHER_DETAIL] y như đường vào nội bộ
     * (`PRMBaseFragment.openPromotionDetail`): TẮT → thông báo PRM_MOB_021 và **không** mở màn.
     * Gác ở đây là bắt buộc — kill-switch không được có cửa sau chỉ vì host gọi thẳng entry.
     *
     * Màn tự fetch chi tiết theo [voucherId]; trong lúc chờ hiện shimmer.
     *
     * @param voucherId Id voucher cần xem.
     * @param activity Activity host (FragmentActivity / AppCompatActivity).
     * @param containerViewId Khác null → `replace` trên container này; null → `add` lên
     * `android.R.id.content`. Cùng quy ước với [openMyPromotion].
     *
     * @throws IllegalStateException khi chưa gọi [initialize].
     */
    @JvmStatic
    @JvmOverloads
    fun openPromotionDetail(
        voucherId: String,
        activity: FragmentActivity,
        containerViewId: Int? = null,
    ) {
        check(PromotionContainer.isInitialized()) {
            "PRMSDK.initialize() must be called before openPromotionDetail()."
        }
        if (!PromotionFeatureGate.canOpenVoucherDetail()) {
            Toast.makeText(activity, R.string.prm_feature_disabled, Toast.LENGTH_SHORT).show()
            callback?.onAvailabilityChanged(false)
            return
        }
        val fm = activity.supportFragmentManager
        if (fm.findFragmentByTag(TAG_PROMOTION_DETAIL) != null) return
        val fragment = PRMDetailFragment.newInstance(voucherId)
        fm.beginTransaction()
            .setReorderingAllowed(true)
            .apply {
                if (containerViewId != null) {
                    replace(containerViewId, fragment, TAG_PROMOTION_DETAIL)
                } else {
                    add(android.R.id.content, fragment, TAG_PROMOTION_DETAIL)
                }
            }
            .addToBackStack(TAG_PROMOTION_DETAIL)
            .commit()
    }
}
