
package com.ttcn.promotionsdk.ui.entry.api

/**
 * DTO công khai của [PromotionSDKApi]. Đối ứng 1-1 với `PromotionSDKResults.swift` bên iOS.
 *
 * Ngày tháng giữ nguyên **chuỗi thô của server** (iOS parse sang `Date` cho hợp tay Swift).
 * Cố ý: parse ở đây sẽ thêm một điểm hỏng mới, trong khi tầng UI của SDK cũng đang dùng chuỗi thô.
 */

/** 1 voucher trong danh sách trả cho đối tác. */
data class PromotionVoucher(
    val id: String,
    val merchantName: String,
    /** Tên ưu đãi. */
    val title: String,
    val imageUrl: String?,
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
 * dùng [PromotionSDKApi.findEligible].
 */
data class PromotionVoucherPage(
    val vouchers: List<PromotionVoucher>,
    val isLastPage: Boolean,
)

/** Chi tiết 1 voucher (Get Customer Voucher Detail). */
data class PromotionVoucherDetail(
    val id: String,
    val merchantName: String,
    val title: String,
    val description: String,
    /** Hướng dẫn sử dụng (điều khoản / cách dùng). */
    val guideline: String,
    val startDate: String?,
    val expireDate: String?,
    val bannerUrl: String?,
    val logoUrl: String?,
    val status: String,
    val displayStatusLabel: String?,
)

/** 1 ưu đãi đủ điều kiện (Find Eligible Campaigns) cho luồng checkout. */
data class PromotionEligibleOffer(
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
)

/** Kết quả Find Eligible Campaigns: 2 nhóm "của tôi" / "khác", phân trang ĐỘC LẬP. */
data class PromotionEligibleResult(
    val myOffers: List<PromotionEligibleOffer>,
    val otherOffers: List<PromotionEligibleOffer>,
    val myIsLastPage: Boolean,
    val otherIsLastPage: Boolean,
)

/**
 * 1 dòng sản phẩm trong đơn. Host truyền vào [PromotionSDKApi.findEligible] để lấy campaign theo SKU
 * — đơn không kèm items chỉ nhận campaign cấp đơn.
 */
data class PromotionOrderItem(
    /** Mã SKU sản phẩm (bắt buộc). */
    val skuId: String,
    /** Số lượng (> 0). */
    val quantity: Int,
    /** Đơn giá — chuỗi số nguyên (VNĐ), vd "500000". */
    val unitPrice: String,
    val productId: String? = null,
    /** Tên sản phẩm — cho rule theo tên / hiển thị. */
    val productName: String? = null,
    /** Ngành hàng / danh mục — cho rule theo category. */
    val productCategory: String? = null,
)

/** Kết quả validate một tập voucher với đơn hàng, trước khi áp. */
data class PromotionValidationResult(
    val overallValid: Boolean,
    val totalDiscountAmount: String,
    val finalAmount: String,
    val items: List<PromotionDiscountItem>,
)

data class PromotionDiscountItem(
    val objectId: String,
    val discountAmount: String,
    val isValid: Boolean,
    /** Lý do voucher không hợp lệ, vd "EXPIRED", "BUDGET_EXCEEDED", "NOT_ELIGIBLE". */
    val eligibilityStatus: String,
)

/** Kết quả tạo redemption session. */
data class PromotionRedemptionResult(
    val sessionId: String,
    val totalDiscount: String,
    val finalAmount: String,
    /** Lỗi validation nếu có voucher không hợp lệ trong session. */
    val validationErrors: List<PromotionRedemptionError>,
)

data class PromotionRedemptionError(
    /** Mã lỗi nghiệp vụ, vd "VOUCHER_EXPIRED", "INSUFFICIENT_BUDGET". */
    val code: String,
    /** Mô tả lỗi có thể hiển thị cho user. */
    val message: String,
)
