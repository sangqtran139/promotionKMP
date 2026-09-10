package com.ttcn.promotionsdk.di

import android.content.Context
import android.content.pm.ApplicationInfo
import com.ttcn.promotionsdk.config.PromotionSDKConfig
import com.ttcn.promotionsdk.data.local.AndroidContextHolder

/**
 * Điểm khởi tạo **bắt buộc** trên Android, giữ chữ ký của `PromotionSDK.init(context, options)` cũ.
 * Làm hai việc mà bản common không làm được:
 *
 * 1. Nạp `applicationContext` cho `SharedPreferences` — nơi FeatureFlag cache cờ tính năng.
 * 2. Suy ra [PromotionSDKConfig.isDebug] từ `ApplicationInfo.FLAG_DEBUGGABLE`.
 *
 * Chỉ `applicationContext` được giữ; SDK không giữ Activity hay View context nào.
 * Gọi thẳng `initialize(config)` trên Android sẽ ném lỗi khi FeatureFlag cần tới storage.
 */
public fun PromotionContainer.initialize(context: Context, config: PromotionSDKConfig) {
    val appContext = context.applicationContext
    AndroidContextHolder.set(appContext)
    val isDebug = (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    initialize(config.copy(isDebug = isDebug))
}
