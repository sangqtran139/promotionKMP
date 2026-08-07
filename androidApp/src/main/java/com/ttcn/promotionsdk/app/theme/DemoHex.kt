package com.ttcn.promotionsdk.app.theme

/**
 * Codec màu ↔ hex **phía host**. Cùng định dạng với JSON theme của SDK — `#AARRGGBB`, alpha đứng
 * **trước** (quy ước `Color.parseColor` của Android, không phải CSS); 6 ký tự coi như đục.
 *
 * SDK có `ThemeHex` cùng thuật toán nhưng đó là nội bộ (`internal`) — host không với tới và cũng
 * không cần: hex chỉ là chuyện của màn cấu hình theme bên app demo. SDK nhận vào `@ColorInt Int`
 * qua token, hoặc nguyên chuỗi JSON qua `PromotionThemeJson`.
 */
internal object DemoHex {

    /** `0xFFEE0033` → `"#EE0033"`; `0x80EE0033` → `"#80EE0033"`. */
    fun format(color: Int): String {
        val alpha = (color ushr 24) and 0xFF
        return if (alpha == 0xFF) {
            String.format("#%06X", color and 0xFFFFFF)
        } else {
            String.format("#%08X", color)
        }
    }

    /** `null` nếu chuỗi rỗng, sai độ dài, hoặc không phải hex. */
    fun parse(hex: String?): Int? {
        val s = hex?.trim()?.removePrefix("#") ?: return null
        if (s.length != 6 && s.length != 8) return null
        val value = s.toLongOrNull(radix = 16) ?: return null
        return if (s.length == 8) value.toInt() else (0xFF000000L or value).toInt()
    }
}
