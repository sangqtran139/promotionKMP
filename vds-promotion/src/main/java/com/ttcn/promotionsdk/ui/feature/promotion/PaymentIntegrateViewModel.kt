package com.ttcn.promotionsdk.ui.feature.promotion

import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.data.remote.PromotionApiException
import com.ttcn.promotionsdk.core.domain.repository.PromotionRepository
import com.ttcn.promotionsdk.ui.base.PRMBaseViewModel
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.FakeVoucherData
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyVoucherListItem
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.toMyVoucherListItem
class PaymentIntegrateViewModel(
    private val repository: PromotionRepository,
    private val requestContextProvider: PromotionRequestContextProvider,
) : PRMBaseViewModel<PaymentIntegrateUiState, PaymentIntegrateAction, PaymentIntegrateEffect>(
    PaymentIntegrateUiState(),
) {

    override fun handleAction(action: PaymentIntegrateAction) {
        when (action) {
            PaymentIntegrateAction.LoadInitialIfNeeded -> {
                if (!uiState.value.hasLoadedInitial) loadInitialVouchers()
            }
            is PaymentIntegrateAction.ApplyVouchers -> {
                setState { copy(appliedVouchers = action.selected) }
            }
        }
    }

    private fun loadInitialVouchers() {
        launch {
            setState { copy(isLoading = true) }

            // TODO: xóa block fake và bỏ comment block API bên dưới khi có API
            val myList = FakeVoucherData.getMyVouchers(page = 0)
            val otherList = FakeVoucherData.getOtherVouchers(page = 0)
            setState {
                copy(
                    isLoading = false,
                    myVouchers = myList,
                    otherVouchers = otherList,
                    totalVoucherCount = FakeVoucherData.getTotalMyVoucherCount() +
                            FakeVoucherData.getTotalOtherVoucherCount(),
                    hasLoadedInitial = true,
                )
            }

            // ── API thật (uncomment khi sẵn sàng) ──────────────────────────
            // val customerId = requestContextProvider.getCustomerId()
            // if (customerId.isNullOrBlank()) {
            //     setState { copy(isLoading = false) }
            //     sendEffect(PaymentIntegrateEffect.ShowError("missing_customer_id"))
            //     return@launch
            // }
            // runCatching {
            //     repository.searchCustomerVouchers(
            //         customerId = customerId,
            //         keyword = null,
            //         serviceCode = "vay",
            //         sectionCode = null,
            //         tab = null,
            //         myVouchersPage = 0,
            //         myVouchersSize = 10,
            //         otherVouchersPage = 0,
            //         otherVouchersSize = 10,
            //     )
            // }.onSuccess { response ->
            //     val myList = response?.myVouchers?.content.orEmpty().map { it.toMyVoucherListItem() }
            //     val otherList = response?.otherVouchers?.content.orEmpty().map { it.toMyVoucherListItem() }
            //     setState {
            //         copy(
            //             isLoading = false,
            //             myVouchers = myList,
            //             otherVouchers = otherList,
            //             totalVoucherCount = (response?.myVouchers?.totalElements ?: myList.size) +
            //                     (response?.otherVouchers?.totalElements ?: otherList.size),
            //             hasLoadedInitial = true,
            //         )
            //     }
            // }.onFailure { throwable ->
            //     setState {
            //         copy(isLoading = false, myVouchers = emptyList(), otherVouchers = emptyList(),
            //             totalVoucherCount = 0, hasLoadedInitial = true)
            //     }
            //     sendEffect(PaymentIntegrateEffect.ShowError(throwable.toErrorCode()))
            // }
        }
    }

    override fun onError(throwable: Throwable) {
        setState { copy(isLoading = false) }
        sendEffect(PaymentIntegrateEffect.ShowError(throwable.toErrorCode()))
    }

    private fun Throwable.toErrorCode(): String =
        (this as? PromotionApiException)?.errorCode ?: message ?: "error_general"
}