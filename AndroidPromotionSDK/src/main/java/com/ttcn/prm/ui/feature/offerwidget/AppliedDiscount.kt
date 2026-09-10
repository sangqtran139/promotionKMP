package com.ttcn.prm.ui.feature.offerwidget

import com.ttcn.promotionsdk.presentation.offerwidget.OfferWidgetAppliedDiscount

/**
 * Một ưu đãi đã được validate/áp dụng — kiểu **công khai** của SDK.
 *
 * Host nhận danh sách này từ [PRMOfferWidget.discountDetails] và truyền lại cho
 * [PRMOfferWidget.setDiscountDetails].
 *
 * **Là `typealias`, không phải data class riêng.** Kiểu thật nằm ở `promotionLogic`
 * ([OfferWidgetAppliedDiscount]) để Android và iOS dùng đúng một model; trước đây Android chép lại y hệt
 * năm field rồi map qua map lại hai chiều ở `OfferWidgetViewModel` — chép sai một field là hai nền tảng
 * lệch nhau mà không ai biết. Giữ tên `AppliedDiscount` ở đây để host **không phải sửa import**;
 * `AppliedDiscount(...)` vẫn dựng được như cũ.
 *
 * ⚠️ Host viết bằng **Java** thì không thấy typealias — phải dùng thẳng
 * `com.ttcn.promotionsdk.presentation.offerwidget.OfferWidgetAppliedDiscount`.
 *
 * Lưu ý: [com.ttcn.prm.entry.PromotionSDKCallback.onVoucherApplied] (thống nhất với iOS) chỉ trả
 * `voucherId`; chi tiết giảm giá đi theo luồng widget ở trên, không qua callback.
 */
typealias AppliedDiscount = OfferWidgetAppliedDiscount
