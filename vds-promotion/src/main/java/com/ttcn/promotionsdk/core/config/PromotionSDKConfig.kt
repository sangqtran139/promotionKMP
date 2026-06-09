package com.ttcn.promotionsdk.core.config

data class PromotionSDKConfig(
    val apiKey: String,
    val baseUrl: String,
    val requestContextProvider: PromotionRequestContextProvider? = null,
    val environment: SdkEnvironment = SdkEnvironment.PROD
)

interface PromotionRequestContextProvider {
    fun getCustomerId(): String? = "123"
    fun getService(): String? = null
    fun getAccessToken(): String? = null
    fun getLanguage(): String? = null
    fun getOrderId(): String? = "123"
    fun getOrderValue(): String? = "123"
}

class EmptyPromotionRequestContextProvider : PromotionRequestContextProvider

enum class SdkEnvironment {
    PROD,
    STAGING
}
