package com.ttcn.promotionsdk.entry

import com.ttcn.promotionsdk.promotionsdkui.theme.PRMSDKTheme

/**
 * @param theme `null` (mặc định) = SDK **tự khôi phục** theme đã lưu lần trước; host cấu hình một
 * lần rồi thôi. Truyền theme cụ thể = ghi đè và lưu lại. Xem [PRMSDK.initialize].
 */
data class PRMSDKOptions(
    val session: PRMSessionConfig,
    val availableServices: List<PRMAvailableService> = emptyList(),
    val theme: PRMSDKTheme? = null,
    val callback: PRMSDKCallback? = null,
)
