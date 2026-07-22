package com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion

import androidx.lifecycle.viewModelScope
import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.domain.exception.toErrorCode
import com.ttcn.promotionsdk.core.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.core.domain.usecase.ValidateStackableDiscountsUseCase
import com.ttcn.promotionsdk.presentation.choosepromotion.ChooseOffer
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionIntent
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionState
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionStore
import com.ttcn.promotionsdk.ui.base.PRMBaseViewModel
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.ChoosePromotionEffect.ApplyValidatedVouchers
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.ChoosePromotionEffect.ShowError
import com.ttcn.promotionsdk.ui.feature.promotion.ext.appliedDiscountFor
import com.ttcn.promotionsdk.ui.feature.promotion.ext.toMyVoucherListItem
import com.ttcn.promotionsdk.ui.feature.promotion.ext.toValidateDiscountsRequest
import com.ttcn.promotionsdk.ui.feature.promotion.ext.withExpiryWarning
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyVoucherListItem
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.toTabItem

/**
 * Lớp bọc mỏng quanh [ChoosePromotionStore] (tầng UI-logic dùng chung ở `promotionLogic`).
 * **Đồng nhất với `ChoosePromotionViewModel` bên iOS** — cùng `store` / `bindStore` / `render` /
 * `handleError` + forward intent cùng thứ tự (xem `MyPromotionViewModel` để hiểu quy ước chung).
 *
 * `validateAndApply` giữ ở ViewModel (bất đối xứng có chủ đích: iOS làm ở `PromotionSDKImpl`); nhưng
 * **rule diễn giải** validate đã dùng chung ở domain (`ValidateDiscountsResult` qua `appliedDiscountFor`).
 */
internal class ChoosePromotionViewModel(
    findEligibleCampaignsUseCase: FindEligibleCampaignsUseCase,
    private val validateStackableDiscountsUseCase: ValidateStackableDiscountsUseCase,
    private val requestContextProvider: PromotionRequestContextProvider,
) : PRMBaseViewModel<ChoosePromotionUiState, ChoosePromotionAction, ChoosePromotionEffect>(
    ChoosePromotionUiState(),
) {

    // ─── Store ────────────────────────────────────────────────────────────────
    private val store = ChoosePromotionStore(findEligibleCampaignsUseCase, viewModelScope)

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
    override fun handleAction(action: ChoosePromotionAction) {
        when (action) {
            ChoosePromotionAction.LoadInitial -> store.dispatch(ChoosePromotionIntent.LoadInitial)
            is ChoosePromotionAction.PreloadVouchers -> store.dispatch(
                ChoosePromotionIntent.Preload(action.myOffers, action.otherOffers, myIsLastPage = false, otherIsLastPage = true)
            )
            ChoosePromotionAction.Refresh -> store.dispatch(ChoosePromotionIntent.Refresh)
            is ChoosePromotionAction.QueryChanged -> store.dispatch(ChoosePromotionIntent.QueryChanged(action.keyword))
            ChoosePromotionAction.Search -> store.dispatch(ChoosePromotionIntent.Search)
            ChoosePromotionAction.ClearKeyword -> store.dispatch(ChoosePromotionIntent.ClearKeyword)
            ChoosePromotionAction.LoadMoreMyVouchers -> store.dispatch(ChoosePromotionIntent.LoadMoreMyVouchers)
            ChoosePromotionAction.LoadMoreOtherVouchers -> store.dispatch(ChoosePromotionIntent.LoadMoreOtherVouchers)
            is ChoosePromotionAction.ValidateAndApply -> validateAndApply(action.selected)
        }
    }

    // ─── State → View ─────────────────────────────────────────────────────────
    private fun render(state: ChoosePromotionState) {
        // Giữ `isValidating` (state cục bộ của validate, không thuộc store) qua mỗi lần store phát.
        setState { state.toUiState(isValidating = isValidating) }
    }

    // ─── Error ──────────────────────────────────────────────────────────────────
    private fun handleError(state: ChoosePromotionState) {
        val code = state.errorCode ?: return
        sendEffect(ShowError(code))   // view map code → chuỗi
        store.dispatch(ChoosePromotionIntent.ConsumeError)
    }

    // ─── VM-only: validate & apply (iOS làm ở PromotionSDKImpl; rule diễn giải dùng chung ở domain) ──
    private fun validateAndApply(selected: List<MyVoucherListItem>) {
        if (selected.isEmpty()) {
            sendEffect(ApplyValidatedVouchers(emptyList()))
            return
        }
        launch {
            setState { copy(isValidating = true) }
            val request = selected.toValidateDiscountsRequest(
                orderId = requestContextProvider.getOrderId().orEmpty(),
                orderValue = requestContextProvider.getOrderValue().orEmpty(),
            )
            runCatching { validateStackableDiscountsUseCase(request) }
                .onSuccess { response ->
                    val details = response
                        ?.let { r -> selected.map { r.appliedDiscountFor(it.voucherId, it.objectType) } }
                        .orEmpty()
                    setState { copy(isValidating = false) }
                    sendEffect(ApplyValidatedVouchers(details))
                }
                .onFailure { throwable ->
                    setState { copy(isValidating = false) }
                    sendEffect(ShowError(throwable.toErrorCode()))
                }
        }
    }
}

/**
 * Chiếu state dùng chung ([ChoosePromotionState]) → **bề mặt view Android** ([ChoosePromotionUiState]).
 * Cùng vai trò với `buildOutput()` bên iOS. [isValidating] giữ từ state cũ (không thuộc store).
 */
private fun ChoosePromotionState.toUiState(isValidating: Boolean) = ChoosePromotionUiState(
    hasLoadedInitial = hasLoadedInitial,
    isLoading = isLoading,
    isRefreshing = isRefreshing,
    isLoadingMore = isLoadingMore,
    isLoadingMoreOther = isLoadingMoreOther,
    isValidating = isValidating,
    isEmpty = isEmpty,
    tabs = tabs.map { it.toTabItem() },
    selectedTabCode = selectedTabCode,
    keyword = keyword,
    page = myPage,
    size = mySize,
    isLastPage = myIsLastPage,
    otherPage = otherPage,
    otherSize = otherSize,
    isLastOtherPage = otherIsLastPage,
    vouchers = myOffers.map { it.toVoucherListItem(expireWarningDate) },
    otherVouchers = otherOffers.map { it.toVoucherListItem(expireWarningDate) },
)

private fun ChooseOffer.toVoucherListItem(expireWarningDate: Int?): MyVoucherListItem =
    source.toMyVoucherListItem().withExpiryWarning(expireWarningDate)
