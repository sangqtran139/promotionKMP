// vds-promotion/src/main/java/com/ttcn/promotionsdk/ui/entry/PromotionSDK.kt
package com.ttcn.promotionsdk.ui.entry

import android.content.Context
import com.ttcn.promotionsdk.core.di.PromotionContainer
import com.ttcn.promotionsdk.ui.theme.PromotionSDKTheme

object PromotionSDK {

    private var theme: PromotionSDKTheme = PromotionSDKTheme()
    private var callback: PromotionSDKCallback? = null

    @JvmStatic
    fun init(context: Context, options: PromotionSDKOptions) {
        theme = options.theme
        callback = options.callback
        PromotionContainer.init(context, options.config)
    }

    @JvmStatic
    fun getTheme(): PromotionSDKTheme = theme

    @JvmStatic
    fun getCallback(): PromotionSDKCallback? = callback

    @JvmStatic
    fun release() {
        PromotionContainer.clear()
        theme = PromotionSDKTheme()
        callback = null
    }
}
