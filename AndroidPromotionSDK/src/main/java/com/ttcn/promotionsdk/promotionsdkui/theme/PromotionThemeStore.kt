package com.ttcn.promotionsdk.promotionsdkui.theme

import com.ttcn.promotionsdk.core.di.PromotionContainer

/**
 * Persist theme: serialize `PRMSDKTheme` → JSON rồi lưu qua `PromotionContainer.putPreference`
 * của lõi (cơ chế chung với iOS). Tầng này giữ **key** và việc serialize; lõi giữ cơ chế lưu.
 * Đối ứng `PromotionThemeStore.swift`.
 */
internal object PromotionThemeStore {
    private const val KEY_THEME = "promotion_theme_config_v1"

    // Fail-open: `preferences` ném nếu chưa initialize() — theme không phải chức năng sống-còn.
    fun save(theme: PRMSDKTheme) {
        runCatching { PromotionContainer.preferences.putString(KEY_THEME, PRMThemeJson.toJson(theme)) }
    }

    fun load(): PRMSDKTheme? =
        runCatching { PromotionContainer.preferences.getString(KEY_THEME) }
            .getOrNull()?.let(PRMThemeJson::fromJson)

    fun clear() {
        runCatching { PromotionContainer.preferences.remove(KEY_THEME) }
    }
}
