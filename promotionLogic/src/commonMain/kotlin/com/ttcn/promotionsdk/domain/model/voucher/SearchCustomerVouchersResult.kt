package com.ttcn.promotionsdk.domain.model.voucher

data class SearchCustomerVouchersResult(
    val keyword: String? = null,
    val serviceCode: String? = null,
    val tabs: List<VoucherTabItem> = emptyList(),
    val defaultTab: String? = null,
    val selectedTab: String? = null,
    val content: List<VoucherItem> = emptyList(),
    val number: Int? = null,
    val size: Int? = null,
    val last: Boolean? = null,
    val totalElements: Long? = null,
    /** Ngưỡng cảnh báo sắp hết hạn (đơn vị ngày) — cấu hình tĩnh BFF; null nếu không trả. */
    val expireWarningDate: Int? = null,
) {
    /**
     * Tab đang active theo thứ tự ưu tiên nghiệp vụ, **dùng chung Android & iOS**:
     * server chỉ định ([selectedTab]) → mặc định ([defaultTab]) → tab client vừa yêu cầu
     * ([requestedTab]) → tab đầu danh sách (theo [VoucherTabItem.order], không phụ thuộc
     * thứ tự list thô từ server).
     *
     * > Hàm này trả lời câu hỏi **"đáp xuống tab nào"** — chỉ có nghĩa khi client chưa có ý kiến.
     * > `MyPromotionStore` vì vậy gọi nó **không kèm [requestedTab]**: khi user vừa bấm một tab thì
     * > tab đó thắng thẳng, không đưa vào đây so bì. Nghe theo [selectedTab] trong tình huống ấy sẽ
     * > làm tab sáng nhảy ngược ngay dưới ngón tay user nếu server echo lệch tab đã yêu cầu.
     */
    fun resolveActiveTab(requestedTab: String? = null): String? =
        selectedTab
            ?: defaultTab
            ?: requestedTab
            ?: tabs.minByOrNull { it.order ?: Int.MAX_VALUE }?.code
}

data class VoucherTabItem(
    val code: String,
    val label: String,
    val count: Int? = null,
    val order: Int? = null,
    val isDefault: Boolean = false,
)

data class VoucherItem(
    val voucherId: String,
    val merchantName: String? = null,
    val title: String? = null,
    val description: String? = null,
    val logo: String? = null,
    val expirationDate: String? = null,
    val status: String? = null,
    val displayStatusLabel: String? = null,
    val campaignId: String? = null,
    val campaignType: String? = null,
    val objectType: String = "CAMPAIGN",
    /** Server đánh dấu voucher tự áp dụng → widget tự validate và áp khi load. */
    val isAutoApplied: Boolean = false,
    val applicableProducts: List<ApplicableProduct> = emptyList(),
)
