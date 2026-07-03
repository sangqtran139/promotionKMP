// vds-promotion/src/main/java/com/ttcn/promotionsdk/ui/entry/PromotionSDK.kt
package com.ttcn.promotionsdk.ui.entry

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.ttcn.promotionsdk.core.di.PromotionContainer
import com.ttcn.promotionsdk.core.di.internal.SdkDi
import com.ttcn.promotionsdk.core.di.internal.get
import com.ttcn.promotionsdk.core.domain.model.featureflag.PromotionFeatureFlags
import com.ttcn.promotionsdk.core.domain.usecase.GetPromotionFeatureFlagsUseCase
import com.ttcn.promotionsdk.core.domain.usecase.PromotionUseCases
import com.ttcn.promotionsdk.ui.di.ViewModelModule
import com.ttcn.promotionsdk.ui.entry.PromotionSDK.getTheme
import com.ttcn.promotionsdk.ui.entry.PromotionSDK.init
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyPromotionFragment
import com.ttcn.promotionsdk.ui.theme.PromotionSDKTheme
import com.ttcn.promotionsdk.ui.theme.PromotionThemeConfig
import com.ttcn.promotionsdk.ui.theme.PromotionThemeRegistry
import com.ttcn.promotionsdk.ui.theme.PromotionThemeStore

object PromotionSDK {

    private const val TAG_MY_PROMOTION = "prm_my_promotion"

    private var theme: PromotionSDKTheme = PromotionSDKTheme()
    private var callback: PromotionSDKCallback? = null

    @Volatile
    private var isUiDiLoaded: Boolean = false

    /**
     * Truy cập tất cả use case của SDK ở chế độ headless (tự build UI).
     * Phải gọi [init] trước khi sử dụng.
     *
     * ```kotlin
     * val result = PromotionSDK.useCases.searchVouchers(request)
     * ```
     */
    @JvmStatic
    val useCases: PromotionUseCases get() = get()

    /**
     * Trạng thái feature flags hiện tại từ Unleash.
     * Phải gọi [init] trước khi sử dụng.
     *
     * ```kotlin
     * val flags = PromotionSDK.featureFlags
     * if (flags.isEnabled(PromotionFeatureFlag.VOUCHER_LIST)) { ... }
     * ```
     */
    @JvmStatic
    val featureFlags: PromotionFeatureFlags get() = get<GetPromotionFeatureFlagsUseCase>()()

    /**
     * Keeps [getTheme] in sync when hosts call [PromotionTheme.configure] / [PromotionTheme.clear]
     * without re-running [init].
     */
    @JvmStatic
    internal fun syncThemeConfig(config: PromotionThemeConfig?) {
        theme = when (config) {
            null -> PromotionSDKTheme()
            else -> PromotionSDKTheme(config = config)
        }
    }

    @JvmStatic
    fun init(context: Context, options: PromotionSDKOptions) {
        callback = options.callback
        PromotionContainer.init(context, options.config)
        PromotionThemeStore.init(context)
        val resolvedConfig = PromotionThemeStore.load() ?: options.theme.config
        val resolvedTheme = PromotionSDKTheme(config = resolvedConfig)
        theme = resolvedTheme
        PromotionThemeRegistry.configure(resolvedConfig)
        ensureUiDiLoaded()
    }

    @JvmStatic
    fun getTheme(): PromotionSDKTheme = theme

    @JvmStatic
    fun getCallback(): PromotionSDKCallback? = callback

    /**
     * Hiển thị màn "Ưu đãi của tôi" ([MyPromotionFragment]).
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
        ensureUiDiLoaded()
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
        PromotionContainer.clear()
        PromotionThemeRegistry.configure(null)
        PromotionThemeStore.clear()
        syncThemeConfig(null)
        callback = null
        isUiDiLoaded = false
    }

    private fun ensureUiDiLoaded() {
        if (isUiDiLoaded) return
        synchronized(this) {
            if (isUiDiLoaded) return
            SdkDi.getInstance().loadModules(ViewModelModule.module)
            isUiDiLoaded = true
        }
    }
}
