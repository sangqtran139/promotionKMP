package com.ttcn.promotionsdk.app.theme

import android.content.Context
import android.graphics.Color
import com.ttcn.prm.ui.theme.PromotionSDKTheme
import com.ttcn.prm.ui.theme.PromotionThemeJson
import com.ttcn.prm.ui.theme.token.ButtonToken
import com.ttcn.prm.ui.theme.token.DiscountBadgeToken
import com.ttcn.prm.ui.theme.token.ListItemToken
import com.ttcn.prm.ui.theme.token.SearchBarToken
import com.ttcn.prm.ui.theme.token.TabChipToken
import com.ttcn.prm.ui.theme.token.TabUnderlineToken

/**
 * Hai cách host lấy một [PromotionSDKTheme] — đối ứng `DemoThemeSource.swift` bên iOS.
 *
 * - [fromJsonFile]: đọc `assets/promotion_theme.json` (**cùng một file** với bản trong bundle iOS)
 *   rồi parse bằng [PromotionThemeJson]. Đây là câu trả lời cho "SDK dùng file JSON được không": được,
 *   một file dùng chung hai nền tảng.
 * - [fromObject]: dựng token bằng code, không qua JSON.
 *
 * Cả hai trả về cùng một bộ màu (teal `#2CA196`) để so sánh hai đường đi cho ra kết quả giống nhau.
 */
object DemoThemeSource {

    const val ASSET_NAME = "promotion_theme.json"

    /** `null` nếu file thiếu hoặc JSON hỏng — host tự quyết định giữ theme cũ. */
    fun fromJsonFile(context: Context): PromotionSDKTheme? =
        runCatching { context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() } }
            .getOrNull()
            ?.let(PromotionThemeJson::fromJson)

    /** Cùng bộ màu với [fromJsonFile], nhưng viết thẳng bằng token — không đụng JSON. */
    fun fromObject(): PromotionSDKTheme {
        val teal = Color.parseColor("#2CA196")
        val tealLight = Color.parseColor("#EAF6F4")
        val white = Color.parseColor("#FFFFFF")
        val grey = Color.parseColor("#7A7A7A")
        val greyLight = Color.parseColor("#F4F4F4")

        return PromotionSDKTheme(
            buttonToken = ButtonToken(
                backgroundColor = teal,
                textColor = white,
                shadowColor = Color.parseColor("#332CA196"),
                cornerRadius = 12f,
            ),
            searchBarToken = SearchBarToken(
                borderColor = teal,
                hintTextColor = grey,
                textColor = Color.parseColor("#222222"),
                iconColor = teal,
                cornerRadius = 10f,
            ),
            listItemToken = ListItemToken(
                linkTextColor = teal,
                usedBadgeTextColor = grey,
                usedBadgeBackgroundColor = greyLight,
                radioButtonStrokeColor = Color.parseColor("#A7A7A7"),
                radioButtonSelectedStrokeColor = teal,
            ),
            tabChipToken = TabChipToken(
                activeBackgroundColor = teal,
                inactiveBackgroundColor = tealLight,
                activeTextColor = white,
                inactiveTextColor = Color.parseColor("#4E4E4E"),
                cornerRadius = 16f,
            ),
            tabUnderlineToken = TabUnderlineToken(
                indicatorColor = teal,
                activeTextColor = teal,
                inactiveTextColor = grey,
                backgroundColor = white,
            ),
            discountBadgeToken = DiscountBadgeToken(
                availableTextColor = teal,
                unavailableTextColor = Color.parseColor("#A7A7A7"),
                availableBackgroundColor = tealLight,
                unavailableBackgroundColor = greyLight,
                actionTextColor = teal,
            ),
        )
    }
}
