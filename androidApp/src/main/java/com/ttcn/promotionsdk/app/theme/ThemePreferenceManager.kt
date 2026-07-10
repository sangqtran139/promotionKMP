package com.ttcn.promotionsdk.app.theme

import android.content.Context
import com.ttcn.promotionsdk.ui.entry.PromotionTheme
import com.ttcn.promotionsdk.ui.theme.PromotionSDKTheme

/**
 * App demo tự lưu theme, dùng serializer của SDK ([PromotionTheme.toJson]). Mirror
 * `ThemePreferenceManager.swift` bên iOS.
 *
 * Trước đây file này dùng đúng `PREFS_NAME`/`KEY` của `PromotionThemeStore` trong SDK — hai bên ghi
 * đè lên nhau trong cùng một file SharedPreferences. Giờ app có không gian riêng.
 */
class ThemePreferenceManager(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(theme: PromotionSDKTheme) {
        prefs.edit().putString(KEY_THEME, PromotionTheme.toJson(theme)).apply()
    }

    fun load(): PromotionSDKTheme? {
        val json = prefs.getString(KEY_THEME, null) ?: return null
        return PromotionTheme.fromJson(json)
    }

    fun clear() {
        prefs.edit().remove(KEY_THEME).apply()
    }

    companion object {
        private const val PREFS_NAME = "demo_theme_prefs"
        private const val KEY_THEME = "demo_theme"
    }
}
