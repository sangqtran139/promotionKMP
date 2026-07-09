package com.ttcn.promotionsdk.ui.theme

import android.content.Context

internal object PromotionThemeStore {

    private const val PREFS_NAME = "promotion_theme_prefs"
    private const val KEY_THEME_CONFIG = "promotion_theme_config"

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun save(config: PromotionThemeConfig) {
        prefs()?.edit()?.putString(KEY_THEME_CONFIG, PromotionThemeJson.toJson(config))?.apply()
    }

    fun load(): PromotionThemeConfig? {
        val json = prefs()?.getString(KEY_THEME_CONFIG, null) ?: return null
        return PromotionThemeJson.fromJson(json)
    }

    fun clear() {
        prefs()?.edit()?.remove(KEY_THEME_CONFIG)?.apply()
        appContext = null
    }

    private fun prefs() = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
