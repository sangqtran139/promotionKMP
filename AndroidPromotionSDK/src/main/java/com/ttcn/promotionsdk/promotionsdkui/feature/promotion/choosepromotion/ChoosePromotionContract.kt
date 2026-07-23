package com.ttcn.promotionsdk.promotionsdkui.feature.promotion.choosepromotion

import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.presentation.choosepromotion.ChooseSeeMoreState
import com.ttcn.promotionsdk.promotionsdkui.feature.promotion.mypromotion.MyVoucherListItem
import com.ttcn.promotionsdk.promotionsdkui.feature.promotion.mypromotion.TabItem

internal data class ChoosePromotionUiState(
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
    val isLastPage: Boolean = false,
    // other vouchers pagination
    val otherPage: Int = 0,
    val otherSize: Int = 10,
    val isLastOtherPage: Boolean = true,
    val vouchers: List<MyVoucherListItem> = emptyList(),
    val otherVouchers: List<MyVoucherListItem> = emptyList(),
    /** Cho phép chọn nhiều voucher — do store quyết định. */
    val isMultiSelection: Boolean = false,
    /** id các voucher đang chọn — selection do store (promotionLogic) quản, không giữ ở Fragment. */
    val selectedIds: List<String> = emptyList(),
    /** Nhóm "Ưu đãi của tôi" đang mở hết hay thu gọn — do store quản (SeeMoreMy). */
    val myExpanded: Boolean = false,
    /** Trạng thái nút "Xem thêm/Thu gọn" — tính bằng rule dùng chung ở store. */
    val mySeeMore: ChooseSeeMoreState = ChooseSeeMoreState.HIDDEN,
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

    /** Seed voucher pre-select (discount đang áp trước đó) — forward xuống store. */
    data class SetPreSelected(val ids: List<String>) : ChoosePromotionAction

    /** Chọn/bỏ chọn 1 voucher theo id — store quyết định rule single/multi. */
    data class ToggleSelection(val id: String) : ChoosePromotionAction

    /** Bấm "Xem thêm/Thu gọn" nhóm của tôi — store chạy state-machine. */
    data object SeeMoreMy : ChoosePromotionAction

    /** Bấm "Áp dụng": trả offers đang chọn cho widget (EndowStore validate). */
    data object ValidateAndApply : ChoosePromotionAction
}

internal sealed interface ChoosePromotionEffect {
    data class OpenVoucherDetail(val voucherId: String) : ChoosePromotionEffect
    data class ShowError(val errorCode: String) : ChoosePromotionEffect

    /**
     * Bấm "Áp dụng" → trả **offers đang chọn** cho widget (`PRMEndowView`); việc validate + quyết định
     * áp/không-đủ-điều-kiện do `EndowStore` lo (dùng chung iOS). Rỗng = bỏ áp.
     */
    data class ApplySelectedOffers(
        val offers: List<EligibleOffer>,
    ) : ChoosePromotionEffect
}