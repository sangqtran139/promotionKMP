package com.ttcn.promotionsdk.core.domain.model.eligible

/**
 * Request cho API Find Eligible Campaigns (`POST .../redemption/eligible`) — luồng checkout
 * "Chọn ưu đãi". Trả hai nhóm `myOffers` (voucher đã sở hữu) và `otherOffers` (campaign công khai
 * chưa sở hữu), **phân trang độc lập**.
 *
 * Bỏ trống [items] thì server chỉ trả campaign cấp đơn, không có campaign yêu cầu SKU.
 */
data class FindEligibleCampaignsRequest(
    val customerId: String,
    val orderId: String,
    val orderValue: String,
    val items: List<EligibleOrderItem> = emptyList(),
    val currency: String = "VND",
    val channel: String = "MOBILE",
    val customerType: String? = null,
    val segment: String? = null,
    val tier: String? = null,
    /** Mã tab lấy từ `tabs[].code`. Null → tab mặc định của server. */
    val tabCode: String? = null,
    /** Null → lấy cả hai nhóm. Có giá trị → chỉ load-more nhóm đó. */
    val section: EligibleSection? = null,
    val myPage: Int = 0,
    val mySize: Int = 10,
    val otherPage: Int = 0,
    val otherSize: Int = 10,
    val filterOptions: EligibleFilterOptions = EligibleFilterOptions(),
)

enum class EligibleSection(val code: String) {
    MY_OFFERS("my_offers"),
    OTHER_OFFERS("other_offers"),
}

data class EligibleOrderItem(
    val skuId: String,
    val quantity: Int,
    val unitPrice: String,
    val orderItemId: String? = null,
    val productId: String? = null,
    val productName: String? = null,
    val productCategory: String? = null,
)

data class EligibleFilterOptions(
    val includeExpired: Boolean = false,
    val checkBudgetAvailability: Boolean = true,
    val includePreview: Boolean = true,
)
