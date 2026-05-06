// vds-promotion/src/main/java/com/ttcn/promotionsdk/core/config/PromotionSDKConfig.kt
package com.ttcn.promotionsdk.core.config

data class PromotionSDKConfig(
    val apiKey: String,
    val baseUrl: String,
    val environment: SdkEnvironment = SdkEnvironment.PROD
)

enum class SdkEnvironment {
    PROD,
    STAGING
}
