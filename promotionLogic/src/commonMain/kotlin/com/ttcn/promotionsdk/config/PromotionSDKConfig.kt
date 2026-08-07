package com.ttcn.promotionsdk.config

import com.ttcn.promotionsdk.domain.model.eligible.EligibleOrderItem

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
    /** Bật log body của HTTP request/response. */
    val isDebug: Boolean = false,
)

interface PromotionRequestContextProvider {
    fun getService(): String? = null
    fun getAccessToken(): String? = null
    fun getLanguage(): String? = null
    fun getOrderId(): String? = null
    fun getOrderValue(): String? = null
    fun getMetaData(): String? = null

    fun getOrderItems(): List<EligibleOrderItem> = emptyList()
}

class EmptyPromotionRequestContextProvider : PromotionRequestContextProvider

enum class SdkEnvironment {
    PROD,
    STAGING
}
