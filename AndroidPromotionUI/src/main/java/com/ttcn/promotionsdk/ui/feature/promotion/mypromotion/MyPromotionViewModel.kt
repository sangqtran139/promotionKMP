package com.ttcn.promotionsdk.ui.feature.promotion.mypromotion

import androidx.lifecycle.viewModelScope
import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherStatus
import com.ttcn.promotionsdk.core.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionIntent
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionState
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionStore
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionTab
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionVoucher
import com.ttcn.promotionsdk.ui.base.PRMBaseViewModel
import com.ttcn.promotionsdk.ui.feature.promotion.ext.toServiceSelectorUiItem

/**
 * Lớp bọc mỏng quanh [MyPromotionStore] (tầng UI-logic dùng chung ở `promotionLogic`).
 *
 * **Đồng nhất với `MyPromotionViewModel` bên iOS** từ tên thuộc tính (`store`) tới cấu trúc hàm
 * (`bindStore` / `render` / `handleError` + forward intent cùng thứ tự). Khác biệt duy nhất là bất
 * khả kháng do paradigm: Android theo MVI (`handleAction`/`setState`/`sendEffect`, collect `Flow`
 * trực tiếp), iOS theo MVVM+Combine (`transform`/publishers, quan sát qua `watchState`).
 *
 * `OpenServiceSelector`/`ServiceSelected` là phần thuần Android (bottom sheet + `config`).
 */
internal class MyPromotionViewModel(
    searchCustomerVouchersUseCase: SearchCustomerVouchersUseCase,
    private val config: PromotionSDKConfig,
) : PRMBaseViewModel<MyPromotionUiState, MyPromotionAction, MyPromotionEffect>(MyPromotionUiState()) {

    // ─── Store ────────────────────────────────────────────────────────────────
    private val store = MyPromotionStore(searchCustomerVouchersUseCase, viewModelScope)

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
    override fun handleAction(action: MyPromotionAction) {
        when (action) {
            MyPromotionAction.LoadInitialIfNeeded -> store.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
            MyPromotionAction.Refresh -> store.dispatch(MyPromotionIntent.Refresh)
            MyPromotionAction.LoadMore -> store.dispatch(MyPromotionIntent.LoadMore)
            is MyPromotionAction.SelectTab -> store.dispatch(MyPromotionIntent.SelectTab(action.tabCode))
            is MyPromotionAction.SearchKeyword -> store.dispatch(MyPromotionIntent.Search(action.keyword))
            is MyPromotionAction.OpenServiceSelector -> openServiceSelector(action.voucher)
            is MyPromotionAction.ServiceSelected -> Unit // TODO: điều hướng màn dịch vụ khi có đích đến
        }
    }

    // ─── State → View ─────────────────────────────────────────────────────────
    private fun render(state: MyPromotionState) {
        setState { state.toUiState() }
    }

    // ─── Error ──────────────────────────────────────────────────────────────────
    private fun handleError(state: MyPromotionState) {
        val code = state.errorCode ?: return
        sendEffect(MyPromotionEffect.ShowError(code))   // view map code → chuỗi
        store.dispatch(MyPromotionIntent.ConsumeError)
    }

    // ─── Android-only: bottom sheet "Chọn dịch vụ" (cần config) ──────────────────
    private fun openServiceSelector(voucher: MyVoucherListItem) {
        val applicableProductIds = voucher.applicableProducts.map { it.productId }.toSet()
        val services = config.availableServices
            .filter { it.serviceCode in applicableProductIds }
            .distinctBy { it.serviceCode }
            .map { it.toServiceSelectorUiItem() }
        sendEffect(MyPromotionEffect.ShowServiceSelector(voucher = voucher, services = services))
    }
}

// ─── Map state cấu trúc (store) → model UI Android hiện có ─────────────────────

/**
 * Chiếu state dùng chung ([MyPromotionState]) → **bề mặt view Android** ([MyPromotionUiState]).
 * Cùng vai trò với `buildOutput()` bên iOS; khác cấu trúc do idiom: Android gom thành **một** object
 * UiState (MVI), iOS dựng **graph publisher** (tách theo field).
 */
private fun MyPromotionState.toUiState() = MyPromotionUiState(
    hasLoadedInitial = hasLoadedInitial,
    isLoading = isLoading,
    isRefreshing = isRefreshing,
    isRefreshingTab = isRefreshingTab,
    isLoadingMore = isLoadingMore,
    isEmpty = isEmpty,
    tabs = tabs.map { it.toTabItem() },
    selectedTabCode = selectedTabCode,
    keyword = keyword,
    page = page,
    size = size,
    isLastPage = isLastPage,
    vouchers = vouchers.map { it.toMyVoucherListItem() },
)

/** `internal` để dùng chung MyPromotion + ChoosePromotion (cùng model tab UI). */
internal fun MyPromotionTab.toTabItem() = TabItem(
    code = code,
    label = label,
    count = count,
    order = order,
    isDefault = isDefault,
)

/**
 * Map `MyPromotionVoucher` (store, đã tính quyết định) → `MyVoucherListItem` (model UI Android).
 * `internal` để **dùng chung** giữa màn "Ưu đãi của tôi" và "Tìm ưu đãi" (cả hai hiển thị cùng cell).
 */
internal fun MyPromotionVoucher.toMyVoucherListItem() = MyVoucherListItem(
    voucherId = source.voucherId,
    campaignId = source.campaignId.orEmpty(),
    merchantName = source.merchantName.orEmpty(),
    title = source.title.orEmpty(),
    description = source.description.orEmpty(),
    logo = source.logo.orEmpty(),
    expirationDate = source.expirationDate.orEmpty(),
    displayStatusLabel = source.displayStatusLabel.orEmpty(),
    status = VoucherStatus.from(source.status),
    objectType = source.objectType,
    isAutoApplied = source.isAutoApplied,
    applicableProducts = source.applicableProducts,
)
