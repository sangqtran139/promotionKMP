package com.ttcn.promotionsdk.ui.theme

import com.ttcn.promotionsdk.ui.theme.token.ButtonToken
import com.ttcn.promotionsdk.ui.theme.token.DiscountBadgeToken
import com.ttcn.promotionsdk.ui.theme.token.ListItemToken
import com.ttcn.promotionsdk.ui.theme.token.SearchBarToken
import com.ttcn.promotionsdk.ui.theme.token.TabChipToken
import com.ttcn.promotionsdk.ui.theme.token.TabUnderlineToken

/**
 * Cấu hình theme của SDK. Đối ứng 1-1 với `PromotionSDKTheme.swift` bên iOS: cùng sáu token, cùng
 * tên field, cùng ngữ nghĩa `null` = **giữ mặc định của SDK** cho nhóm đó.
 *
 * Trước đây Android còn có `PromotionThemeConfig` — bản sao đúng sáu field này, kèm
 * `PromotionSDKTheme.from(config)` và `toThemeConfig()` chuyển qua lại mà không type nào mang thêm
 * thông tin gì. Đã gộp về một, khớp với iOS.
 */
data class PromotionSDKTheme(
    val buttonToken: ButtonToken? = null,
    val searchBarToken: SearchBarToken? = null,
    val listItemToken: ListItemToken? = null,
    val tabChipToken: TabChipToken? = null,
    val tabUnderlineToken: TabUnderlineToken? = null,
    val discountBadgeToken: DiscountBadgeToken? = null,
)
