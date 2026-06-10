package com.ttcn.promotionsdk.ui.feature.promotion.promotiondetail

import com.ttcn.promotionsdk.core.data.dto.voucher.VoucherStatus
import com.ttcn.promotionsdk.core.domain.model.VoucherDetail

data class PromotionDetailUiState(
    val isLoading: Boolean = false,
    val detail: VoucherDetail? = null,
    val status: VoucherStatus = VoucherStatus.UNKNOWN,
    val actionVisible: Boolean = true,
    val actionEnabled: Boolean = false,
    val actionLabel: String = "",
)

sealed interface PromotionDetailAction {
    data class LoadDetail(val voucherId: String) : PromotionDetailAction
}

sealed interface PromotionDetailEffect {
    data class ShowError(val errorCode: String) : PromotionDetailEffect
}
