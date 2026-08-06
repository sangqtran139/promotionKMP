//
//  ThemeHex.swift
//  PromotionSDK
//
//  Codec màu ↔ hex `#AARRGGBB`. Đối ứng `ThemeHex.kt` bên Android — cùng tên type (`ThemeHex`),
//  cùng hàm `format` / `parse`, cùng thuật toán, nên JSON theme đọc được ở cả hai nền tảng.
//
//  Alpha đứng **trước** (`#AARRGGBB`), theo quy ước Android (`Color.parseColor`) — không phải CSS.
//

import UIKit

enum ThemeHex {

    /// `#AARRGGBB`, rút gọn `#RRGGBB` khi màu đục. `nil` in → `nil` out.
    /// (Android `format(Int): String` không nhận null; Swift để optional cho gọn ở chỗ gọi.)
    static func format(_ color: UIColor?) -> String? {
        guard let color else { return nil }
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        color.getRed(&r, green: &g, blue: &b, alpha: &a)
        let clamp: (CGFloat) -> Int = { Int((max(0, min(1, $0)) * 255).rounded()) }
        let alpha = clamp(a)
        if alpha == 0xFF {
            return String(format: "#%02X%02X%02X", clamp(r), clamp(g), clamp(b))
        }
        return String(format: "#%02X%02X%02X%02X", alpha, clamp(r), clamp(g), clamp(b))
    }

    /// Parse `#AARRGGBB` hoặc `#RRGGBB` (coi là đục). `nil` nếu chuỗi rỗng/sai.
    static func parse(_ hex: String?) -> UIColor? {
        guard var s = hex?.trimmingCharacters(in: .whitespaces), !s.isEmpty else { return nil }
        if s.hasPrefix("#") { s.removeFirst() }
        guard s.count == 6 || s.count == 8, let value = UInt64(s, radix: 16) else { return nil }
        let r, g, b, a: CGFloat
        if s.count == 8 {
            a = CGFloat((value & 0xFF000000) >> 24) / 255
            r = CGFloat((value & 0x00FF0000) >> 16) / 255
            g = CGFloat((value & 0x0000FF00) >> 8) / 255
            b = CGFloat(value & 0x000000FF) / 255
        } else {
            r = CGFloat((value & 0xFF0000) >> 16) / 255
            g = CGFloat((value & 0x00FF00) >> 8) / 255
            b = CGFloat(value & 0x0000FF) / 255
            a = 1
        }
        return UIColor(red: r, green: g, blue: b, alpha: a)
    }
}
