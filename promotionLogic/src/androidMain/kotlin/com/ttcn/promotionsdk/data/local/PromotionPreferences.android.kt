package com.ttcn.promotionsdk.data.local

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import kotlin.concurrent.Volatile

/**
 * Giữ `applicationContext` do `PromotionContainer.initialize(context, config)` nạp vào. Chỉ dùng để mở
 * `SharedPreferences` — SDK không giữ context nào khác.
 */
internal object AndroidContextHolder {

    @Volatile
    private var applicationContext: Context? = null

    fun set(context: Context) {
        applicationContext = context.applicationContext
    }

    fun clear() {
        applicationContext = null
    }

    fun require(): Context = requireNotNull(applicationContext) {
        "Promotion SDK chưa có Context. Trên Android hãy gọi PromotionContainer.initialize(context, config)."
    }
}

/**
 * Tự mở file prefs riêng (`promotion_sdk_prefs`, `MODE_PRIVATE`) rồi truyền vào `Settings` — **không**
 * dùng module `multiplatform-settings-no-arg`, vì bản đó lấy `PreferenceManager.getDefaultSharedPreferences()`
 * tức file prefs MẶC ĐỊNH của app host, và `PromotionPreferences.clear()` sẽ xoá sạch dữ liệu của họ.
 * Xem docs/common/StorageGuide.md §2.1.
 */
internal actual fun createPreferences(): PromotionPreferences = SettingsPreferences(
    SharedPreferencesSettings(
        AndroidContextHolder.require()
            .getSharedPreferences(PromotionPreferences.PREFS_NAME, Context.MODE_PRIVATE)
    )
)
