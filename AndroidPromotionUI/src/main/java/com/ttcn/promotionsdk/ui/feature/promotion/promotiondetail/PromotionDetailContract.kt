package com.ttcn.promotionsdk.ui.feature.promotion.promotiondetail

import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherDetail
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherStatus
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.ServiceSelectorUiItem

internal data class PromotionDetailUiState(
    val isLoading: Boolean = false,
    val detail: VoucherDetail? = null,
    val status: VoucherStatus = VoucherStatus.UNKNOWN,
    val actionVisible: Boolean = true,
    val actionEnabled: Boolean = false,
    val actionLabel: String = "",
)

internal sealed interface PromotionDetailAction {
    data class LoadDetail(val voucherId: String) : PromotionDetailAction
    data object OpenServiceSelector : PromotionDetailAction
    data class ServiceSelected(val service: ServiceSelectorUiItem) : PromotionDetailAction
}

internal sealed interface PromotionDetailEffect {
    data class ShowError(val errorCode: String) : PromotionDetailEffect
    data class ShowServiceSelector(val services: List<ServiceSelectorUiItem>) : PromotionDetailEffect
}
