package com.ttcn.promotionsdk.domain.model.eligible

/**
 * Request cho API Find Eligible Campaigns (`POST .../redemptions/eligible`) — luồng checkout
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
    /** Ngày đặt đơn (ISO-8601, có offset) — null thì server dùng thời điểm gọi API. */
    val orderDate: String? = null,
    /** Metadata cấp đơn tự do phục vụ đánh giá ưu đãi (vd bankName/savingsAmount/term của sản phẩm
     * tiết kiệm; kênh/vị trí nếu cần cũng truyền qua đây — server không có field riêng). */
    val orderMetadata: Map<String, String>? = null,
    /** Kịch bản đánh giá ưu đãi (vd `ALL` / `AUDIENCE_ONLY` / `PRODUCTS`). Null → server mặc định `ALL`. */
    val scenario: String? = null,
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
     * Bản sao với cặp trang cho lần gọi kế — hai nhóm phân trang **độc lập**: chỉ [section] được yêu
     * cầu mới tiến sang [nextPage], nhóm còn lại giữ trang hiện tại. [section] null → cả hai về [nextPage].
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
    /** SKU source id đối tác — rỗng ("") = host không truyền, mapper bỏ hẳn field khi gửi lên server. */
    val skuSourceId: String,
    val quantity: Int,
    val unitPrice: String,
    val orderItemId: String? = null,
    val productId: String? = null,
    val productName: String? = null,
    val productCategory: String? = null,
)

data class EligibleFilterOptions(
    /** Lọc theo loại campaign. Null → không lọc. */
    val campaignTypes: List<String>? = null,
    /** Lọc theo kiểu giảm giá. Null → không lọc. */
    val discountTypes: List<String>? = null,
    val includeExpired: Boolean = false,
    val checkBudgetAvailability: Boolean = true,
    val includePreview: Boolean = true,
)
