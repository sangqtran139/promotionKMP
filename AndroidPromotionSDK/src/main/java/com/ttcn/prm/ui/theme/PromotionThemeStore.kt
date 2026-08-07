package com.ttcn.prm.ui.theme

import com.ttcn.promotionsdk.di.PromotionContainer

/**
 * Persist theme: serialize `PromotionSDKTheme` → JSON rồi lưu qua `PromotionContainer.putPreference`
 * của lõi (cơ chế chung với iOS). Tầng này giữ **key** và việc serialize; lõi giữ cơ chế lưu.
 * Đối ứng `PromotionThemeStore.swift`.
 */
internal object PromotionThemeStore {
    private const val KEY_THEME = "promotion_theme_config_v1"

    // Fail-open: `preferences` ném nếu chưa initialize() — theme không phải chức năng sống-còn.
    fun save(theme: PromotionSDKTheme) {
        runCatching { PromotionContainer.preferences.putString(KEY_THEME, PromotionThemeJson.toJson(theme)) }
    }

    fun load(): PromotionSDKTheme? =
        runCatching { PromotionContainer.preferences.getString(KEY_THEME) }
            .getOrNull()?.let(PromotionThemeJson::fromJson)

    fun clear() {
        runCatching { PromotionContainer.preferences.remove(KEY_THEME) }
    }
}
