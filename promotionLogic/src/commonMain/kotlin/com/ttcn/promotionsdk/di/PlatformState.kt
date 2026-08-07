package com.ttcn.promotionsdk.di

/**
 * Dọn state riêng của nền tảng khi [PromotionContainer.clear] chạy.
 * Android: nhả `applicationContext`. iOS: không có gì để nhả.
 */
internal expect fun clearPlatformState()
