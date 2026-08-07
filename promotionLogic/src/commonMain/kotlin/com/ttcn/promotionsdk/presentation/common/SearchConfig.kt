package com.ttcn.promotionsdk.presentation.common

/**
 * Số ký tự tối đa của ô tìm kiếm ưu đãi — **chặn ngay khi nhập**, dùng chung màn "Ưu đãi của tôi",
 * "Tìm kiếm" và "Chọn ưu đãi" (TLNV MOB_001 control 5.2 / MOB_004 control 2.1: Maxlength 255).
 *
 * iOS khai lại hằng số này ở `PromotionSearchLimit.maxKeywordLength` — đổi thì đổi cả hai.
 */
const val PROMOTION_SEARCH_MAX_LENGTH = 255
