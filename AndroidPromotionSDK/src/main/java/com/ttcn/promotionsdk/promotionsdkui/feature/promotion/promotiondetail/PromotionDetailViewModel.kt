package com.ttcn.promotionsdk.promotionsdkui.feature.promotion.promotiondetail

import androidx.lifecycle.viewModelScope
import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
import com.ttcn.promotionsdk.core.domain.usecase.GetCustomerVoucherDetailUseCase
import com.ttcn.promotionsdk.presentation.promotiondetail.PromotionDetailIntent
import com.ttcn.promotionsdk.presentation.promotiondetail.PromotionDetailState
import com.ttcn.promotionsdk.presentation.promotiondetail.PromotionDetailStore
import com.ttcn.promotionsdk.presentation.serviceselector.servicesForApplicableProducts
import com.ttcn.promotionsdk.promotionsdkui.base.PRMBaseViewModel
import com.ttcn.promotionsdk.promotionsdkui.feature.promotion.ext.toServiceSelectorUiItem

/**
 * Lớp bọc mỏng quanh [PromotionDetailStore] (tầng UI-logic dùng chung ở `promotionLogic`).
 * **Đồng nhất với `PromotionDetailViewModel` bên iOS** — cùng `store` / `bindStore` / `render` /
 * `handleError` + forward intent cùng thứ tự (xem `MyPromotionViewModel` để hiểu quy ước chung).
 * `OpenServiceSelector`/`ServiceSelected` là phần thuần Android (bottom sheet + `config`).
 */
internal class PromotionDetailViewModel(
    getCustomerVoucherDetailUseCase: GetCustomerVoucherDetailUseCase,
    private val config: PromotionSDKConfig,
) : PRMBaseViewModel<PromotionDetailUiState, PromotionDetailAction, PromotionDetailEffect>(
    PromotionDetailUiState(),
) {

    // ─── Store ────────────────────────────────────────────────────────────────
    private val store = PromotionDetailStore(getCustomerVoucherDetailUseCase, viewModelScope)

    init {
        bindStore()
    }

    private fun bindStore() {
        launch {
            store.state.collect { state ->
                render(state)
                handleError(state)
            }
        }
    }

    // ─── Intent forwarding ──────────────────────────────────────────────────────
    override fun handleAction(action: PromotionDetailAction) {
        when (action) {
            is PromotionDetailAction.LoadDetail -> store.dispatch(PromotionDetailIntent.LoadDetail(action.voucherId))
            PromotionDetailAction.OpenServiceSelector -> openServiceSelector()
            is PromotionDetailAction.ServiceSelected -> Unit // TODO: điều hướng màn dịch vụ khi có đích đến
        }
    }

    // ─── State → View ─────────────────────────────────────────────────────────
    private fun render(state: PromotionDetailState) {
        setState { state.toUiState() }
    }

    // ─── Error ──────────────────────────────────────────────────────────────────
    private fun handleError(state: PromotionDetailState) {
        val code = state.errorCode ?: return
        sendEffect(PromotionDetailEffect.ShowError(code))   // view map code → chuỗi
        store.dispatch(PromotionDetailIntent.ConsumeError)
    }

    // ─── Android-only: bottom sheet "Chọn dịch vụ" (render native; lọc dùng chung ở promotionLogic) ──
    private fun openServiceSelector() {
        val applicableProducts = store.currentState().detail?.applicableProducts.orEmpty()
        val services = servicesForApplicableProducts(applicableProducts, config.availableServices)
            .map { it.toServiceSelectorUiItem() }
        sendEffect(PromotionDetailEffect.ShowServiceSelector(services))
    }
}

/**
 * Chiếu state dùng chung ([PromotionDetailState]) → **bề mặt view Android** ([PromotionDetailUiState]).
 * Cùng vai trò với `buildOutput()` bên iOS (xem `MyPromotionViewModel`).
 */
private fun PromotionDetailState.toUiState() = PromotionDetailUiState(
    isLoading = isLoading,
    detail = detail,
    status = status,
    actionVisible = actionVisible,
    actionEnabled = actionEnabled,
    actionLabel = actionLabel,
)
