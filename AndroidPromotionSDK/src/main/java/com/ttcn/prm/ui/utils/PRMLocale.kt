package com.ttcn.prm.ui.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Chọn ngôn ngữ hiển thị của SDK theo [com.ttcn.prm.entry.PromotionSessionConfig.language].
 *
 * **Vì sao SDK phải tự bọc context.** `getString()` lấy chuỗi theo locale của **cấu hình app host**,
 * tức là theo cài đặt máy. Nhưng `language` là tham số public host truyền vào — nó phải quyết định
 * chữ trên màn SDK. Trước đây tham số đó **chỉ** đi xuống header của Ktor: một tham số public không
 * làm điều mà tên nó nói, và không có gì phát hiện ra.
 *
 * **Vì sao KHÔNG đụng `Locale.setDefault` hay cấu hình của Activity host.** SDK sống trong tiến
 * trình của host; đổi locale toàn cục là đổi ngôn ngữ **cả app của host** — tác dụng phụ mà không
 * SDK nào được phép gây ra. [wrap] tạo một context riêng, chỉ dùng để inflate màn của SDK.
 *
 * Đối ứng `PRMLocalization` bên iOS — sửa bên nào thì sửa cả bên kia.
 */
internal object PRMLocale {

    /** `"vi-VN"` — trùng mặc định của `PromotionSessionConfig.language` và của iOS. */
    const val DEFAULT_LANGUAGE = "vi-VN"

    @Volatile
    private var languageTag: String = DEFAULT_LANGUAGE

    /** Gọi từ `PromotionSDK.initialize`. Chuỗi rỗng → về mặc định. */
    @JvmStatic
    fun configure(language: String?) {
        languageTag = language?.trim().takeUnless { it.isNullOrEmpty() } ?: DEFAULT_LANGUAGE
    }

    /**
     * Locale đang dùng — cho cả `getString` lẫn phần format số/ngày.
     *
     * `forLanguageTag` nhận `"vi-VN"`, `"en"`, `"en-US"`. Tag không hợp lệ thì nó trả locale rỗng
     * (`""`), lúc đó lùi về mặc định thay vì để `getString` rơi về locale của máy.
     */
    @JvmStatic
    fun current(): Locale {
        val locale = Locale.forLanguageTag(languageTag)
        return if (locale.language.isEmpty()) Locale.forLanguageTag(DEFAULT_LANGUAGE) else locale
    }

    /**
     * Context đã gắn locale của SDK. Dùng để inflate layout và gọi `getString` trong màn SDK.
     *
     * Không có `values-<lang>` khớp thì Android tự lùi về `values/` (tiếng Việt) — không bao giờ
     * hiện tên resource lên màn hình.
     */
    @JvmStatic
    fun wrap(context: Context): Context {
        val locale = current()
        val config = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(config)
    }
}
