package com.ttcn.promotionsdk.ui.entry

import com.ttcn.promotionsdk.ui.theme.PromotionSDKTheme

/**
 * @param theme `null` (mặc định) = SDK **tự khôi phục** theme đã lưu lần trước; host cấu hình một
 * lần rồi thôi. Truyền theme cụ thể = ghi đè và lưu lại. Xem [PromotionSDK.init].
 */
data class PromotionSDKOptions(
    val config: PromotionConfig,
    val theme: PromotionSDKTheme? = null,
    val callback: PromotionSDKCallback? = null
)
