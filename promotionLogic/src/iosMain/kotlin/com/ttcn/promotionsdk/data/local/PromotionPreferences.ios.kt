package com.ttcn.promotionsdk.data.local

import com.russhwolf.settings.NSUserDefaultsSettings
import platform.Foundation.NSUserDefaults

/**
 * Dùng một suite riêng (`promotion_sdk_prefs`) thay vì `standardUserDefaults`, để `clear()` không
 * xoá nhầm preference của app host — tương đương việc Android mở file prefs riêng. Đây cũng là lý do
 * **không** dùng `multiplatform-settings-no-arg`: bản đó gắn cứng `standardUserDefaults`.
 * Xem docs/common/StorageGuide.md §2.1.
 *
 * `NSUserDefaults(suiteName:)` trả null nếu suite name trùng tên miền của app hoặc là một domain
 * global; rơi về `standardUserDefaults` để SDK vẫn chạy thay vì crash lúc khởi tạo.
 */
internal actual fun createPreferences(): PromotionPreferences = SettingsPreferences(
    NSUserDefaultsSettings(
        NSUserDefaults(suiteName = PromotionPreferences.PREFS_NAME)
            ?: NSUserDefaults.standardUserDefaults
    )
)
