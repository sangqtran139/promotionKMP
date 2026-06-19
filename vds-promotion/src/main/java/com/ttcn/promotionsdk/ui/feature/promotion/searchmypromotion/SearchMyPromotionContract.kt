package com.ttcn.promotionsdk.ui.feature.promotion.searchmypromotion

import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyVoucherListItem

data class SearchMyPromotionUiState(
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

sealed interface SearchMyPromotionAction {
    data class QueryChanged(val keyword: String) : SearchMyPromotionAction
    data object Search : SearchMyPromotionAction
    data object LoadMore : SearchMyPromotionAction
    data object ClearKeyword : SearchMyPromotionAction
    data object Retry : SearchMyPromotionAction
}

sealed interface SearchMyPromotionEffect {
    data class ShowError(val errorCode: String) : SearchMyPromotionEffect
}
