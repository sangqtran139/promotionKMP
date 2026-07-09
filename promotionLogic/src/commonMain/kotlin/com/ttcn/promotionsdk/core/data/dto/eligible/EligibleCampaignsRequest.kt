package com.ttcn.promotionsdk.core.data.dto.eligible

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EligibleCampaignsRequest(
    @SerialName("customerInfo") val customerInfo: EligibleCustomerInfo,
    @SerialName("orderInfo") val orderInfo: EligibleOrderInfo,
    @SerialName("filterOptions") val filterOptions: EligibleFilterOptionsDto,
    @SerialName("pagination") val pagination: EligiblePagination,
)

@Serializable
internal data class EligibleCustomerInfo(
    @SerialName("customerId") val customerId: String,
    @SerialName("customerType") val customerType: String? = null,
    @SerialName("segment") val segment: String? = null,
    @SerialName("tier") val tier: String? = null,
)

@Serializable
internal data class EligibleOrderInfo(
    @SerialName("orderId") val orderId: String,
    @SerialName("orderValue") val orderValue: String,
    @SerialName("currency") val currency: String = "VND",
    @SerialName("channel") val channel: String = "MOBILE",
    @SerialName("items") val items: List<EligibleOrderItemDto> = emptyList(),
)

@Serializable
internal data class EligibleOrderItemDto(
    @SerialName("skuId") val skuId: String,
    @SerialName("quantity") val quantity: Int,
    @SerialName("unitPrice") val unitPrice: String,
    @SerialName("orderItemId") val orderItemId: String? = null,
    @SerialName("productId") val productId: String? = null,
    @SerialName("productName") val productName: String? = null,
    @SerialName("productCategory") val productCategory: String? = null,
)

@Serializable
internal data class EligibleFilterOptionsDto(
    @SerialName("includeExpired") val includeExpired: Boolean = false,
    @SerialName("checkBudgetAvailability") val checkBudgetAvailability: Boolean = true,
    @SerialName("includePreview") val includePreview: Boolean = true,
)

@Serializable
internal data class EligiblePagination(
    @SerialName("myOffers") val myOffers: EligiblePageRequest,
    @SerialName("otherOffers") val otherOffers: EligiblePageRequest,
    @SerialName("tabCode") val tabCode: String? = null,
    @SerialName("sectionCode") val sectionCode: String? = null,
)

@Serializable
internal data class EligiblePageRequest(
    @SerialName("page") val page: Int,
    @SerialName("size") val size: Int,
)
