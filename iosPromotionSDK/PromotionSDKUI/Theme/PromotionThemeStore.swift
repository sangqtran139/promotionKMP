//
//  PromotionThemeStore.swift
//  PRMSDK
//
//  Persist theme: serialize PRMSDKTheme → JSON rồi lưu qua `PromotionContainer.preferences`
//  của lõi (cùng cơ chế `PromotionPreferences` mà Android dùng). Tầng này giữ **key** và việc
//  serialize; lõi giữ cơ chế lưu. Đối ứng `PromotionThemeStore.kt`.
//

import Foundation
@_implementationOnly import PRMKotlinBridge

enum PromotionThemeStore {
    private static let keyTheme = "promotion_theme_config_v1"

    static func save(_ theme: PRMSDKTheme) {
        PromotionContainer.shared.preferences.putString(key: keyTheme, value: PRMThemeJson.toJson(theme))
    }

    static func load() -> PRMSDKTheme? {
        guard let json = PromotionContainer.shared.preferences.getString(key: keyTheme) else { return nil }
        return PRMThemeJson.fromJson(json)
    }

    static func clear() {
        PromotionContainer.shared.preferences.remove(key: keyTheme)
    }
}
