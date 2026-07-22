package com.ttcn.promotionsdk.core.domain.model.eligible

/**
 * Request cho API Find Eligible Campaigns (`POST .../redemption/eligible`) — luồng checkout
 * "Chọn ưu đãi". Trả hai nhóm `myOffers` (voucher đã sở hữu) và `otherOffers` (campaign công khai
 * chưa sở hữu), **phân trang độc lập**.
 *
 * Bỏ trống [items] thì server chỉ trả campaign cấp đơn, không có campaign yêu cầu SKU.
 */
data class FindEligibleCampaignsRequest(
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
    /** Tìm ưu đãi theo tên/mã voucher — lọc cả myOffers lẫn otherOffers; null/rỗng → không lọc. */
    val keyword: String? = null,
    val myPage: Int = 0,
    val mySize: Int = 10,
    val otherPage: Int = 0,
    val otherSize: Int = 10,
    val filterOptions: EligibleFilterOptions = EligibleFilterOptions(),
) {
    /**
     * Bản sao với cặp trang đúng cho lần gọi kế khi hai nhóm phân trang **ĐỘC LẬP** — rule dùng
     * chung Android & iOS. Chỉ [section] được yêu cầu mới tiến sang [nextPage]; nhóm còn lại giữ
     * trang hiện tại ([currentMyPage] / [currentOtherPage]). [section] null (load đầu/refresh) →
     * cả hai nhóm cùng về [nextPage] (thường là 0).
     */
    fun forSectionPage(
        section: EligibleSection?,
        nextPage: Int,
        currentMyPage: Int = 0,
        currentOtherPage: Int = 0,
    ): FindEligibleCampaignsRequest {
        val isMine = section == EligibleSection.MY_OFFERS
        return copy(
            section = section,
            myPage = if (section == null || isMine) nextPage else currentMyPage,
            otherPage = if (section == null || !isMine) nextPage else currentOtherPage,
        )
    }
}

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
