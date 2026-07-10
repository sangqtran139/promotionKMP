package com.ttcn.promotionsdk.ui.feature.promotion.promotiondetail

import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
import com.ttcn.promotionsdk.core.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.core.domain.exception.toErrorCode
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherStatus
import com.ttcn.promotionsdk.core.domain.usecase.GetCustomerVoucherDetailUseCase
import com.ttcn.promotionsdk.ui.base.PRMBaseViewModel
import com.ttcn.promotionsdk.ui.feature.promotion.ext.toServiceSelectorUiItem

internal class PromotionDetailViewModel(
    private val getCustomerVoucherDetailUseCase: GetCustomerVoucherDetailUseCase,
    private val requestContextProvider: PromotionRequestContextProvider,
    private val config: PromotionSDKConfig,
) :
    PRMBaseViewModel<PromotionDetailUiState, PromotionDetailAction, PromotionDetailEffect>(
        PromotionDetailUiState(),
    ) {
    override fun handleAction(action: PromotionDetailAction) {
        when (action) {
            is PromotionDetailAction.LoadDetail -> loadDetail(action.voucherId)
            PromotionDetailAction.OpenServiceSelector -> openServiceSelector()
            is PromotionDetailAction.ServiceSelected -> Unit // TODO: navigate when destination is ready
        }
    }

    private fun openServiceSelector() {
        val applicableProductIds = uiState.value.detail
            ?.applicableProducts
            .orEmpty()
            .map { it.productId }
            .toSet()
        val services = config.availableServices
            .filter { it.serviceCode in applicableProductIds }
            .distinctBy { it.serviceCode }
            .map { it.toServiceSelectorUiItem() }
        sendEffect(PromotionDetailEffect.ShowServiceSelector(services))
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

    /**
     * Nút "Dùng ngay" hiện khi voucher còn dùng được. Quy tắc trạng thái nằm ở `promotionLogic`
     * (`VoucherStatus.displayState()`), dùng chung với iOS — không `when` trên từng mã ở đây nữa,
     * để thêm mã mới vào server không phải sửa hai nền tảng.
     */
    private fun VoucherStatus.toActionUiState(displayStatusLabel: String): VoucherActionUiState {
        return if (displayState().isUsable) {
            VoucherActionUiState(visible = true, enabled = true, label = "")
        } else {
            VoucherActionUiState(visible = false, enabled = false, label = displayStatusLabel)
        }
    }
}

internal data class VoucherActionUiState(
    val visible: Boolean,
    val enabled: Boolean,
    val label: String,
)
