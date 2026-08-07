package com.ttcn.prm.ui.feature.promotion.choosepromotion

import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.presentation.choosepromotion.ChooseSeeMoreState
import com.ttcn.prm.ui.feature.promotion.mypromotion.MyVoucherListItem
import com.ttcn.prm.ui.feature.promotion.mypromotion.TabItem

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
    /**
     * Phần "Ưu đãi của tôi" đang thực sự hiển thị — đã cắt theo trạng thái mở/thu gọn bằng rule dùng
     * chung `visibleMyOffers()` ở store. Fragment render thẳng, không cắt lại.
     */
    val visibleVouchers: List<MyVoucherListItem> = emptyList(),
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
     * Mang [EligibleOffer] chứ không phải model UI để giữ nguồn sự thật ở domain (store map sang
     * model UI khi publish state).
     */
    data class PreloadVouchers(
        val myOffers: List<EligibleOffer>,
        val otherOffers: List<EligibleOffer>,
        val myIsLastPage: Boolean,
        val otherIsLastPage: Boolean,
    ) : ChoosePromotionAction

    data object Refresh : ChoosePromotionAction

    /**
     * Người dùng gõ một ký tự. Store debounce 400ms rồi **reload server-side** kèm `keyword`
     * (v1.6 §7.3) — cùng khuôn với `SearchMyPromotionAction.QueryChanged`.
     */
    data class QueryChanged(val keyword: String) : ChoosePromotionAction

    /** Bấm Enter / nút tìm: chạy ngay, bỏ qua debounce. */
    data object Search : ChoosePromotionAction

    /** Xoá trắng ô tìm kiếm → reload danh sách đầy đủ (không gửi `keyword`). */
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
     * áp/không-đủ-điều-kiện do `EndowStore` lo (dùng chung iOS). Rỗng = **không làm gì** (không đóng
     * màn, không gỡ ưu đãi đang áp) — khớp iOS; muốn gỡ thì bấm widget `PRMEndowView`.
     */
    data class ApplySelectedOffers(
        val offers: List<EligibleOffer>,
    ) : ChoosePromotionEffect
}