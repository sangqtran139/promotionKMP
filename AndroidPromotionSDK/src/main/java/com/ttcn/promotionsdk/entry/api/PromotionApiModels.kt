
package com.ttcn.promotionsdk.entry.api

/**
 * DTO công khai của [PRMSDKApi]. Đối ứng 1-1 với `PromotionApiModels.swift` bên iOS:
 * cùng tên type, cùng tên field, cùng thứ tự khai báo. Sửa một bên thì sửa cả hai.
 *
 * Ngày tháng giữ nguyên **chuỗi thô của server** ở cả hai nền tảng. Parse ở tầng này thì hỏng định
 * dạng sẽ trả `null`, và host không phân biệt được "voucher vô thời hạn" với "server trả định dạng
 * lạ". Định dạng ngày là việc của tầng hiển thị.
 */

/** 1 voucher trong danh sách trả cho đối tác. */
data class PRMVoucher(
    val id: String,
    val merchantName: String,
    /** Tên ưu đãi. */
    val title: String,
    val imageURL: String?,
    val expireDate: String?,
    val isUsed: Boolean,
    /** Mã trạng thái thô từ server (ACTIVE/USED/EXPIRED...). */
    val status: String?,
    /** Nhãn trạng thái do server cung cấp (ưu tiên hiển thị). */
    val displayStatusLabel: String?,
)

/**
 * Kết quả lấy voucher **của khách** (Search Customer Vouchers).
 *
 * API chỉ trả voucher đã sở hữu. Để lấy "Ưu đãi khác" (campaign chưa sở hữu, đủ điều kiện cho đơn)
 * dùng [PRMSDKApi.findEligible].
 */
data class PRMVoucherPage(
    val vouchers: List<PRMVoucher>,
    val isLastPage: Boolean,
    /** Ngưỡng cảnh báo sắp hết hạn (đơn vị ngày) — cấu hình tĩnh BFF; null nếu không trả. */
    val expireWarningDate: Int? = null,
)

/** Chi tiết 1 voucher (Get Customer Voucher Detail). */
data class PRMVoucherDetail(
    val id: String,
    val merchantName: String,
    val title: String,
    val description: String,
    /** Hướng dẫn sử dụng (điều khoản / cách dùng). */
    val guideline: String,
    val startDate: String?,
    val expireDate: String?,
    val bannerURL: String?,
    val logoURL: String?,
    val status: String,
    val displayStatusLabel: String?,
    /** Danh sách mã (codex) đã cấp cho khách. */
    val codes: List<String> = emptyList(),
    /** Link hướng dẫn sử dụng. */
    val usageGuideUrl: String? = null,
)

/** 1 ưu đãi đủ điều kiện (Find Eligible Campaigns) cho luồng checkout. */
data class PRMEligibleOffer(
    /** Định danh để chọn/validate: `voucherId` (nhóm "của tôi") hoặc `campaignId` (nhóm "khác"). */
    val id: String,
    /** Tên ưu đãi hiển thị. */
    val name: String,
    /** Loại campaign (DISCOUNT/COUPON...), dùng làm `objectType` khi validate. */
    val objectType: String,
    /** true = dùng được ngay; false = chưa đủ điều kiện (hiển thị mờ). */
    val usable: Boolean,
    /** Số tiền giảm dự kiến (chuỗi số thô), null nếu không có preview. */
    val estimatedDiscount: String?,
    val expireDate: String?,
    /** Gợi ý lý do chưa đủ điều kiện (khi [usable] = false). SDK trả rule thô, không dựng sẵn câu. */
    val ineligibleReason: String?,
    /** Logo voucher/ưu đãi (có ở cả 2 nhóm). */
    val logoUrl: String? = null,
    /** Tên đối tác/merchant phát hành (có ở cả 2 nhóm). */
    val partnerName: String? = null,
    /** Mã code đã phát hành — chỉ nhóm "của tôi". */
    val voucherCode: String? = null,
)

/** Kết quả Find Eligible Campaigns: 2 nhóm "của tôi" / "khác", phân trang ĐỘC LẬP. */
data class PRMEligibleResult(
    val myOffers: List<PRMEligibleOffer>,
    val otherOffers: List<PRMEligibleOffer>,
    val myIsLastPage: Boolean,
    val otherIsLastPage: Boolean,
    /** Ngưỡng cảnh báo sắp hết hạn (đơn vị ngày) — cấu hình tĩnh BFF; null nếu không trả. */
    val expireWarningDate: Int? = null,
)

/**
 * 1 dòng sản phẩm trong đơn. Host truyền vào [PRMSDKApi.findEligible] để lấy campaign theo SKU
 * — đơn không kèm items chỉ nhận campaign cấp đơn.
 */
data class PRMOrderItem(
    /** Mã SKU sản phẩm (bắt buộc). */
    val skuId: String,
    val productId: String? = null,
    /** Tên sản phẩm — cho rule theo tên / hiển thị. */
    val productName: String? = null,
    /** Ngành hàng / danh mục — cho rule theo category. */
    val productCategory: String? = null,
    /** Số lượng (> 0). */
    val quantity: Int,
    /** Đơn giá — chuỗi số nguyên (VNĐ), vd "500000". */
    val unitPrice: String,
)

/** Kết quả validate một tập voucher với đơn hàng, trước khi áp. */
data class PRMValidationResult(
    val overallValid: Boolean,
    val totalDiscountAmount: String,
    val finalAmount: String,
    val items: List<PRMDiscountItem>,
)

data class PRMDiscountItem(
    val objectId: String,
    val discountAmount: String,
    val isValid: Boolean,
    /** Lý do voucher không hợp lệ, vd "EXPIRED", "BUDGET_EXCEEDED", "NOT_ELIGIBLE". */
    val eligibilityStatus: String,
)

/** Kết quả tạo redemption session. */
data class PRMRedemptionResult(
    val sessionId: String,
    val totalDiscount: String,
    val finalAmount: String,
    /** Lỗi validation nếu có voucher không hợp lệ trong session. */
    val validationErrors: List<PRMRedemptionError>,
)

data class PRMRedemptionError(
    /** Mã lỗi nghiệp vụ, vd "VOUCHER_EXPIRED", "INSUFFICIENT_BUDGET". */
    val code: String,
    /** Mô tả lỗi có thể hiển thị cho user. */
    val message: String,
)
