package com.ttcn.promotionsdk.core.data.local

/**
 * Kho key-value đồng bộ, thay cho `SharedPrefStorage` (JVM-only) của bản Android.
 * Android dùng `SharedPreferences`, iOS dùng `NSUserDefaults` — cùng ngữ nghĩa ghi ngay, đọc đồng bộ.
 *
 * Chỉ có API boolean vì FeatureFlag là nhu cầu duy nhất; mở rộng khi thật sự cần.
 */
internal interface KeyValueStorage {
    fun putBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, default: Boolean = false): Boolean
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
internal expect fun createKeyValueStorage(): KeyValueStorage
