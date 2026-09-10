//
//  PRMLocalization.swift
//  PromotionKit
//
//  Chọn bảng chuỗi theo `PromotionSessionConfig.language` mà host truyền lúc `initialize`.
//
//  Vì sao KHÔNG dùng thẳng `NSLocalizedString(_:comment:)`: hàm đó tra trong `Bundle.main` — bundle
//  của **app host** — và theo ngôn ngữ **của thiết bị**. Cả hai đều sai với một SDK:
//
//  1. Chuỗi của SDK nằm trong bundle của framework, không nằm trong app host.
//  2. `language` là tham số public mà host truyền vào; nó phải quyết định chữ trên UI, chứ không
//     phải cài đặt ngôn ngữ của máy. Trước đây tham số đó **chỉ** đi xuống header của Ktor — tức là
//     một tham số public không làm điều mà tên nó nói.
//
//  Đối ứng `PRMLocale` bên Android — sửa bên nào thì sửa cả bên kia.
//

import Foundation

enum PRMLocalization {

    /// Ngôn ngữ SDK đang render. Đặt ở `PromotionSDK.initialize`, đọc ở `PromotionUIStrings`.
    ///
    /// `nonisolated(unsafe)`: chỉ ghi một lần trong `initialize` (đã `@MainActor`), sau đó chỉ đọc
    /// từ tầng UI — cũng trên main. Đánh dấu tường minh để `SWIFT_STRICT_CONCURRENCY` không phải đoán.
    nonisolated(unsafe) private(set) static var languageCode = defaultLanguageCode

    /// `"vi-VN"` — cùng giá trị mặc định với `PromotionSessionConfig.language` và với Android.
    static let defaultLanguageCode = "vi-VN"

    /// Bundle chứa `.lproj` của SDK. `Bundle(for:)` chứ không phải `.main`.
    private static let frameworkBundle = Bundle(for: PRMLocalizationToken.self)

    /// Bundle của đúng ngôn ngữ đang chọn; không có `.lproj` khớp thì lùi về bundle framework
    /// (tức là bảng của `defaultLocalization`) — SDK vẫn hiện chữ, không bao giờ hiện key trần.
    nonisolated(unsafe) private static var cachedBundle: Bundle?
    nonisolated(unsafe) private static var cachedForLanguage: String?

    static var bundle: Bundle {
        if cachedForLanguage == languageCode, let cached = cachedBundle { return cached }
        let resolved = resolveBundle(for: languageCode)
        cachedBundle = resolved
        cachedForLanguage = languageCode
        return resolved
    }

    /// Locale dùng cho `NumberFormatter`/`DateFormatter` — dấu phân cách nghìn phải theo ngôn ngữ,
    /// không hardcode `"."`.
    static var locale: Locale { Locale(identifier: languageCode) }

    /// Gọi từ `PromotionSDK.initialize`. Nhận đúng chuỗi host truyền (`"vi-VN"`, `"en"`, `"en-US"`…).
    static func configure(languageCode: String) {
        let trimmed = languageCode.trimmingCharacters(in: .whitespacesAndNewlines)
        self.languageCode = trimmed.isEmpty ? defaultLanguageCode : trimmed
    }

    /// `"vi-VN"` → thử `vi-VN.lproj`, rồi `vi.lproj`. Apple đặt tên thư mục theo mã ngôn ngữ ngắn
    /// (`vi.lproj`), còn host thì hay truyền mã đầy đủ — không thử cả hai là luôn trượt.
    ///
    /// Ngôn ngữ không có bảng (`"fr-FR"`) thì lùi về **`vi.lproj`**, và phải lùi *tường minh*:
    /// trả thẳng `frameworkBundle` là giao việc chọn ngôn ngữ lại cho `Bundle`, mà `Bundle` chọn
    /// theo **ngôn ngữ của thiết bị** — máy đang để tiếng Anh thì SDK hiện tiếng Anh dù host truyền
    /// `"fr-FR"`, tức là `language` lại một lần nữa không quyết định được chữ trên UI. Test
    /// `test_ngonNguLa_luiVeTiengViet` canh đúng chỗ này.
    private static func resolveBundle(for language: String) -> Bundle {
        var candidates = [language, shortCode(of: language)]
        candidates += [defaultLanguageCode, shortCode(of: defaultLanguageCode)]
        for candidate in candidates {
            if let path = frameworkBundle.path(forResource: candidate, ofType: "lproj"),
               let localized = Bundle(path: path) {
                return localized
            }
        }
        return frameworkBundle
    }

    /// `"vi-VN"` → `"vi"`, `"en_US"` → `"en"`.
    private static func shortCode(of language: String) -> String {
        String(language.prefix(while: { $0 != "-" && $0 != "_" }))
    }
}

/// Chỉ để `Bundle(for:)` tìm ra framework của SDK. Không có thành viên nào.
private final class PRMLocalizationToken {}
