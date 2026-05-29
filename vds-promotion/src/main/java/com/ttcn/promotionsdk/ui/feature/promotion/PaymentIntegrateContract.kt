package com.ttcn.promotionsdk.ui.feature.promotion

import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyVoucherListItem

data class PaymentIntegrateUiState(
    val isLoading: Boolean = false,
    val myVouchers: List<MyVoucherListItem> = emptyList(),
    val otherVouchers: List<MyVoucherListItem> = emptyList(),
    val totalVoucherCount: Int = 0,
    val appliedVouchers: List<MyVoucherListItem> = emptyList(),
    val hasLoadedInitial: Boolean = false,
)

sealed interface PaymentIntegrateAction {
    data object LoadInitialIfNeeded : PaymentIntegrateAction
    data class ApplyVouchers(val selected: List<MyVoucherListItem>) : PaymentIntegrateAction
}

sealed interface PaymentIntegrateEffect {
    data class ShowError(val errorCode: String) : PaymentIntegrateEffect
}