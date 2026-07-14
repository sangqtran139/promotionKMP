package com.ttcn.promotionsdk.ui.entry

// Extension ở androidMain của promotionLogic: nạp applicationContext + suy ra isDebug.
import android.content.Context
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.core.di.PromotionContainer
import com.ttcn.promotionsdk.core.di.initialize
import com.ttcn.promotionsdk.core.domain.model.featureflag.PromotionFeatureFlag
import com.ttcn.promotionsdk.core.domain.usecase.PromotionFeatureGate
import com.ttcn.promotionsdk.ui.entry.PromotionSDK.init
import com.ttcn.promotionsdk.ui.entry.PromotionSDK.release
import com.ttcn.promotionsdk.ui.entry.PromotionSDK.updateContext
import com.ttcn.promotionsdk.ui.entry.api.PromotionSDKApi
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyPromotionFragment
import com.ttcn.promotionsdk.ui.feature.promotion.promotiondetail.PromotionDetailFragment
import com.ttcn.promotionsdk.ui.theme.PromotionSDKTheme
import com.ttcn.promotionsdk.ui.theme.PromotionThemeRegistry
import com.ttcn.promotionsdk.ui.theme.PromotionThemeStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

object PromotionSDK {

    private const val TAG_MY_PROMOTION = "prm_my_promotion"
    private const val TAG_PROMOTION_DETAIL = "prm_promotion_detail"

    private var callback: PromotionSDKCallback? = null
    private var mutableContext: PromotionMutableContext? = null
    private var sdkScope: CoroutineScope? = null

    /**
     * Bề mặt headless cho host tự dựng UI. Phải gọi [init] trước.
     *
     * ```kotlin
     * when (val r = PromotionSDK.api.getVouchers()) {
     *     is PromotionApiResult.Success -> render(r.data.vouchers)
     *     is PromotionApiResult.Failure -> showError(r.error)
     * }
     * ```
     *
     * Thay cho `useCases: PromotionUseCases` trước đây — kiểu đó thuộc `promotionLogic`, mà host
     * chỉ tích hợp `AndroidPromotionUI` nên không resolve được. Xem [PromotionSDKApi].
     *
     * Dựng mới mỗi lần đọc: sau [release] + [init] lại, instance cũ vẫn giữ use case của đồ thị DI đã bị huỷ.
     *
     * @throws IllegalStateException khi chưa gọi [init].
     */
    @JvmStatic
    val api: PromotionSDKApi
        get() {
            check(PromotionContainer.isInitialized()) {
                "PromotionSDK.init() must be called before api."
            }
            return PromotionSDKApi()
        }

    /** Session đã truyền lúc [init]. Null khi chưa [init] hoặc không truyền [PromotionSessionConfig]. */
    @JvmStatic
    val session: PromotionSessionConfig? get() = mutableContext?.session

    /** Giá trị dynamic hiện tại được ghi qua [updateContext]. Null khi chưa [updateContext]. */
    @JvmStatic
    val currentOrderId: String? get() = mutableContext?.orderId
    @JvmStatic
    val currentOrderValue: String? get() = mutableContext?.orderValue
    @JvmStatic
    val currentServiceCode: String? get() = mutableContext?.serviceCode
    @JvmStatic
    val currentMetaData: String? get() = mutableContext?.metaData

    // SDK **không** phơi API hỏi feature flag ra host. Host không cần biết cờ nào đang bật: mọi
    // điểm vào đều tự gác qua `PromotionFeatureGate` của `promotionLogic` — `openMyPromotion`,
    // `PRMBaseFragment.openPromotionDetail`, `PRMEndowView` — và hiện thông báo PRM_MOB_021 khi bị
    // chặn. Trước đây có `PromotionSDK.featureFlags`, nhưng không nơi nào dùng.

    @JvmStatic
    fun init(context: Context, options: PromotionSDKOptions) {
        callback = options.callback
        mutableContext = PromotionMutableContext(options.session)
        PromotionContainer.initialize(context, options.toCoreConfig(mutableContext))
        // Host truyền theme → ghi đè và lưu. Không truyền → khôi phục theme đã lưu lần trước.
        // Nhờ vậy host cấu hình một lần; các lần mở app sau chỉ cần init(config), theme tự sống lại.
        val resolved = options.theme
        if (resolved != null) PromotionThemeStore.save(resolved)
        PromotionThemeRegistry.configure(resolved ?: PromotionThemeStore.load())
        sdkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        // Nạp cờ tính năng từ server. `refresh()` không ném lỗi: hỏng thì giữ cache (fail-open).
        sdkScope?.launch { PromotionFeatureGate.refresh() }
    }

