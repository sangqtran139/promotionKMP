package com.ttcn.promotionsdk.ui.entry

import com.ttcn.promotionsdk.core.config.AvailableService
import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
import com.ttcn.promotionsdk.core.config.SdkEnvironment

/**
 * Cấu hình phiên SDK do host cấp — kiểu **công khai** của `AndroidPromotionUI`.
 *
 * Bản sao của `PromotionSDKConfig` ở lõi, cố ý không tái sử dụng type lõi: host chỉ nhận **một**
 * AAR và không có `com.ttcn.promotionsdk.core.*` trên compile classpath. Xem [PromotionSDKApi].
 *
 * [PromotionSDKConfig.isDebug] không có ở đây: `PromotionSDK.init` suy ra từ
 * `ApplicationInfo.FLAG_DEBUGGABLE` của host.
 */
data class PromotionConfig(
    val apiKey: String,
    val baseUrl: String,
    val contextProvider: PromotionContextProvider? = null,
    val environment: PromotionEnvironment = PromotionEnvironment.PROD,
    val availableServices: List<PromotionAvailableService> = emptyList(),
)

/**
 * Nguồn token / customerId / thông tin đơn do host cấp. SDK đọc lại ở **mỗi** request, nên host
 * refresh token là SDK thấy ngay — không cần `init` lại.
 */
interface PromotionContextProvider {
    fun getCustomerId(): String? = null
    fun getService(): String? = null
    fun getAccessToken(): String? = null
    fun getLanguage(): String? = null
    fun getOrderId(): String? = null
    fun getOrderValue(): String? = null
}

/**
 * Một dịch vụ khả dụng mà voucher có thể áp dụng.
 *
 * [serviceCode] phải khớp `productId` trong `applicableProducts` của voucher thì dịch vụ mới hiện
 * ở bottom sheet "Chọn dịch vụ".
 */
data class PromotionAvailableService(
    val serviceCode: String,
    val serviceName: String,
    val serviceType: String = "",
    val iconUrl: String = "",
)

enum class PromotionEnvironment { PROD, STAGING }

// ─── Public → core ──────────────────────────────────────────────────────────

internal fun PromotionConfig.toCoreConfig(): PromotionSDKConfig = PromotionSDKConfig(
    apiKey = apiKey,
    baseUrl = baseUrl,
    requestContextProvider = contextProvider?.let(::CoreContextProviderAdapter),
    environment = when (environment) {
        PromotionEnvironment.PROD -> SdkEnvironment.PROD
        PromotionEnvironment.STAGING -> SdkEnvironment.STAGING
    },
    availableServices = availableServices.map {
        AvailableService(
            serviceCode = it.serviceCode,
            serviceName = it.serviceName,
            serviceType = it.serviceType,
            iconUrl = it.iconUrl,
        )
    },
)

/** Uỷ quyền chứ không sao chép: host trả giá trị mới (token refresh) là lõi đọc được ngay. */
private class CoreContextProviderAdapter(
    private val delegate: PromotionContextProvider,
) : PromotionRequestContextProvider {
    override fun getCustomerId(): String? = delegate.getCustomerId()
    override fun getService(): String? = delegate.getService()
    override fun getAccessToken(): String? = delegate.getAccessToken()
    override fun getLanguage(): String? = delegate.getLanguage()
    override fun getOrderId(): String? = delegate.getOrderId()
    override fun getOrderValue(): String? = delegate.getOrderValue()
}
