package com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion

import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.ui.entry.AppliedDiscount
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyVoucherListItem
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.TabItem

internal data class ChoosePromotionUiState(
    val hasLoadedInitial: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isLoadingMoreOther: Boolean = false,
    val isValidating: Boolean = false,
    val isEmpty: Boolean = false,
    val tabs: List<TabItem> = emptyList(),
    val selectedTabCode: String? = null,
    val keyword: String = "",
    // my vouchers pagination
    val page: Int = 0,
    val size: Int = 10,
    val isLastPage: Boolean = false,
    // other vouchers pagination
    val otherPage: Int = 0,
    val otherSize: Int = 10,
    val isLastOtherPage: Boolean = true,
    val vouchers: List<MyVoucherListItem> = emptyList(),
    val otherVouchers: List<MyVoucherListItem> = emptyList(),
)

internal sealed interface ChoosePromotionAction {
    data object LoadInitial : ChoosePromotionAction

    /**
     * Truyền data đã load sẵn từ PRMEndowView để tránh double API call.
     * Nếu cả hai list đều rỗng → ViewModel sẽ tự gọi API.
     *
     * Mang [EligibleOffer] chứ không phải model UI: ô tìm kiếm lọc trên `campaignName` của
     * bản gốc, và `findEligible` không nhận `keyword` để lọc phía server.
     */
    data class PreloadVouchers(
        val myOffers: List<EligibleOffer>,
        val otherOffers: List<EligibleOffer>,
    ) : ChoosePromotionAction

    data object Refresh : ChoosePromotionAction

    /**
     * Người dùng gõ một ký tự. Debounce rồi lọc — cùng khuôn với `SearchMyPromotionAction.QueryChanged`.
     * Khác ở chỗ màn này lọc **trong bộ nhớ**: `findEligible` không nhận `keyword`.
     */
    data class QueryChanged(val keyword: String) : ChoosePromotionAction

    /** Bấm Enter / nút tìm: lọc ngay, bỏ qua debounce. */
    data object Search : ChoosePromotionAction

    /** Xoá trắng ô tìm kiếm → hiện lại toàn bộ danh sách đã tải. */
    data object ClearKeyword : ChoosePromotionAction

    data object LoadMoreMyVouchers : ChoosePromotionAction
    data object LoadMoreOtherVouchers : ChoosePromotionAction

    data class ValidateAndApply(
        val selected: List<MyVoucherListItem>,
    ) : ChoosePromotionAction
}

internal sealed interface ChoosePromotionEffect {
    data class OpenVoucherDetail(val voucherId: String) : ChoosePromotionEffect
    data class ShowError(val errorCode: String) : ChoosePromotionEffect

    /**
     * validateStackableDiscounts thành công.
     * [details] = list từ discountDetails của response.
     */
    data class ApplyValidatedVouchers(
        val details: List<AppliedDiscount>,
    ) : ChoosePromotionEffect
}