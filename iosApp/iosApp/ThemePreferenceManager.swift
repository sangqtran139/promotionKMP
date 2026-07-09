//
//  ThemePreferenceManager.swift
//  VDSPromotionDemoApp
//
//  Lưu/đọc theme bằng UserDefaults (mirror ThemePreferenceManager bên Android — persistence
//  nằm ở APP, dùng serializer của SDK). File giữ NHỎ vì có import PromotionSDKUI.
//

import Foundation
import PromotionSDKUI

final class ThemePreferenceManager {

    static let shared = ThemePreferenceManager()

    private let defaults = UserDefaults.standard
    private let key = "promotion_theme_config"

    func save(_ theme: PromotionSDKTheme) {
        guard let json = theme.jsonString() else { return }
        defaults.set(json, forKey: key)
    }

    func load() -> PromotionSDKTheme? {
        guard let json = defaults.string(forKey: key) else { return nil }
        return PromotionSDKTheme.from(jsonString: json)
    }

    func clear() {
        defaults.removeObject(forKey: key)
    }
}
