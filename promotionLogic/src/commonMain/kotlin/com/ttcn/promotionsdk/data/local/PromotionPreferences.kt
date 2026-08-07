package com.ttcn.promotionsdk.data.local

import com.russhwolf.settings.Settings

/**
 * Kho key-value đồng bộ **duy nhất** của SDK — một cơ chế cho cả hai nền tảng (Android
 * `SharedPreferences`, iOS `NSUserDefaults`), cùng ngữ nghĩa ghi ngay, đọc đồng bộ.
 *
 * Vừa là chỗ FeatureFlag cache cờ (boolean), vừa là chỗ tầng UI persist cấu hình như theme (string).
 * Có sẵn cả boolean lẫn string nên **mở rộng được**: sau này UI cần lưu thêm boolean thì dùng ngay,
 * không phải thêm type mới. Tầng UI lấy instance qua `PromotionContainer.preferences` — giống nhau
 * trên Android và iOS.
 *
 * Public vì tầng UI nằm ở **module khác** (AndroidPromotionSDK / PromotionSDKUI); hiện thực
 * ([SettingsPreferences]) và hàm dựng nó vẫn nội bộ.
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
 * Hiện thực **duy nhất** của [PromotionPreferences], dùng chung cho cả hai nền tảng: `Settings` của
 * multiplatform-settings đã bọc sẵn `SharedPreferences` và `NSUserDefaults` sau một API đồng bộ, nên
 * phần get/put/remove chỉ còn viết một lần ở đây thay vì hai lần trong `androidMain` + `iosMain`.
 *
 * Cái mà `expect/actual` còn phải lo là **dựng delegate** — chỗ duy nhất thật sự khác nhau giữa hai
 * nền tảng (một bên cần `Context`, một bên cần suite name).
 *
 * `Settings` cố tình **không** rò ra ngoài: [PromotionPreferences] mới là kiểu mà tầng UI và Swift
 * nhìn thấy, nên đổi thư viện sau này không thành breaking change cho host.
 */
internal class SettingsPreferences(private val settings: Settings) : PromotionPreferences {

    override fun putBoolean(key: String, value: Boolean) {
        settings.putBoolean(key, value)
    }

    override fun getBoolean(key: String, default: Boolean): Boolean =
        settings.getBoolean(key, default)

    override fun putString(key: String, value: String) {
        settings.putString(key, value)
    }

    override fun getString(key: String): String? = settings.getStringOrNull(key)

    override fun contains(key: String): Boolean = settings.hasKey(key)

    override fun remove(key: String) {
        settings.remove(key)
    }

    override fun clear() {
        settings.clear()
    }
}

/**
 * Trên Android cần `Context`, được nạp qua `PromotionContainer.initialize(context, config)`.
 * Gọi `initialize(config)` (bản common) trên Android mà không có context sẽ ném lỗi rõ ràng ở đây.
 */
internal expect fun createPreferences(): PromotionPreferences
