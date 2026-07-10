package com.ttcn.promotionsdk.ui.entry

import android.content.Context
import androidx.annotation.ColorInt
import com.ttcn.promotionsdk.ui.theme.PromotionSDKTheme
import com.ttcn.promotionsdk.ui.theme.PromotionThemeDefaults
import com.ttcn.promotionsdk.ui.theme.PromotionThemeDisplay
import com.ttcn.promotionsdk.ui.theme.PromotionThemeJson
import com.ttcn.promotionsdk.ui.theme.PromotionThemeRegistry
import com.ttcn.promotionsdk.ui.theme.token.ButtonToken
import com.ttcn.promotionsdk.ui.theme.token.DiscountBadgeToken
import com.ttcn.promotionsdk.ui.theme.token.ListItemToken
import com.ttcn.promotionsdk.ui.theme.token.SearchBarToken
import com.ttcn.promotionsdk.ui.theme.token.TabChipToken
import com.ttcn.promotionsdk.ui.theme.token.TabUnderlineToken

/**
 * Cấu hình theme sau khi đã `PromotionSDK.init(...)`.
 * Đối ứng `PromotionSDK.configure(theme:)` / `PromotionSDK.currentTheme` bên iOS.
 */
object PromotionTheme {

    fun configure(theme: PromotionSDKTheme) {
        PromotionThemeRegistry.configure(theme)
        PromotionSDK.syncThemeConfig(theme)
    }

    fun currentTheme(): PromotionSDKTheme? = PromotionThemeRegistry.currentConfig()

    fun clear() {
        PromotionThemeRegistry.configure(null)
        PromotionSDK.syncThemeConfig(null)
    }

    // ─── Serialize ────────────────────────────────────────────────────────────

    /**
     * JSON hex `#AARRGGBB`, **dùng chung với iOS** (`PromotionSDKTheme.jsonString()`).
     * Đối tác ship một file theme cho cả hai nền tảng.
     */
    fun toJson(theme: PromotionSDKTheme): String = PromotionThemeJson.toJson(theme)

    /** Trả `null` nếu chuỗi không hợp lệ. */
    fun fromJson(json: String): PromotionSDKTheme? = PromotionThemeJson.fromJson(json)

    // ─── Preview / tài liệu ───────────────────────────────────────────────────

    /** Giá trị mặc định thật của SDK, để host dựng màn cấu hình theme. */
    fun sdkDefaults(context: Context): PromotionSDKTheme =
        PromotionThemeDefaults.defaultConfig(context)

    fun colorToHex(@ColorInt color: Int): String = PromotionThemeDefaults.colorToHex(color)

    fun pxToDp(context: Context, px: Float): Float = PromotionThemeDefaults.pxToDp(context, px)

    fun loadDisplayDefaults(context: Context): PromotionThemeDisplay.Defaults =
        PromotionThemeDisplay.load(context)

    fun mergeDisplayWithSaved(
        context: Context,
        sdk: PromotionThemeDisplay.Defaults,
        saved: PromotionSDKTheme?,
    ): PromotionThemeDisplay.Defaults =
        PromotionThemeDisplay.mergeWithSaved(context, sdk, saved)
}

typealias ThemeButtonToken = ButtonToken
typealias ThemeSearchBarToken = SearchBarToken
typealias ThemeListItemToken = ListItemToken
typealias ThemeTabChipToken = TabChipToken
typealias ThemeTabUnderlineToken = TabUnderlineToken
typealias ThemeDiscountBadgeToken = DiscountBadgeToken

/** Tên cũ. `PromotionThemeConfig` đã gộp vào [PromotionSDKTheme]. */
typealias ThemeConfig = PromotionSDKTheme
