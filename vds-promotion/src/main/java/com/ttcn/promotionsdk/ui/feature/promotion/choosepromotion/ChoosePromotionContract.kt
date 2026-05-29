package com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion

import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyVoucherListItem
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.TabItem

data class ChoosePromotionUiState(
    val hasLoadedInitial: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isLoadingMoreOther: Boolean = false,
    val isEmpty: Boolean = false,
    val tabs: List<TabItem> = emptyList(),
    val selectedTabCode: String? = null,
    val keyword: String = "",
    // my vouchers pagination
    val page: Int = 0,
    val size: Int = 10,
    val isLastPage: Boolean = true,
    // other vouchers pagination
    val otherPage: Int = 0,
    val otherSize: Int = 10,
    val isLastOtherPage: Boolean = true,
    val vouchers: List<MyVoucherListItem> = emptyList(),
    val otherVouchers: List<MyVoucherListItem> = emptyList(),
)

sealed interface ChoosePromotionAction {
    data class InitWithData(
        val myVouchers: List<MyVoucherListItem>,
        val otherVouchers: List<MyVoucherListItem>,
    ) : ChoosePromotionAction
    data object LoadInitialIfNeeded : ChoosePromotionAction
    data object Refresh : ChoosePromotionAction
    data class SearchKeyword(val keyword: String) : ChoosePromotionAction
    data object LoadMoreMyVouchers : ChoosePromotionAction
    data object LoadMoreOtherVouchers : ChoosePromotionAction
}

sealed interface ChoosePromotionEffect {
    data class OpenVoucherDetail(val voucherId: String) : ChoosePromotionEffect
    data class ShowError(val errorCode: String) : ChoosePromotionEffect
}