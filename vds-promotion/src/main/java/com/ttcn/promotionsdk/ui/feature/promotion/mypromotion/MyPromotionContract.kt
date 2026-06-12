package com.ttcn.promotionsdk.ui.feature.promotion.mypromotion

import com.ttcn.promotionsdk.core.data.dto.voucher.VoucherStatus
import com.ttcn.promotionsdk.core.domain.model.VoucherItem
import com.ttcn.promotionsdk.core.domain.model.VoucherTabItem

data class MyPromotionUiState(
    val hasLoadedInitial: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isRefreshingTab: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isEmpty: Boolean = false,
    val tabs: List<TabItem> = emptyList(),
    val selectedTabCode: String? = null,
    val keyword: String = "",
    val page: Int = 0,
    val size: Int = 10,
    val isLastPage: Boolean = true,
    val vouchers: List<MyVoucherListItem> = emptyList(),
)

sealed interface MyPromotionAction {
    data object LoadInitialIfNeeded : MyPromotionAction
    data object Refresh : MyPromotionAction
    data class SelectTab(val tabCode: String) : MyPromotionAction
    data class SearchKeyword(val keyword: String) : MyPromotionAction
    data object LoadMore : MyPromotionAction
}

sealed interface MyPromotionEffect {
    data class OpenVoucherDetail(val voucherId: String) : MyPromotionEffect
    data class ShowError(val errorCode: String) : MyPromotionEffect
}

data class MyVoucherListItem(
    val voucherId: String,
    val campaignId: String = "",
    val merchantName: String,
    val title: String,
    val description: String,
    val logo: String,
    val expirationDate: String,
    val displayStatusLabel: String,
    val status: VoucherStatus,
    val objectType: String = "CAMPAIGN",
    val isSelected: Boolean = false,
    val isAutoApplied: Boolean = false,
)

data class TabItem(
    val code: String,
    val label: String,
    val count: Int,
    val order: Int,
)

fun VoucherItem.toMyVoucherListItem(): MyVoucherListItem {
    return MyVoucherListItem(
        voucherId = voucherId,
        campaignId = campaignId.orEmpty(),
        merchantName = merchantName.orEmpty(),
        title = title.orEmpty(),
        description = description.orEmpty(),
        logo = logo.orEmpty(),
        expirationDate = expirationDate.orEmpty(),
        displayStatusLabel = displayStatusLabel.orEmpty(),
        status = VoucherStatus.from(status),
        objectType = objectType,
    )
}

fun VoucherTabItem.toMyVoucherTabUi(): TabItem {
    return TabItem(
        code = code,
        label = label,
        count = count ?: 0,
        order = order ?: Int.MAX_VALUE,
    )
}
