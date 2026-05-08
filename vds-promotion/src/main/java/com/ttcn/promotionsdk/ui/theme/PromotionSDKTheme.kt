// vds-promotion/src/main/java/com/ttcn/promotionsdk/ui/theme/PromotionSDKTheme.kt
package com.ttcn.promotionsdk.ui.theme

data class PromotionSDKTheme(
    val colors: PromotionColorToken = PromotionColorToken(),
    val fonts: PromotionFontTokens = PromotionFontTokens(),
    val icons: PromotionIconToken = PromotionIconToken(),
    val borders: PromotionBorderTokens = PromotionBorderTokens()
)