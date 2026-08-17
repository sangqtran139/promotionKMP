package com.ttcn.promotionsdk.config

import com.ttcn.promotionsdk.domain.model.eligible.EligibleOrderItem

data class AvailableService(
    val productId: String,
    val productName: String,
    val skuSourceId: String,
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

/**
 * Order items dùng cho `findEligible`, đã **đổ `serviceCode` vào `productId`** của từng item.
 *
 * API `findEligible` (spec 3.5.4 v19) **không có field `serviceCode`** ở bất kỳ cấp nào — chiều
 * dịch vụ đi qua `orderInfo.items[].productId`, đúng như `VoucherInfoDto` đã ghi: *"`productId` là
 * khoá khớp với `PromotionAvailableService.productId` do host khai"*.
 *
 * Nguồn giá trị là [getService] — tức `serviceCode` host truyền ở `updateContext`, đọc lại ở **mỗi**
 * request nên app có nhiều điểm mở màn chọn ưu đãi chỉ cần `updateContext` trước khi mở, không phải
 * re-init SDK. Trước đây [getService] không có một call-site production nào: host truyền
 * `serviceCode` mà `findEligible` không hề nhận được.
 *
 * **Item tự khai `productId` thì giữ nguyên** — host biết rõ dòng hàng của mình hơn context cấp đơn.
 * Chỉ điền vào chỗ còn trống.
 *
 * Một hàm dùng chung thay vì lặp ở `ChoosePromotionStore.buildRequest` và `EndowStore.loadInitial`:
 * hai chỗ đó lệch nhau là widget và màn chọn hỏi server hai câu khác nhau.
 */
internal fun PromotionRequestContextProvider.eligibleOrderItems(): List<EligibleOrderItem> {
    val service = getService()?.takeIf { it.isNotBlank() } ?: return getOrderItems()
    return getOrderItems().map { item ->
        if (item.productId.isNullOrBlank()) item.copy(productId = service) else item
    }
}

class EmptyPromotionRequestContextProvider : PromotionRequestContextProvider

enum class SdkEnvironment {
    PROD,
    STAGING
}
