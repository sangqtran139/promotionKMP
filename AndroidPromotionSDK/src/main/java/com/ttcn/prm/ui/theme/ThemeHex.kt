package com.ttcn.prm.ui.theme

/**
 * Codec màu ↔ hex của theme. **Kotlin thuần, không `android.graphics`** — cùng một thuật toán với
 * `UIColor.hexString` / `UIColor(hex:)` bên iOS, nên JSON theme đọc được ở cả hai nền tảng.
 *
 * Định dạng: `#AARRGGBB` (alpha **trước**), rút gọn `#RRGGBB` khi màu đục.
 * Đầu vào chấp nhận cả 6 và 8 ký tự, có hoặc không có `#`.
 *
 * > Alpha đứng trước là quy ước của Android (`Color.parseColor`), **không** phải của CSS. Bản iOS
 * > trước đây ghi `#RRGGBBAA`, nên `#EE0033FF` (đỏ đục) đọc sang Android thành `alpha=EE, b=FF` —
 * > một màu xanh mờ, sai lặng lẽ. Đó là lý do file này tồn tại thay vì mỗi bên tự parse.
 */
internal object ThemeHex {

    /** `0xFFEE0033` → `"#EE0033"`; `0x80EE0033` → `"#80EE0033"`. */
    fun format(color: Int): String {
        val alpha = (color ushr 24) and 0xFF
        return if (alpha == 0xFF) {
            String.format("#%06X", color and 0xFFFFFF)
        } else {
            String.format("#%08X", color)
        }
    }

    /** `null` nếu chuỗi rỗng, sai độ dài, hoặc không phải hex. Màu 6 ký tự được coi là đục. */
    fun parse(hex: String?): Int? {
        val s = hex?.trim()?.removePrefix("#") ?: return null
        if (s.length != 6 && s.length != 8) return null
        val value = s.toLongOrNull(radix = 16) ?: return null
        return if (s.length == 8) value.toInt() else (0xFF000000L or value).toInt()
    }
}
