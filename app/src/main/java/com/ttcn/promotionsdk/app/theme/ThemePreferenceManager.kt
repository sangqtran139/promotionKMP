package com.ttcn.promotionsdk.app.theme

import android.content.Context
import com.ttcn.promotionsdk.ui.theme.PromotionThemeConfig
import com.ttcn.promotionsdk.ui.theme.PromotionThemeJson

class ThemePreferenceManager(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(config: PromotionThemeConfig) {
        prefs.edit().putString(KEY_THEME_CONFIG, PromotionThemeJson.toJson(config)).apply()
    }

    fun load(): PromotionThemeConfig? {
        val json = prefs.getString(KEY_THEME_CONFIG, null) ?: return null
        return PromotionThemeJson.fromJson(json)
    }

    fun clear() {
        prefs.edit().remove(KEY_THEME_CONFIG).apply()
    }

    companion object {
        private const val PREFS_NAME = "promotion_theme_prefs"
        private const val KEY_THEME_CONFIG = "promotion_theme_config"
    }
}
