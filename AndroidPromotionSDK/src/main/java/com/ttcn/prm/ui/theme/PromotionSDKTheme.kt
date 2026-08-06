package com.ttcn.prm.ui.theme

import com.ttcn.prm.ui.theme.token.ButtonToken
import com.ttcn.prm.ui.theme.token.DiscountBadgeToken
import com.ttcn.prm.ui.theme.token.ListItemToken
import com.ttcn.prm.ui.theme.token.SearchBarToken
import com.ttcn.prm.ui.theme.token.TabChipToken
import com.ttcn.prm.ui.theme.token.TabUnderlineToken

/**
 * Cấu hình theme của SDK. Đối ứng 1-1 với `PromotionSDKTheme.swift` bên iOS: cùng sáu token, cùng
 * tên field, cùng ngữ nghĩa `null` = **giữ mặc định của SDK** cho nhóm đó.
 */
data class PromotionSDKTheme(
    val buttonToken: ButtonToken? = null,
    val searchBarToken: SearchBarToken? = null,
    val listItemToken: ListItemToken? = null,
    val tabChipToken: TabChipToken? = null,
    val tabUnderlineToken: TabUnderlineToken? = null,
    val discountBadgeToken: DiscountBadgeToken? = null,
)
