package com.ttcn.promotionsdk.ui.theme

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ttcn.promotionsdk.ui.theme.token.ButtonToken
import com.ttcn.promotionsdk.ui.theme.token.DiscountBadgeToken
import com.ttcn.promotionsdk.ui.theme.token.ListItemToken
import com.ttcn.promotionsdk.ui.theme.token.SearchBarToken
import com.ttcn.promotionsdk.ui.theme.token.TabChipToken
import com.ttcn.promotionsdk.ui.theme.token.TabUnderlineToken

/**
 * Serialize [PromotionSDKTheme] ↔ JSON. **Một định dạng duy nhất, dùng chung Android và iOS** —
 * đối ứng phần `ThemeDTO` trong `PromotionSDKTheme.swift`. Đối tác ship một file theme cho cả hai.
 *
 * Màu là chuỗi hex **`#AARRGGBB`** (alpha **trước**), rút gọn còn `#RRGGBB` khi màu đục — đúng khuôn
 * `Color.parseColor`. `cornerRadius` là số, đơn vị **dp** (iOS đọc thành pt).
 *
 * ```json
 * { "button": { "backgroundColor": "#FFEE0033", "cornerRadius": 8.0 } }
 * ```
 *
 * Nhóm vắng mặt = giữ mặc định SDK cho nhóm đó, nên mọi field của DTO đều nullable.
 *
 * **Hàm này từng hỏng thật.** Nó `gson.toJson(config)` thẳng, mà field màu là `Int?`, nên sinh ra
 * `{"buttonToken":{"backgroundColor":-1030080}}` — không key nào, không kiểu nào khớp `ThemeDTO` của
 * iOS, dù comment bên Swift ghi "mirror Android PromotionThemeJson". Đưa JSON bên này sang bên kia
 * thì parse hỏng, `runCatching` nuốt lỗi và trả `null`: **theme biến mất trong im lặng**.
 */
object PromotionThemeJson {

    private val gson: Gson = GsonBuilder().create()

    fun toJson(theme: PromotionSDKTheme): String = gson.toJson(theme.toDto())

    fun fromJson(json: String): PromotionSDKTheme? =
        runCatching { gson.fromJson(json, ThemeDto::class.java)?.toTheme() }.getOrNull()
}

// ─── DTO ────────────────────────────────────────────────────────────────────
//
// Gson dựng object bằng `Unsafe`, không gọi constructor — nên mọi field phải nullable và việc map
// nằm ở hàm riêng, không ở constructor.

private data class ThemeDto(
    val button: ButtonDto? = null,
    val searchBar: SearchBarDto? = null,
    val listItem: ListItemDto? = null,
    val tabChip: TabChipDto? = null,
    val tabUnderline: TabUnderlineDto? = null,
    val discountBadge: DiscountBadgeDto? = null,
)

private data class ButtonDto(
    val backgroundColor: String? = null,
    val textColor: String? = null,
    val shadowColor: String? = null,
    val cornerRadius: Float? = null,
)

private data class SearchBarDto(
    val borderColor: String? = null,
    val hintTextColor: String? = null,
    val textColor: String? = null,
    val iconColor: String? = null,
    val cornerRadius: Float? = null,
)

private data class ListItemDto(
    val linkTextColor: String? = null,
    val usedBadgeTextColor: String? = null,
    val usedBadgeBackgroundColor: String? = null,
    val radioButtonStrokeColor: String? = null,
    val radioButtonSelectedStrokeColor: String? = null,
)

private data class TabChipDto(
    val activeBackgroundColor: String? = null,
    val inactiveBackgroundColor: String? = null,
    val activeTextColor: String? = null,
    val inactiveTextColor: String? = null,
    val cornerRadius: Float? = null,
)

private data class TabUnderlineDto(
    val indicatorColor: String? = null,
    val activeTextColor: String? = null,
    val inactiveTextColor: String? = null,
    val backgroundColor: String? = null,
)

private data class DiscountBadgeDto(
    val availableTextColor: String? = null,
    val unavailableTextColor: String? = null,
    val availableBackgroundColor: String? = null,
    val unavailableBackgroundColor: String? = null,
    val actionTextColor: String? = null,
)

// ─── Token → DTO ────────────────────────────────────────────────────────────

private fun hex(color: Int?): String? = color?.let(ThemeHex::format)

private fun color(hex: String?): Int? = ThemeHex.parse(hex)

private fun PromotionSDKTheme.toDto() = ThemeDto(
    button = buttonToken?.let {
        ButtonDto(hex(it.backgroundColor), hex(it.textColor), hex(it.shadowColor), it.cornerRadius)
    },
    searchBar = searchBarToken?.let {
        SearchBarDto(
            hex(it.borderColor), hex(it.hintTextColor), hex(it.textColor), hex(it.iconColor),
            it.cornerRadius,
        )
    },
    listItem = listItemToken?.let {
        ListItemDto(
            hex(it.linkTextColor), hex(it.usedBadgeTextColor), hex(it.usedBadgeBackgroundColor),
            hex(it.radioButtonStrokeColor), hex(it.radioButtonSelectedStrokeColor),
        )
    },
    tabChip = tabChipToken?.let {
        TabChipDto(
            hex(it.activeBackgroundColor), hex(it.inactiveBackgroundColor),
            hex(it.activeTextColor), hex(it.inactiveTextColor), it.cornerRadius,
        )
    },
    tabUnderline = tabUnderlineToken?.let {
        TabUnderlineDto(
            hex(it.indicatorColor), hex(it.activeTextColor), hex(it.inactiveTextColor),
            hex(it.backgroundColor),
        )
    },
    discountBadge = discountBadgeToken?.let {
        DiscountBadgeDto(
            hex(it.availableTextColor), hex(it.unavailableTextColor),
            hex(it.availableBackgroundColor), hex(it.unavailableBackgroundColor),
            hex(it.actionTextColor),
        )
    },
)

// ─── DTO → Token ────────────────────────────────────────────────────────────

private fun ThemeDto.toTheme() = PromotionSDKTheme(
    buttonToken = button?.let {
        ButtonToken(
            color(it.backgroundColor), color(it.textColor), color(it.shadowColor), it.cornerRadius,
        )
    },
    searchBarToken = searchBar?.let {
        SearchBarToken(
            color(it.borderColor), color(it.hintTextColor), color(it.textColor),
            color(it.iconColor), it.cornerRadius,
        )
    },
    listItemToken = listItem?.let {
        ListItemToken(
            color(it.linkTextColor), color(it.usedBadgeTextColor),
            color(it.usedBadgeBackgroundColor), color(it.radioButtonStrokeColor),
            color(it.radioButtonSelectedStrokeColor),
        )
    },
    tabChipToken = tabChip?.let {
        TabChipToken(
            color(it.activeBackgroundColor), color(it.inactiveBackgroundColor),
            color(it.activeTextColor), color(it.inactiveTextColor), it.cornerRadius,
        )
    },
    tabUnderlineToken = tabUnderline?.let {
        TabUnderlineToken(
            color(it.indicatorColor), color(it.activeTextColor), color(it.inactiveTextColor),
            color(it.backgroundColor),
        )
    },
    discountBadgeToken = discountBadge?.let {
        DiscountBadgeToken(
            color(it.availableTextColor), color(it.unavailableTextColor),
            color(it.availableBackgroundColor), color(it.unavailableBackgroundColor),
            color(it.actionTextColor),
        )
    },
)
