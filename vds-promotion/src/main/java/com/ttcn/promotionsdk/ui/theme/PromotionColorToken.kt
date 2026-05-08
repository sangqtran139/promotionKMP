// vds-promotion/src/main/java/com/ttcn/promotionsdk/ui/theme/PromotionColorToken.kt
package com.ttcn.promotionsdk.ui.theme

data class PromotionColorToken(
    // Màu chủ đạo: tab indicator (cả 2 màn), ripple base
    val colorPrimary: Int? = null,              // EE0033

    // Nền màn MyEndow (bg_endow drawable)
    val colorBackground: Int? = null,

    // Nền content sheet (bg_item_endow), nền TabLayout detail
    val colorSurface: Int? = null,              // FBFBFB

    // Nền list homeList
    val colorListBackground: Int? = null,       // FBFBFB

    // Text tiêu đề đậm: tvEndow, tvContent, tab selected
    val colorOnSurface: Int? = null,            // black

    // Text phụ: tvStore, tvExpired, tab unselected
    val colorOnSurfaceVariant: Int? = null,     // 7A7A7A

    // Nền FAB scroll to top
    val colorFabBackground: Int? = null,        // tokenWhite

    // Ripple của tab (EndowDetail TabLayout)
    val colorTabRipple: Int? = null,            // D3D3D3
)
