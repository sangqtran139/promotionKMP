package com.ttcn.prm.ui.feature.promotion.searchmypromotion

import com.ttcn.prm.ui.feature.promotion.mypromotion.MyVoucherListItem
import com.ttcn.prm.ui.feature.promotion.mypromotion.ServiceSelectorUiItem

internal data class SearchMyPromotionUiState(
    val keyword: String = "",
    val vouchers: List<MyVoucherListItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isEmpty: Boolean = false,
    val isLastPage: Boolean = true,
    val page: Int = 0,
    val pageSize: Int = DEFAULT_PAGE_SIZE
) {
    companion object {
        const val DEFAULT_PAGE_SIZE = 10
    }
}

/**
 * Chỉ khai báo action mà màn thật sự phát — khớp 1-1 `SearchMyPromotionViewModel.Input` bên iOS.
 * (`ClearKeyword`/`Retry` của store chưa màn nào dùng: gõ trắng đã đi qua `QueryChanged("")`.)
 */
internal sealed interface SearchMyPromotionAction {
    data class QueryChanged(val keyword: String) : SearchMyPromotionAction
    data object Search : SearchMyPromotionAction
    data object LoadMore : SearchMyPromotionAction
    data class OpenServiceSelector(val voucher: MyVoucherListItem) : SearchMyPromotionAction
    data class ServiceSelected(
        val voucher: MyVoucherListItem,
        val service: ServiceSelectorUiItem,
    ) : SearchMyPromotionAction
}

internal sealed interface SearchMyPromotionEffect {
    data class ShowError(val errorCode: String) : SearchMyPromotionEffect
    data class ShowServiceSelector(
        val voucher: MyVoucherListItem,
        val services: List<ServiceSelectorUiItem>,
    ) : SearchMyPromotionEffect
}