    /**
     * Cập nhật context đơn hàng / dịch vụ — gọi mỗi khi host vào màn có voucher (checkout, dịch vụ…).
     *
     * Ghi vào [PromotionMutableContext] đang sống; không cần [init] lại. SDK đọc lại các giá trị này
     * ở **mỗi** request, nên gọi trước khi mở màn hoặc gọi API là đủ.
     *
     * @throws IllegalStateException nếu [init] chưa được gọi.
     */
    @JvmStatic
    @JvmOverloads
    fun updateContext(
        orderId: String? = null,
        orderValue: String? = null,
        serviceCode: String? = null,
        metaData: String? = null,
    ) {
        val ctx = checkNotNull(mutableContext) {
            "PromotionSDK.init() must be called before updateContext()."
        }
        ctx.orderId = orderId
        ctx.orderValue = orderValue
        ctx.serviceCode = serviceCode
        ctx.metaData = metaData
    }

    /**
     * Đổi theme **và lưu lại** sau khi đã [init]. `null` = xoá theme đã lưu, về mặc định SDK.
     * Đối ứng `PromotionSDK.configure(theme:)` bên iOS (khác: iOS trên instance, Android trên object).
     *
     * View đã render có thể chỉ cập nhật khi được dựng lại (rebind/đẩy màn mới) — nên cấu hình theme
     * **một lần** lúc khởi tạo là tốt nhất.
     */
    @JvmStatic
    fun configure(theme: PromotionSDKTheme?) {
        PromotionThemeRegistry.configure(theme)
        if (theme != null) PromotionThemeStore.save(theme) else PromotionThemeStore.clear()
    }

    /** Theme đang áp (`null` nếu đang dùng mặc định). Đối ứng `sdk.currentTheme` bên iOS. */
    @JvmStatic
    fun currentTheme(): PromotionSDKTheme? = PromotionThemeRegistry.currentConfig()

    /** `true` sau [init] và trước [release]. Gọi [release] khi chưa init là vô hại. */
    @JvmStatic
    fun isInitialized(): Boolean = PromotionContainer.isInitialized()

    @JvmStatic
    fun getCallback(): PromotionSDKCallback? = callback

    /**
     * Hiển thị màn "Ưu đãi của tôi" ([MyPromotionFragment]).
     *
     * Gác bởi cờ [PromotionFeatureFlag.VOUCHER_LIST]: TẮT → hiện thông báo PRM_MOB_021 và **không**
     * mở màn, y như `PromotionSDK.openMyPromotions` bên iOS.
     *
     * @param activity Activity host (FragmentActivity / AppCompatActivity).
     * @param containerViewId Nếu khác null, dùng FragmentTransaction.replace trên container này;
     * nếu null, fragment được add lên android.R.id.content.
     *
     * @throws IllegalStateException khi chưa gọi [init].
     */
    @JvmStatic
    @JvmOverloads
    fun openMyPromotion(activity: FragmentActivity, containerViewId: Int? = null) {
        check(PromotionContainer.isInitialized()) {
            "PromotionSDK.init() must be called before openMyPromotion()."
        }
        if (!PromotionFeatureGate.canOpenVoucherList()) {
            Toast.makeText(activity, R.string.prm_feature_disabled, Toast.LENGTH_SHORT).show()
            return
        }
        val fm = activity.supportFragmentManager
        if (fm.findFragmentByTag(TAG_MY_PROMOTION) != null) return
        val fragment = MyPromotionFragment()
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
     * Đối ứng `sdk.openPromotionDetail(voucherId:from:)` bên iOS.
     *
     * Gác bởi cờ [PromotionFeatureFlag.VOUCHER_DETAIL] y như đường vào nội bộ
     * (`PRMBaseFragment.openPromotionDetail`): TẮT → thông báo PRM_MOB_021 và **không** mở màn.
     * Gác ở đây là bắt buộc — kill-switch không được có cửa sau chỉ vì host gọi thẳng entry.
     *
     * Màn tự fetch chi tiết theo [voucherId]; trong lúc chờ hiện shimmer.
     *
     * @param activity Activity host (FragmentActivity / AppCompatActivity).
     * @param voucherId Id voucher cần xem.
     * @param containerViewId Khác null → `replace` trên container này; null → `add` lên
     * `android.R.id.content`. Cùng quy ước với [openMyPromotion].
     *
     * @throws IllegalStateException khi chưa gọi [init].
     */
    @JvmStatic
    @JvmOverloads
    fun openPromotionDetail(
        activity: FragmentActivity,
        voucherId: String,
        containerViewId: Int? = null,
    ) {
        check(PromotionContainer.isInitialized()) {
            "PromotionSDK.init() must be called before openPromotionDetail()."
        }
        if (!PromotionFeatureGate.canOpenVoucherDetail()) {
            Toast.makeText(activity, R.string.prm_feature_disabled, Toast.LENGTH_SHORT).show()
            return
        }
        val fm = activity.supportFragmentManager
        if (fm.findFragmentByTag(TAG_PROMOTION_DETAIL) != null) return
        val fragment = PromotionDetailFragment.newInstance(voucherId)
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

}
