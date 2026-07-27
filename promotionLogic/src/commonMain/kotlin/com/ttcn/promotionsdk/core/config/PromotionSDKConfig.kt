package com.ttcn.promotionsdk.core.config

import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOrderItem

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
    fun getService(): String? = null
    fun getAccessToken(): String? = null
    fun getLanguage(): String? = null
    fun getOrderId(): String? = null
    fun getOrderValue(): String? = null
    fun getMetaData(): String? = null

    /**
     * Order items (SKU) của đơn hiện tại — để `findEligible` lấy campaign yêu cầu SKU.
     * Mặc định rỗng (host chưa cấp → chỉ campaign cấp đơn). **Dùng chung 2 nền tảng**: `EndowStore`
     * và `ChoosePromotionStore` đọc hàm này thay cho `items = emptyList()` trước đây (đồng bộ request).
     */
    fun getOrderItems(): List<EligibleOrderItem> = emptyList()
}

class EmptyPromotionRequestContextProvider : PromotionRequestContextProvider

enum class SdkEnvironment {
    PROD,
    STAGING
}
