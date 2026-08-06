package com.ttcn.prm.ui.feature.promotion.mypromotion

import com.ttcn.promotionsdk.core.domain.model.voucher.ApplicableProduct
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherItem
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherStatus
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherTabItem

internal data class MyPromotionUiState(
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

/**
 * Chỉ khai báo action mà màn thật sự phát — khớp 1-1 `MyPromotionViewModel.Input` bên iOS.
 * (Tìm kiếm nằm ở màn riêng `SearchMyPromotionFragment`, không phải ở đây.)
 */
internal sealed interface MyPromotionAction {
    data object LoadInitialIfNeeded : MyPromotionAction
    data object Refresh : MyPromotionAction
    data class SelectTab(val tabCode: String) : MyPromotionAction
    data object LoadMore : MyPromotionAction
    data class OpenServiceSelector(val voucher: MyVoucherListItem) : MyPromotionAction
    data class ServiceSelected(
        val voucher: MyVoucherListItem,
        val service: ServiceSelectorUiItem,
    ) : MyPromotionAction
}

internal sealed interface MyPromotionEffect {
    data class OpenVoucherDetail(val voucherId: String) : MyPromotionEffect
    data class ShowError(val errorCode: String) : MyPromotionEffect
    data class ShowServiceSelector(
        val voucher: MyVoucherListItem,
        val services: List<ServiceSelectorUiItem>,
    ) : MyPromotionEffect
}

internal data class MyVoucherListItem(
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
    val applicableProducts: List<ApplicableProduct> = emptyList(),
    /**
     * Voucher còn dùng được — **quyết định do store tính** (`MyPromotionVoucher.isEnabled` /
     * `ChooseOffer.isUsable`). UI đọc thẳng, KHÔNG tự suy lại từ [status] (tránh 2 nền tảng lệch rule).
     */
    val isEnabled: Boolean = true,
    /**
     * Số ngày còn lại khi voucher sắp hết hạn (trong ngưỡng `expireWarningDate` của server) —
     * **quyết định do store tính**; `null` nếu không áp dụng. UI chỉ format "Còn X ngày".
     */
    val expiringInDays: Int? = null,
)

internal data class TabItem(
    val code: String,
    val label: String,
    val count: Int,
    val order: Int,
    val isDefault: Boolean = false,
)

internal fun VoucherItem.toMyVoucherListItem(): MyVoucherListItem {
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
        isAutoApplied = isAutoApplied,
        applicableProducts = applicableProducts,
    )
}

internal fun VoucherTabItem.toMyVoucherTabUi(): TabItem {
    return TabItem(
        code = code,
        label = label,
        count = count ?: 0,
        order = order ?: Int.MAX_VALUE,
        isDefault = isDefault,
    )
}
