package com.ttcn.prm.ui.feature.promotion.promotiondetail

import com.ttcn.promotionsdk.domain.model.voucher.VoucherDetail
import com.ttcn.promotionsdk.domain.model.voucher.VoucherStatus
import com.ttcn.prm.ui.feature.promotion.mypromotion.ServiceSelectorUiItem

internal data class PromotionDetailUiState(
    val isLoading: Boolean = false,
    val detail: VoucherDetail? = null,
    val status: VoucherStatus = VoucherStatus.UNKNOWN,
    val actionVisible: Boolean = true,
    val actionEnabled: Boolean = false,
    /** Nhãn server — **Fragment không dùng**, nút dùng `prm_use_now`. Xem `PromotionDetailState`. */
    val actionLabel: String = "",
)

/**
 * Màn chi tiết **không nhận dữ liệu seed từ ngoài**: chỉ `voucherId` đi qua navigation, mọi thứ hiển
 * thị đều đến từ `getCustomerVoucherDetail`. Trong lúc chờ thì hiện shimmer. Giống hệt iOS.
 */

internal sealed interface PromotionDetailAction {
    data class LoadDetail(val voucherId: String) : PromotionDetailAction
    data object OpenServiceSelector : PromotionDetailAction
    data class ServiceSelected(val service: ServiceSelectorUiItem) : PromotionDetailAction
}

internal sealed interface PromotionDetailEffect {
    data class ShowError(val errorCode: String) : PromotionDetailEffect
    /**
     * Mở "Chọn dịch vụ". Luật **1 dịch vụ → chọn thẳng, không mở sheet** (TLNV MOB_002 control #5)
     * nằm trong `ServiceSelectorBottomSheet.present`, dùng chung cho cả 3 màn — không nhân bản ở
     * từng effect.
     */
    data class ShowServiceSelector(val services: List<ServiceSelectorUiItem>) : PromotionDetailEffect
}
