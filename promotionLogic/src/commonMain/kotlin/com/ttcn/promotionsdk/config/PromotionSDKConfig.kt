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

    /**
     * Xin host lấy access token mới, gọi khi một request đã ăn **HTTP 401**. Host gọi lại [onResult]
     * với `true` khi đã có token mới, `false` khi chịu (phiên chết thật). SDK thử lại request hỏng
     * **đúng một lần** khi nhận `true`; nhận `false` thì để lỗi `TOKEN_EXPIRED` nổi lên.
     *
     * **`Boolean` chứ không phải token mới** là cố ý: token vào SDK theo **một** đường duy nhất là
     * [getAccessToken]. Trả token ở đây sẽ tạo đường thứ hai, và kèm theo là câu hỏi "SDK dùng chuỗi
     * tôi trả hay đọc lại kho của tôi?". Chữ ký này không để lại chỗ cho câu hỏi đó: host ghi token
     * mới vào kho của mình rồi báo `true`, lượt thử lại đọc [getAccessToken] như mọi request khác.
     *
     * **Callback chứ không phải `suspend`** cũng là cố ý: cơ chế lấy token của host là bất đồng bộ
     * và nằm ở tầng native, nên chữ ký này phải implement được bằng closure thường từ cả Swift lẫn
     * Java. Lõi tự bọc lại thành coroutine.
     *
     * Mặc định **chịu ngay** (`onResult(false)`) — host không cài đặt thì 401 hỏng luôn, không chờ.
     * Chi tiết luồng: [com.ttcn.promotionsdk.data.remote.TokenRefreshGate].
     *
     * Được gọi từ thread nền, và **tối đa một lượt refresh tại một thời điểm** cho toàn SDK dù có
     * bao nhiêu request cùng ăn 401.
     */
    fun refreshAccessToken(onResult: (Boolean) -> Unit) = onResult(false)
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
