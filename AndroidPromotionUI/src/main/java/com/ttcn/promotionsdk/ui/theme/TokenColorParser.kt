package com.ttcn.promotionsdk.ui.theme

import androidx.annotation.ColorInt

object TokenColorParser {

    /**
     * Parse `#AARRGGBB` / `#RRGGBB` (có hoặc không có `#`). `null` nếu chuỗi không hợp lệ.
     *
     * Trước đây gọi `Color.parseColor`, vốn còn nhận cả tên màu (`"red"`, `"cyan"`). Nay uỷ quyền
     * cho [ThemeHex] — cùng thuật toán với iOS, và không kéo `android.graphics` vào đường serialize
     * theme (nhờ vậy `PromotionThemeJson` test được bằng unit test JVM thường).
     */
    @ColorInt
    fun parse(hex: String?): Int? = ThemeHex.parse(hex)
}
