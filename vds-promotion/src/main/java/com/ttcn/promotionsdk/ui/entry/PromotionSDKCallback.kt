package com.ttcn.promotionsdk.ui.entry

import com.ttcn.promotionsdk.ui.entry.AppliedDiscount

interface PromotionSDKCallback {

    /** Gọi khi user chọn và xác nhận áp dụng ưu đãi thành công. */
    fun onVoucherApplied(discountDetails: List<AppliedDiscount>) {}

    /** Gọi khi SDK gặp lỗi không xử lý được ở tầng UI. */
    fun onError(errorCode: String) {}

    /** Gọi khi màn hình SDK được đóng (user back hoặc SDK release). */
    fun onSDKClosed() {}
}
