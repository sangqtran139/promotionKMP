package com.ttcn.prm.ui.theme.applytoken

import androidx.annotation.ColorInt
import com.ttcn.prm.ui.theme.ThemeHex

internal object TokenColorParser {

    /**
     * Parse `#AARRGGBB` / `#RRGGBB` (có hoặc không có `#`). `null` nếu chuỗi không hợp lệ.
     *
     * Uỷ quyền cho [com.ttcn.prm.ui.theme.ThemeHex] — Kotlin thuần, cùng thuật toán với iOS, không
     * chạm `android.graphics`. Chỉ nhận chuỗi hex; tên màu (`"red"`, `"cyan"`) trả `null`.
     */
    @ColorInt
    fun parse(hex: String?): Int? = ThemeHex.parse(hex)
}
