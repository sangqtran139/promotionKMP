package com.ttcn.promotionsdk.promotionsdkui.feature.promotion.choosepromotion

import androidx.lifecycle.viewModelScope
import com.ttcn.promotionsdk.core.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.presentation.choosepromotion.ChooseOffer
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionIntent
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionState
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionStore
import com.ttcn.promotionsdk.presentation.choosepromotion.mySeeMoreState
import com.ttcn.promotionsdk.promotionsdkui.base.PRMBaseViewModel
import com.ttcn.promotionsdk.promotionsdkui.feature.promotion.choosepromotion.ChoosePromotionEffect.ApplySelectedOffers
import com.ttcn.promotionsdk.promotionsdkui.feature.promotion.choosepromotion.ChoosePromotionEffect.ShowError
import com.ttcn.promotionsdk.promotionsdkui.feature.promotion.ext.toMyVoucherListItem
import com.ttcn.promotionsdk.promotionsdkui.feature.promotion.mypromotion.MyVoucherListItem
import com.ttcn.promotionsdk.promotionsdkui.feature.promotion.mypromotion.toTabItem

/**
 * Lớp bọc mỏng quanh [ChoosePromotionStore] (tầng UI-logic dùng chung ở `promotionLogic`).
 * **Đồng nhất với `ChoosePromotionViewModel` bên iOS** — cùng `store` / `bindStore` / `render` /
 * `handleError` + forward intent cùng thứ tự (xem `MyPromotionViewModel` để hiểu quy ước chung).
 *
 * Bấm "Áp dụng" chỉ **trả offers đang chọn** cho widget; validate + áp do `EndowStore` lo (dùng chung
 * iOS — trước đây Android validate ở đây, iOS ở `PromotionSDKImpl`).
 */
internal class ChoosePromotionViewModel(
    findEligibleCampaignsUseCase: FindEligibleCampaignsUseCase,
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
            is ChoosePromotionAction.SetPreSelected -> store.dispatch(ChoosePromotionIntent.SetPreSelected(action.ids))
            is ChoosePromotionAction.ToggleSelection -> store.dispatch(ChoosePromotionIntent.ToggleSelection(action.id))
            ChoosePromotionAction.SeeMoreMy -> store.dispatch(ChoosePromotionIntent.SeeMoreMy)
            ChoosePromotionAction.ValidateAndApply -> applySelected()
        }
    }

    // ─── State → View ─────────────────────────────────────────────────────────
    private fun render(state: ChoosePromotionState) {
        setState { state.toUiState() }
    }

    // ─── Error ──────────────────────────────────────────────────────────────────
    private fun handleError(state: ChoosePromotionState) {
        val code = state.errorCode ?: return
        sendEffect(ShowError(code))   // view map code → chuỗi
        store.dispatch(ChoosePromotionIntent.ConsumeError)
    }

    // ─── Áp dụng: chỉ trả offers đang chọn cho widget (EndowStore validate) ──────
    // Selection do store giữ (`selectedIds`); resolve về EligibleOffer đang chọn.
    private fun applySelected() {
        val state = store.currentState()
        val selectedOffers = (state.myOffers + state.otherOffers)
            .map { it.source }
            .filter { it.id in state.selectedIds }
        sendEffect(ApplySelectedOffers(selectedOffers))
    }
}

/**
 * Chiếu state dùng chung ([ChoosePromotionState]) → **bề mặt view Android** ([ChoosePromotionUiState]).
 * Cùng vai trò với `buildOutput()` bên iOS.
 */
private fun ChoosePromotionState.toUiState() = ChoosePromotionUiState(
    hasLoadedInitial = hasLoadedInitial,
    isLoading = isLoading,
    isRefreshing = isRefreshing,
    isLoadingMore = isLoadingMore,
    isLoadingMoreOther = isLoadingMoreOther,
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
    vouchers = myOffers.map { it.toVoucherListItem() },
    otherVouchers = otherOffers.map { it.toVoucherListItem() },
    isMultiSelection = isMultiSelection,
    selectedIds = selectedIds,
    myExpanded = myExpanded,
    mySeeMore = mySeeMoreState(),
)

/** Quyết định hiển thị (`isUsable`/`expiringInDays`) lấy thẳng từ store — không tự tính lại. */
private fun ChooseOffer.toVoucherListItem(): MyVoucherListItem =
    source.toMyVoucherListItem().copy(isEnabled = isUsable, expiringInDays = expiringInDays)
