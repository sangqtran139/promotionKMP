package com.ttcn.promotionsdk.core.config

data class AvailableService(
    val serviceCode: String,
    val serviceName: String,
    val serviceType: String,
    val iconUrl: String
)

data class PromotionSDKConfig(
    val baseUrl: String,
    val requestContextProvider: PromotionRequestContextProvider? = null,
    val environment: SdkEnvironment = SdkEnvironment.PROD,
    val availableServices: List<AvailableService> = emptyList(),
    /**
     * Bật log body của HTTP request/response. Trên Android bản gốc suy ra từ
     * `ApplicationInfo.FLAG_DEBUGGABLE`; ở common không có `Context` nên host truyền vào.
     * Xem overload `PromotionContainer.initialize(context, config)` phía androidMain.
     */
    val isDebug: Boolean = false,
)

interface PromotionRequestContextProvider {
    /** Giữ cho feature flag (userId → Unleash). KHÔNG còn gửi trong body request các API promotion. */
    fun getCustomerId(): String? = null
    fun getService(): String? = null
    fun getAccessToken(): String? = null
    fun getLanguage(): String? = null
    fun getOrderId(): String? = null
    fun getOrderValue(): String? = null
    fun getMetaData(): String? = null
}

class EmptyPromotionRequestContextProvider : PromotionRequestContextProvider

enum class SdkEnvironment {
    PROD,
    STAGING
}
