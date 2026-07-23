package com.ttcn.promotionsdk.promotionsdkui.theme

import com.ttcn.promotionsdk.promotionsdkui.theme.token.PRMButtonToken
import com.ttcn.promotionsdk.promotionsdkui.theme.token.PRMDiscountBadgeToken
import com.ttcn.promotionsdk.promotionsdkui.theme.token.PRMListItemToken
import com.ttcn.promotionsdk.promotionsdkui.theme.token.PRMSearchBarToken
import com.ttcn.promotionsdk.promotionsdkui.theme.token.PRMTabChipToken
import com.ttcn.promotionsdk.promotionsdkui.theme.token.PRMTabUnderlineToken

/**
 * Cấu hình theme của SDK. Đối ứng 1-1 với `PRMSDKTheme.swift` bên iOS: cùng sáu token, cùng
 * tên field, cùng ngữ nghĩa `null` = **giữ mặc định của SDK** cho nhóm đó.
 *
 * Trước đây Android còn có `PromotionThemeConfig` — bản sao đúng sáu field này, kèm
 * `PRMSDKTheme.from(config)` và `toThemeConfig()` chuyển qua lại mà không type nào mang thêm
 * thông tin gì. Đã gộp về một, khớp với iOS.
 */
data class PRMSDKTheme(
    val buttonToken: PRMButtonToken? = null,
    val searchBarToken: PRMSearchBarToken? = null,
    val listItemToken: PRMListItemToken? = null,
    val tabChipToken: PRMTabChipToken? = null,
    val tabUnderlineToken: PRMTabUnderlineToken? = null,
    val discountBadgeToken: PRMDiscountBadgeToken? = null,
)
