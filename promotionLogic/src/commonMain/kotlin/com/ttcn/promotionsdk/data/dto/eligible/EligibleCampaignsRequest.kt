package com.ttcn.promotionsdk.data.dto.eligible

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body của API Find Eligible Campaigns (v1.6).
 * Định danh khách **không** nằm trong body — BFF lấy từ JWT `sub` (bỏ `customerId`/`sourceId`).
 * `sectionCode` nằm **top-level** (không trong `pagination`).
 */
@Serializable
internal data class EligibleCampaignsRequest(
    @SerialName("customerInfo") val customerInfo: EligibleCustomerInfo,
    @SerialName("orderInfo") val orderInfo: EligibleOrderInfo,
    @SerialName("filterOptions") val filterOptions: EligibleFilterOptionsDto,
    /** Kịch bản đánh giá ưu đãi (vd `ALL` / `AUDIENCE_ONLY` / `PRODUCTS`). Null → server mặc định `ALL`. */
    @SerialName("scenario") val scenario: String? = null,
    /** `my_offers` | `other_offers`. Null → trả cả 2 nhóm. */
    @SerialName("sectionCode") val sectionCode: String? = null,
    /** Tìm ưu đãi theo tên/mã voucher (max 255) — cả 2 nhóm; null → không lọc. */
    @SerialName("keyword") val keyword: String? = null,
    @SerialName("pagination") val pagination: EligiblePagination,
)

@Serializable
internal data class EligibleCustomerInfo(
    @SerialName("name") val name: String? = null,
    /** Metadata tự do phục vụ đánh giá ưu đãi (vd customerType/segment/tier). */
    @SerialName("metadata") val metadata: Map<String, String>? = null,
    @SerialName("profile") val profile: EligibleCustomerProfile? = null,
)

@Serializable
internal data class EligibleCustomerProfile(
    @SerialName("email") val email: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("birthdate") val birthdate: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("country") val country: String? = null,
    @SerialName("city") val city: String? = null,
    @SerialName("state") val state: String? = null,
    @SerialName("postalCode") val postalCode: String? = null,
    @SerialName("addressLine") val addressLine: String? = null,
)

@Serializable
internal data class EligibleOrderInfo(
    @SerialName("orderId") val orderId: String,
    @SerialName("orderValue") val orderValue: String,
    @SerialName("currency") val currency: String = "VND",
    @SerialName("orderDate") val orderDate: String? = null,
    @SerialName("metadata") val metadata: Map<String, String>? = null,
    @SerialName("items") val items: List<EligibleOrderItemDto> = emptyList(),
)

@Serializable
internal data class EligibleOrderItemDto(
    @SerialName("orderItemId") val orderItemId: String? = null,
    /** SKU source id đối tác — BE resolve `skuId` nội bộ (thay cho `skuId`). */
    @SerialName("skuSourceId") val skuSourceId: String,
    @SerialName("productId") val productId: String? = null,
    /** Product source id đối tác — BE resolve `productId`. */
    @SerialName("productSourceId") val productSourceId: String? = null,
    @SerialName("quantity") val quantity: Int,
    @SerialName("unitPrice") val unitPrice: String,
    /** = quantity × unitPrice — optional, server tự tính nếu bỏ trống. */
    @SerialName("subTotal") val subTotal: String? = null,
    /** Metadata cấp item cho rule engine (vd productName/productCategory). */
    @SerialName("metadata") val metadata: Map<String, String>? = null,
)

@Serializable
internal data class EligibleFilterOptionsDto(
    @SerialName("campaignTypes") val campaignTypes: List<String>? = null,
    @SerialName("discountTypes") val discountTypes: List<String>? = null,
    @SerialName("includeExpired") val includeExpired: Boolean = false,
    @SerialName("checkBudgetAvailability") val checkBudgetAvailability: Boolean = true,
    @SerialName("includePreview") val includePreview: Boolean = true,
)

@Serializable
internal data class EligiblePagination(
    @SerialName("myOffers") val myOffers: EligiblePageRequest,
    @SerialName("otherOffers") val otherOffers: EligiblePageRequest,
)

@Serializable
internal data class EligiblePageRequest(
    @SerialName("page") val page: Int,
    @SerialName("size") val size: Int,
)
