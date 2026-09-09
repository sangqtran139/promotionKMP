package com.ttcn.prm.ui.utils

import android.util.Log
import com.ttcn.prm.BuildConfig

/**
 * Log nội bộ của SDK — thay cho Timber.
 *
 * **Vì sao bỏ Timber.** SDK gọi `Timber.x()` 12 chỗ nhưng **chưa bao giờ** gọi `Timber.plant()`.
 * Timber không có cây nào thì mọi lời gọi im lặng rơi vào hư không — tức 12 chỗ log đó chưa từng in
 * ra gì. Và nếu SDK *có* plant một cây, nó sẽ ghi vào cây **toàn cục** dùng chung với app host: log
 * của SDK trộn vào hệ thống log của host, host tắt/định tuyến kiểu gì thì SDK chịu kiểu đó. Cả hai
 * đằng đều sai với một thư viện.
 *
 * Kèm theo đó, plugin lint của Timber (`timber.lint.WrongTimberUsageDetector`) đang **crash** trên
 * repo này, làm `lintRelease` fail vì lỗi của chính detector chứ không phải lỗi code.
 *
 * `android.util.Log` gác bằng [BuildConfig.DEBUG]: bản release của SDK không in gì, kể cả khi host
 * build debug — `BuildConfig` ở đây là của **module SDK**, cố định theo bản AAR đã publish.
 *
 * Đối ứng `PRMLog` bên iOS (`os_log`, không phải `NSLog`).
 */
internal object PRMLog {

    /** Một tag cho cả SDK: dev của host lọc `adb logcat -s PromotionSDK` là ra đúng phần của SDK. */
    private const val TAG = "PromotionSDK"

    fun d(scope: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, "[$scope] $message")
    }

    fun w(scope: String, message: String) {
        if (BuildConfig.DEBUG) Log.w(TAG, "[$scope] $message")
    }

    /**
     * Lỗi thì in **cả bản release**: đây là thứ dev của host cần khi đi tìm nguyên nhân, và nó hiếm
     * nên không làm ngập logcat. Không kèm dữ liệu người dùng — chỉ thông điệp và stack trace.
     */
    fun e(scope: String, message: String, error: Throwable? = null) {
        Log.e(TAG, "[$scope] $message", error)
    }
}
