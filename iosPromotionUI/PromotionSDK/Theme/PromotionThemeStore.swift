//
//  PromotionThemeStore.swift
//  PromotionSDK
//
//  Persist theme: serialize PromotionSDKTheme → JSON rồi lưu qua `PromotionContainer.preferences`
//  của lõi (cùng cơ chế `PromotionPreferences` mà Android dùng). Tầng này giữ **key** và việc
//  serialize; lõi giữ cơ chế lưu. Đối ứng `PromotionThemeStore.kt`.
//

import Foundation
@_implementationOnly import PromotionKit

enum PromotionThemeStore {
    private static let keyTheme = "promotion_theme_config_v1"

    static func save(_ theme: PromotionSDKTheme) {
        PromotionContainer.shared.preferences.putString(key: keyTheme, value: PromotionThemeJson.toJson(theme))
    }

    static func load() -> PromotionSDKTheme? {
        guard let json = PromotionContainer.shared.preferences.getString(key: keyTheme) else { return nil }
        return PromotionThemeJson.fromJson(json)
    }

    static func clear() {
        PromotionContainer.shared.preferences.remove(key: keyTheme)
    }
}
