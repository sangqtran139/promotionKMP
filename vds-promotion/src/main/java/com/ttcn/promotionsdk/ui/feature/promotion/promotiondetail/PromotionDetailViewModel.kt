package com.ttcn.promotionsdk.ui.feature.promotion.promotiondetail

import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherStatus
import com.ttcn.promotionsdk.core.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.core.domain.exception.toErrorCode
import com.ttcn.promotionsdk.core.domain.usecase.GetCustomerVoucherDetailUseCase
import com.ttcn.promotionsdk.ui.base.PRMBaseViewModel

internal class PromotionDetailViewModel(
    private val getCustomerVoucherDetailUseCase: GetCustomerVoucherDetailUseCase,
    private val requestContextProvider: PromotionRequestContextProvider,
) :
    PRMBaseViewModel<PromotionDetailUiState, PromotionDetailAction, PromotionDetailEffect>(
        PromotionDetailUiState(),
    ) {
    override fun handleAction(action: PromotionDetailAction) {
        when (action) {
            is PromotionDetailAction.LoadDetail -> loadDetail(action.voucherId)
        }
    }

    private fun loadDetail(voucherId: String) {
        launch {
            setState { copy(isLoading = true) }
            val customerId = requestContextProvider.getCustomerId()
            if (customerId.isNullOrBlank()) {
                setState { copy(isLoading = false) }
                sendEffect(PromotionDetailEffect.ShowError(ErrorCodes.MISSING_CUSTOMER_ID))
                return@launch
            }

            runCatching {
                getCustomerVoucherDetailUseCase(
                    voucherId = voucherId,
                    customerId = customerId,
                    service = requestContextProvider.getService(),
                )
            }.onSuccess { detail ->
                val status = VoucherStatus.from(detail?.status)
                val label = detail?.displayStatusLabel.orEmpty()
                val actionState = status.toActionUiState(label)
                setState {
                    copy(
                        isLoading = false,
                        detail = detail,
                        status = status,
                        actionVisible = actionState.visible,
                        actionEnabled = actionState.enabled,
                        actionLabel = actionState.label,
                    )
                }
                if (detail == null) {
                    sendEffect(PromotionDetailEffect.ShowError("error_detail_unavailable"))
                }
            }.onFailure { throwable ->
                setState { copy(isLoading = false) }
                sendEffect(PromotionDetailEffect.ShowError(throwable.toErrorCode()))
            }
        }
    }

    override fun onError(throwable: Throwable) {
        setState {
            copy(
                isLoading = false,
            )
        }
        sendEffect(PromotionDetailEffect.ShowError(throwable.toErrorCode()))
    }

    private fun VoucherStatus.toActionUiState(displayStatusLabel: String): VoucherActionUiState {
        return when (this) {
            VoucherStatus.ACTIVE -> VoucherActionUiState(
                visible = true,
                enabled = true,
                label = displayStatusLabel,
            )

            VoucherStatus.RESERVED,
            VoucherStatus.REDEEMED,
            VoucherStatus.EXPIRED,
            VoucherStatus.REVOKED,
            VoucherStatus.SUSPENDED -> VoucherActionUiState(
                visible = true,
                enabled = false,
                label = displayStatusLabel,
            )

            VoucherStatus.UNKNOWN -> VoucherActionUiState(
                visible = false,
                enabled = false,
                label = displayStatusLabel,
            )
        }
    }
}

data class VoucherActionUiState(
    val visible: Boolean,
    val enabled: Boolean,
    val label: String,
)
