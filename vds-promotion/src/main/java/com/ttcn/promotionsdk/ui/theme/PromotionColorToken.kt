// vds-promotion/src/main/java/com/ttcn/promotionsdk/ui/theme/PromotionColorToken.kt
package com.ttcn.promotionsdk.ui.theme

import androidx.annotation.ColorInt

data class PromotionColorToken(

    // Nền tổng thể SDK
    @ColorInt val colorBackground: Int? = null,

    // Nền surface trắng (card, bottom sheet)
    @ColorInt val colorSurface: Int? = null,

    // Text đen chính — title, content
    @ColorInt val colorTextPrimary: Int? = null,

    // Text xám phụ — store name, expired date, status
    @ColorInt val colorTextSecondary: Int? = null,

    // Màu brand — tab indicator, button outline border, badge
    @ColorInt val colorBrand: Int? = null,

    // Button gradient CTA — "Săn ngay", "Sử dụng ngay", "Đổi điểm"
    @ColorInt val colorButtonGradientStart: Int? = null,
    @ColorInt val colorButtonGradientEnd: Int? = null,

    // Button outline — "Tặng ưu đãi", "Mua với..."
    @ColorInt val colorButtonOutlineBorder: Int? = null,
    @ColorInt val colorButtonOutlineText: Int? = null,

    // TabLayout (shared ở cả 2 màn)
    @ColorInt val colorTabIndicator: Int? = null,
    @ColorInt val colorTabTextSelected: Int? = null,
    @ColorInt val colorTabTextUnselected: Int? = null,
    @ColorInt val colorTabRipple: Int? = null,

    // Icon back tint
    @ColorInt val colorIconBack: Int? = null,

    // Nền RecyclerView list
    @ColorInt val colorListBackground: Int? = null,

    // ── Screen 1 only (màn danh sách) ───────────────────────────────────

    // Tab pill "Tất cả" / "Sắp hết hạn"
    @ColorInt val colorTabPillActiveBackground: Int? = null,
    @ColorInt val colorTabPillActiveText: Int? = null,
    @ColorInt val colorTabPillInactiveBackground: Int? = null,
    @ColorInt val colorTabPillInactiveText: Int? = null,

    // FAB scroll-to-top
    @ColorInt val colorFabBackground: Int? = null,
    @ColorInt val colorFabIcon: Int? = null,

    // ── Screen 2 only (màn chi tiết) ────────────────────────────────────

    // Card top chứa thông tin promotion
    @ColorInt val colorCardBackground: Int? = null,

    // Badge trạng thái "Đã sử dụng rồi"
    @ColorInt val colorStatusBadgeBackground: Int? = null,
    @ColorInt val colorStatusBadgeText: Int? = null,

    // Badge discount "-40%"
    @ColorInt val colorDiscountBadgeStart: Int? = null,
    @ColorInt val colorDiscountBadgeEnd: Int? = null,

    // ProgressBar (số lượng còn lại)
    @ColorInt val colorProgressTrack: Int? = null,
    @ColorInt val colorProgressFill: Int? = null,
    @ColorInt val colorProgressText: Int? = null,

    // Section tặng quà
    @ColorInt val colorGiftSectionBackground: Int? = null,
)
