package com.ttcn.prm.ui.theme

import com.ttcn.prm.ui.theme.token.PRMButtonToken
import com.ttcn.prm.ui.theme.token.PRMDiscountBadgeToken
import com.ttcn.prm.ui.theme.token.PRMListItemToken
import com.ttcn.prm.ui.theme.token.PRMSearchBarToken
import com.ttcn.prm.ui.theme.token.PRMTabChipToken
import com.ttcn.prm.ui.theme.token.PRMTabUnderlineToken

/**
 * Cấu hình theme của SDK. Đối ứng 1-1 với `PromotionSDKTheme.swift` bên iOS: cùng sáu token, cùng
 * tên field, cùng ngữ nghĩa `null` = **giữ mặc định của SDK** cho nhóm đó.
 */
data class PromotionSDKTheme(
    val buttonToken: PRMButtonToken? = null,
    val searchBarToken: PRMSearchBarToken? = null,
    val listItemToken: PRMListItemToken? = null,
    val tabChipToken: PRMTabChipToken? = null,
    val tabUnderlineToken: PRMTabUnderlineToken? = null,
    val discountBadgeToken: PRMDiscountBadgeToken? = null,
)
