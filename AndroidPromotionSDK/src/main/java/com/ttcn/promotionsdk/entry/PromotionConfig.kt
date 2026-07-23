package com.ttcn.promotionsdk.entry

import com.ttcn.promotionsdk.core.config.AvailableService
import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
import com.ttcn.promotionsdk.core.config.SdkEnvironment
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOrderItem
import com.ttcn.promotionsdk.entry.api.PRMOrderItem

/**
 * Thông tin phiên đăng nhập và cấu hình kết nối — truyền 1 lần lúc [PRMSDK.initialize].
 * Để cập nhật đơn hàng / dịch vụ mỗi khi vào màn, dùng [PRMSDK.updateContext].
 */
data class PRMSessionConfig(
    val customerId: String,
    val accessToken: String,
    val baseUrl: String,
    val language: String = "vi-VN",
    val environment: PRMEnvironment = PRMEnvironment.PROD,
)

/**
 * Một dịch vụ khả dụng mà voucher có thể áp dụng.
 *
 * [serviceCode] phải khớp `productId` trong `applicableProducts` của voucher thì dịch vụ mới hiện
 * ở bottom sheet "Chọn dịch vụ".
 */
data class PRMAvailableService(
    val serviceCode: String,
    val serviceName: String,
    val serviceType: String = "",
    val iconUrl: String = "",
)

enum class PRMEnvironment { PROD, STAGING }

// ─── Public → core ──────────────────────────────────────────────────────────

internal fun PRMSDKOptions.toCoreConfig(
    contextProvider: PromotionRequestContextProvider?,
): PromotionSDKConfig = PromotionSDKConfig(
    baseUrl = session.baseUrl,
    requestContextProvider = contextProvider,
    environment = when (session.environment) {
        PRMEnvironment.PROD -> SdkEnvironment.PROD
        PRMEnvironment.STAGING -> SdkEnvironment.STAGING
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
 * [PRMSDK.updateContext] ghi trực tiếp vào đây; instance được tạo mới mỗi [PRMSDK.initialize].
 */
internal class PromotionMutableContext(
    val session: PRMSessionConfig,
) : PromotionRequestContextProvider {

    @JvmField @Volatile var orderId: String? = null
    @JvmField @Volatile var orderValue: String? = null
    @JvmField @Volatile var serviceCode: String? = null
    @JvmField @Volatile var metaData: String? = null

    /** Dòng sản phẩm (SKU) của đơn hiện tại — lõi đọc qua [getOrderItems] cho `findEligible`. */
    @JvmField @Volatile var orderItems: List<PRMOrderItem> = emptyList()

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
