package com.ttcn.promotionsdk.presentation

import com.ttcn.promotionsdk.common.daysUntil

/**
 * Ngưỡng "sắp hết hạn" (số ngày) do server cấu hình — **dùng chung mọi màn**.
 *
 * API `Search Customer Vouchers` / `Find Eligible` trả `expireWarningDate` ở mức response, còn API
 * `Get Customer Voucher Detail` **hiện chưa trả**. Mà TLNV (MOB_002 control 2.4 → refer MOB_001
 * item #4) yêu cầu màn Chi tiết hiển thị "HSD còn X ngày" y như màn danh sách.
 *
 * Nên: giá trị lấy từ response detail nếu có, không có thì dùng **giá trị gần nhất** mà danh sách đã
 * nhận ([lastKnownDays]). Vào chi tiết từ danh sách/checkout — luồng thường gặp — là đã có giá trị.
 * Deeplink thẳng vào chi tiết mà BE chưa trả field thì suy biến về "không tô cam", không sai lệch gì
 * khác. Bỏ được nhánh dự phòng này ngay khi BE trả `expireWarningDate` ở API detail.
 */
internal object ExpiryWarning {

    /** Ngưỡng gần nhất nhận được từ API danh sách; null = chưa từng nhận. */
    var lastKnownDays: Int? = null
        private set

    /** Ghi nhận ngưỡng mới từ response danh sách. Bỏ qua null để không xoá giá trị đang có. */
    fun remember(days: Int?) {
        if (days != null) lastKnownDays = days
    }

    /**
     * Số ngày còn lại khi voucher **sắp hết hạn**; null nếu không áp dụng.
     *
     * Cùng rule với `VoucherItem.toMyPromotionVoucher` / `EligibleOffer.toChooseOffer`: chỉ tính khi
     * voucher còn dùng được và số ngày nằm trong `[0, threshold]`.
     */
    fun daysIfExpiringSoon(expirationDate: String?, usable: Boolean, threshold: Int? = lastKnownDays): Int? {
        if (!usable) return null
        val warn = threshold ?: return null
        return daysUntil(expirationDate)?.takeIf { it in 0..warn }
    }
}
