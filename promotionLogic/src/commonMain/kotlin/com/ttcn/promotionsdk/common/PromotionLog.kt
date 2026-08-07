package com.ttcn.promotionsdk.common

/**
 * Log cảnh báo của lõi — **luôn in**, không gác sau `isDebug`.
 *
 * Dành riêng cho những tình huống lõi *nuốt lỗi có chủ đích* để SDK chạy tiếp (vd feature flag hỏng
 * → giữ cache). Nuốt mà im hoàn toàn là cách hỏng tệ nhất: tính năng cư xử sai mà không ai có manh
 * mối nào để lần. Một dòng log là đủ để người tích hợp biết chỗ mà nhìn.
 *
 * `println` chứ không phải Timber/OSLog: lõi là KMP, không được phụ thuộc thư viện của một nền tảng.
 * Android đọc ở logcat (tag `System.out`), iOS đọc ở console Xcode. Cùng cách mà
 * `PromotionCurlLogging` đang dùng.
 */
internal fun promotionWarn(message: String) {
    runCatching { println("$TAG $message") }
}

private const val TAG = "[PromotionSDK][WARN]"
