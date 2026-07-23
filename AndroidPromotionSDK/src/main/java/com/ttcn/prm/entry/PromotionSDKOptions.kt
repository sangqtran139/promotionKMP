package com.ttcn.prm.entry

import com.ttcn.prm.ui.theme.PromotionSDKTheme

/**
 * @param theme `null` (mặc định) = SDK **tự khôi phục** theme đã lưu lần trước; host cấu hình một
 * lần rồi thôi. Truyền theme cụ thể = ghi đè và lưu lại. Xem [PromotionSDK.initialize].
 */
data class PromotionSDKOptions(
    val session: PromotionSessionConfig,
    val availableServices: List<PromotionAvailableService> = emptyList(),
    val theme: PromotionSDKTheme? = null,
    val callback: PromotionSDKCallback? = null,
)
