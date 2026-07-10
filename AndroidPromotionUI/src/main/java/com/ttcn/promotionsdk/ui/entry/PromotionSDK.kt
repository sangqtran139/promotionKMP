package com.ttcn.promotionsdk.ui.entry

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.core.di.PromotionContainer
// Extension ở androidMain của promotionLogic: nạp applicationContext + suy ra isDebug.
import com.ttcn.promotionsdk.core.di.initialize
import com.ttcn.promotionsdk.core.domain.model.featureflag.PromotionFeatureFlag
import com.ttcn.promotionsdk.core.domain.model.featureflag.PromotionFeatureFlags
import com.ttcn.promotionsdk.core.domain.usecase.PromotionFeatureGate
import com.ttcn.promotionsdk.ui.entry.PromotionSDK.getTheme
import com.ttcn.promotionsdk.ui.entry.PromotionSDK.init
import com.ttcn.promotionsdk.ui.entry.api.PromotionSDKApi
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyPromotionFragment
import com.ttcn.promotionsdk.ui.theme.PromotionSDKTheme
import com.ttcn.promotionsdk.ui.theme.PromotionThemeRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

object PromotionSDK {

    private const val TAG_MY_PROMOTION = "prm_my_promotion"

    private var theme: PromotionSDKTheme = PromotionSDKTheme()
    private var callback: PromotionSDKCallback? = null
    private var contextProvider: PromotionContextProvider? = null
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

    /** Context do host cấp ở [PromotionSDKOptions.config]. Null khi chưa [init] hoặc host không truyền. */
    @JvmStatic
    val requestContext: PromotionContextProvider? get() = contextProvider

    // SDK **không** phơi API hỏi feature flag ra host. Host không cần biết cờ nào đang bật: mọi
    // điểm vào đều tự gác qua `PromotionFeatureGate` của `promotionLogic` — `openMyPromotion`,
    // `PRMBaseFragment.openPromotionDetail`, `PRMEndowView` — và hiện thông báo PRM_MOB_021 khi bị
    // chặn. Trước đây có `PromotionSDK.featureFlags`, nhưng không nơi nào dùng.

    /**
     * Keeps [getTheme] in sync when hosts call [PromotionTheme.configure] / [PromotionTheme.clear]
     * without re-running [init].
     */
    @JvmStatic
    internal fun syncThemeConfig(config: PromotionSDKTheme?) {
        theme = config ?: PromotionSDKTheme()
    }

    @JvmStatic
    fun init(context: Context, options: PromotionSDKOptions) {
        callback = options.callback
        contextProvider = options.config.contextProvider
        PromotionContainer.initialize(context, options.config.toCoreConfig())
        theme = options.theme
        PromotionThemeRegistry.configure(options.theme)
        sdkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        // Nạp cờ tính năng từ server. `refresh()` không ném lỗi: hỏng thì giữ cache (fail-open).
        sdkScope?.launch { PromotionFeatureGate.refresh() }
    }

    /** `true` sau [init] và trước [release]. Gọi [release] khi chưa init là vô hại. */
    @JvmStatic
    fun isInitialized(): Boolean = PromotionContainer.isInitialized()

    @JvmStatic
    fun getTheme(): PromotionSDKTheme = theme

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

    @JvmStatic
    fun release() {
        sdkScope?.cancel()
        sdkScope = null
        PromotionContainer.clear()
        PromotionThemeRegistry.configure(null)
        syncThemeConfig(null)
        callback = null
        contextProvider = null
    }

}
