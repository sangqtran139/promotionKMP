package com.ttcn.promotionsdk.presentation.promotiondetail

import com.ttcn.promotionsdk.domain.model.voucher.VoucherDetail
import com.ttcn.promotionsdk.domain.model.voucher.VoucherStatus

/**
 * Contract của màn "PromotionDetail" — **State / Intent / model hiển thị**, dùng chung Android & iOS.
 *
 * Tách khỏi [PromotionDetailStore] để đọc được "màn này có dữ liệu gì, nhận được lệnh gì" mà không phải
 * lội qua phần điều phối. Logic nằm ở store; ở đây chỉ có cấu trúc, **không** chuỗi hiển thị.
 */
public data class PromotionDetailState(
    val isLoading: Boolean = false,
    val detail: VoucherDetail? = null,
    val status: VoucherStatus = VoucherStatus.UNKNOWN,
    val actionVisible: Boolean = true,
    val actionEnabled: Boolean = false,
    /**
     * Nhãn nút do server trả (`VoucherDetail.displayStatusLabel`).
     *
     * ⚠️ **Hai màn chi tiết hiện KHÔNG đọc field này** — chốt dùng chuỗi cứng "Sử dụng ngay"
     * (`prm_use_now` / `PromotionUIStrings.useNow`) vì BE trả "Sử dụng" cho mọi voucher. Giữ lại để
     * bật lại nhãn server không phải sửa store; card ở màn danh sách thì vẫn theo nhãn server.
     */
    val actionLabel: String = "",
    val errorCode: String? = null,
)

/**
 * **Không có intent seed từ ngoài**: màn chi tiết chỉ hiển thị khi `getCustomerVoucherDetail` trả về —
 * dữ liệu từ màn danh sách không được dùng để dựng card/nút (tránh hai nguồn sự thật lệch nhau).
 */
public sealed interface PromotionDetailIntent {
    public data class LoadDetail(val voucherId: String) : PromotionDetailIntent
    public data object ConsumeError : PromotionDetailIntent
}
