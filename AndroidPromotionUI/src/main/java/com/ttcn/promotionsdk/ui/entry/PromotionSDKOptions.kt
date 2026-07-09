// vds-promotion/src/main/java/com/ttcn/promotionsdk/ui/entry/PromotionSDKOptions.kt
package com.ttcn.promotionsdk.ui.entry

import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
import com.ttcn.promotionsdk.ui.theme.PromotionSDKTheme

data class PromotionSDKOptions(
    val config: PromotionSDKConfig,
    val theme: PromotionSDKTheme = PromotionSDKTheme(),
    val callback: PromotionSDKCallback? = null
)
