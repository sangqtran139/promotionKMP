package com.ttcn.promotionsdk.core.data.local

/**
 * Kho key-value đồng bộ **duy nhất** của SDK — một cơ chế cho cả hai nền tảng (Android
 * `SharedPreferences`, iOS `NSUserDefaults`), cùng ngữ nghĩa ghi ngay, đọc đồng bộ.
 *
 * Vừa là chỗ FeatureFlag cache cờ (boolean), vừa là chỗ tầng UI persist cấu hình như theme (string).
 * Có sẵn cả boolean lẫn string nên **mở rộng được**: sau này UI cần lưu thêm boolean thì dùng ngay,
 * không phải thêm type mới. Tầng UI lấy instance qua `PromotionContainer.preferences` — giống nhau
 * trên Android và iOS.
 *
 * Public vì tầng UI nằm ở **module khác** (AndroidPromotionSDK / PromotionSDKUI); các `actual`
 * (`SharedPrefStorage` / `UserDefaultsStorage`) vẫn nội bộ.
 */
interface PromotionPreferences {
    fun putBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, default: Boolean = false): Boolean
    fun putString(key: String, value: String)
    fun getString(key: String): String?
    fun contains(key: String): Boolean
    fun remove(key: String)
    fun clear()

    companion object {
        const val PREFS_NAME = "promotion_sdk_prefs"
    }
}

/**
 * Trên Android cần `Context`, được nạp qua `PromotionContainer.initialize(context, config)`.
 * Gọi `initialize(config)` (bản common) trên Android mà không có context sẽ ném lỗi rõ ràng ở đây.
 */
internal expect fun createPreferences(): PromotionPreferences
