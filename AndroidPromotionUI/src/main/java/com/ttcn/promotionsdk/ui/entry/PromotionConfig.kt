package com.ttcn.promotionsdk.ui.entry

import com.ttcn.promotionsdk.core.config.AvailableService
import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
import com.ttcn.promotionsdk.core.config.SdkEnvironment
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOrderItem
import com.ttcn.promotionsdk.ui.entry.api.PromotionOrderItem

/**
 * Thông tin phiên đăng nhập và cấu hình kết nối — truyền 1 lần lúc [PromotionSDK.initialize].
 * Để cập nhật đơn hàng / dịch vụ mỗi khi vào màn, dùng [PromotionSDK.updateContext].
 */
data class PromotionSessionConfig(
    val customerId: String,
    val accessToken: String,
    val baseUrl: String,
    val language: String = "vi-VN",
    val environment: PromotionEnvironment = PromotionEnvironment.PROD,
)

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

internal fun PromotionSDKOptions.toCoreConfig(
    contextProvider: PromotionRequestContextProvider?,
): PromotionSDKConfig = PromotionSDKConfig(
    baseUrl = session.baseUrl,
    requestContextProvider = contextProvider,
    environment = when (session.environment) {
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

/**
 * Giữ toàn bộ context mà SDK cần — tĩnh (session) + động (đơn hàng/dịch vụ).
 * [PromotionSDK.updateContext] ghi trực tiếp vào đây; instance được tạo mới mỗi [PromotionSDK.initialize].
 */
internal class PromotionMutableContext(
    val session: PromotionSessionConfig,
) : PromotionRequestContextProvider {

    @JvmField @Volatile var orderId: String? = null
    @JvmField @Volatile var orderValue: String? = null
    @JvmField @Volatile var serviceCode: String? = null
    @JvmField @Volatile var metaData: String? = null

    /** Dòng sản phẩm (SKU) của đơn hiện tại — lõi đọc qua [getOrderItems] cho `findEligible`. */
    @JvmField @Volatile var orderItems: List<PromotionOrderItem> = emptyList()

    override fun getCustomerId() = session.customerId
    override fun getAccessToken() = session.accessToken
    override fun getLanguage() = session.language
    override fun getOrderId() = orderId
    override fun getOrderValue() = orderValue
    override fun getService() = serviceCode
    override fun getMetaData() = metaData

    /**
     * Map order items (public) → model lõi cho Find Eligible Campaigns — `ChoosePromotionStore` /
     * `EndowStore` đọc chung 2 nền tảng. Đối ứng `PromotionMutableContext.getOrderItems()` bên iOS.
     */
    override fun getOrderItems(): List<EligibleOrderItem> = orderItems.map {
        EligibleOrderItem(
            skuId = it.skuId,
            quantity = it.quantity,
            unitPrice = it.unitPrice,
            orderItemId = null,
            productId = it.productId,
            productName = it.productName,
            productCategory = it.productCategory,
        )
    }
}
