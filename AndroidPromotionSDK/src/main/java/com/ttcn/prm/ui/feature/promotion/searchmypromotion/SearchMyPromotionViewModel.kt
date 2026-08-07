package com.ttcn.prm.ui.feature.promotion.searchmypromotion

import androidx.lifecycle.viewModelScope
import com.ttcn.promotionsdk.config.PromotionSDKConfig
import com.ttcn.promotionsdk.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.presentation.searchmypromotion.SearchMyPromotionIntent
import com.ttcn.promotionsdk.presentation.searchmypromotion.SearchMyPromotionState
import com.ttcn.promotionsdk.presentation.searchmypromotion.SearchMyPromotionStore
import com.ttcn.promotionsdk.presentation.serviceselector.servicesForApplicableProducts
import com.ttcn.prm.ui.base.PRMBaseViewModel
import com.ttcn.prm.ui.feature.promotion.ext.toServiceSelectorUiItem
import com.ttcn.prm.ui.feature.promotion.mypromotion.MyVoucherListItem
import com.ttcn.prm.ui.feature.promotion.mypromotion.toMyVoucherListItem

/**
 * Lớp bọc mỏng quanh [SearchMyPromotionStore] (tầng UI-logic dùng chung ở `promotionLogic`).
 * **Đồng nhất với `SearchMyPromotionViewModel` bên iOS** — cùng `store` / `bindStore` / `render` /
 * `handleError` + forward intent cùng thứ tự (xem `MyPromotionViewModel` để hiểu quy ước chung).
 *
 * `OpenServiceSelector`/`ServiceSelected` là phần thuần Android (bottom sheet + `config`) — cùng
 * khuôn với `MyPromotionViewModel`, đối ứng `SearchMyPromotionViewController` bên iOS.
 */
internal class SearchMyPromotionViewModel(
    searchCustomerVouchersUseCase: SearchCustomerVouchersUseCase,
    private val config: PromotionSDKConfig,
) : PRMBaseViewModel<SearchMyPromotionUiState, SearchMyPromotionAction, SearchMyPromotionEffect>(
    SearchMyPromotionUiState(),
) {

    // ─── Store ────────────────────────────────────────────────────────────────
    private val store = SearchMyPromotionStore(searchCustomerVouchersUseCase, viewModelScope)

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
    override fun handleAction(action: SearchMyPromotionAction) {
        when (action) {
            is SearchMyPromotionAction.QueryChanged -> store.dispatch(SearchMyPromotionIntent.QueryChanged(action.keyword))
            SearchMyPromotionAction.Search -> store.dispatch(SearchMyPromotionIntent.Search)
            SearchMyPromotionAction.LoadMore -> store.dispatch(SearchMyPromotionIntent.LoadMore)
            is SearchMyPromotionAction.OpenServiceSelector -> openServiceSelector(action.voucher)
            is SearchMyPromotionAction.ServiceSelected -> Unit // TODO: điều hướng màn dịch vụ khi có đích đến
        }
    }

    // ─── State → View ─────────────────────────────────────────────────────────
    private fun render(state: SearchMyPromotionState) {
        setState { state.toUiState() }
    }

    // ─── Error ──────────────────────────────────────────────────────────────────
    private fun handleError(state: SearchMyPromotionState) {
        val code = state.errorCode ?: return
        sendEffect(SearchMyPromotionEffect.ShowError(code))   // view map code → chuỗi
        store.dispatch(SearchMyPromotionIntent.ConsumeError)
    }

    // ─── Android-only: bottom sheet "Chọn dịch vụ" (render native; lọc dùng chung ở promotionLogic) ──
    private fun openServiceSelector(voucher: MyVoucherListItem) {
        val services = servicesForApplicableProducts(voucher.applicableProducts, config.availableServices)
            .map { it.toServiceSelectorUiItem() }
        sendEffect(SearchMyPromotionEffect.ShowServiceSelector(voucher = voucher, services = services))
    }
}

/**
 * Chiếu state dùng chung ([SearchMyPromotionState]) → **bề mặt view Android** ([SearchMyPromotionUiState]).
 * Cùng vai trò với `buildOutput()` bên iOS (xem `MyPromotionViewModel`).
 */
private fun SearchMyPromotionState.toUiState() = SearchMyPromotionUiState(
    keyword = keyword,
    vouchers = vouchers.map { it.toMyVoucherListItem() },
    isLoading = isLoading,
    isLoadingMore = isLoadingMore,
    isEmpty = isEmpty,
    isLastPage = isLastPage,
    page = page,
    pageSize = pageSize,
)
