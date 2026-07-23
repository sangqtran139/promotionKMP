package com.ttcn.promotionsdk.promotionsdkui.theme.applytoken

import androidx.annotation.ColorInt
import com.ttcn.promotionsdk.promotionsdkui.theme.ThemeHex

object PRMTokenColorParser {

    /**
     * Parse `#AARRGGBB` / `#RRGGBB` (có hoặc không có `#`). `null` nếu chuỗi không hợp lệ.
     *
     * Trước đây gọi `Color.parseColor`, vốn còn nhận cả tên màu (`"red"`, `"cyan"`). Nay uỷ quyền
     * cho [com.ttcn.promotionsdk.promotionsdkui.theme.ThemeHex] — cùng thuật toán với iOS, và không kéo `android.graphics` vào đường serialize
     * theme (nhờ vậy `PRMThemeJson` test được bằng unit test JVM thường).
     */
    @ColorInt
    fun parse(hex: String?): Int? = ThemeHex.parse(hex)
}
