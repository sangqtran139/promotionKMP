// vds-promotion/src/main/java/com/ttcn/promotionsdk/core/di/PromotionContainer.kt
package com.ttcn.promotionsdk.core.di

import android.content.Context
import com.ttcn.promotionsdk.core.config.PromotionSDKConfig

object PromotionContainer {

    @Volatile
    private var applicationContext: Context? = null

    @Volatile
    private var config: PromotionSDKConfig? = null

    fun init(context: Context, config: PromotionSDKConfig) {
        applicationContext = context.applicationContext
        this.config = config
    }

    fun clear() {
        applicationContext = null
        config = null
    }
}
