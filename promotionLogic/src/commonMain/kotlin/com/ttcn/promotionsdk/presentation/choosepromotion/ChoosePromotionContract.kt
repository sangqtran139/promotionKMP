package com.ttcn.promotionsdk.presentation.choosepromotion

import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.common.daysUntil
import com.ttcn.promotionsdk.presentation.common.ExpiryWarning
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionTab

/**
 * Contract của màn "ChoosePromotion" — **State / Intent / model hiển thị**, dùng chung Android & iOS.
 *
 * Tách khỏi [ChoosePromotionStore] để đọc được "màn này có dữ liệu gì, nhận được lệnh gì" mà không phải
 * lội qua phần điều phối. Logic nằm ở store; ở đây chỉ có cấu trúc, **không** chuỗi hiển thị.
 */
data class ChoosePromotionState(
    val hasLoadedInitial: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isLoadingMoreOther: Boolean = false,
    val isEmpty: Boolean = false,
    val tabs: List<MyPromotionTab> = emptyList(),
    val selectedTabCode: String? = null,
    val keyword: String = "",
    val myPage: Int = 0,
    val mySize: Int = 10,
    val myIsLastPage: Boolean = false,
    val otherPage: Int = 0,
    val otherSize: Int = 10,
    val otherIsLastPage: Boolean = true,
    val myOffers: List<ChooseOffer> = emptyList(),
    val otherOffers: List<ChooseOffer> = emptyList(),
    val expireWarningDate: Int? = null,
    /** Cho phép chọn nhiều ưu đãi (mặc định chọn đơn — khớp Fragment/VC hiện tại). */
    val isMultiSelection: Boolean = false,
    /** id các ưu đãi đang chọn — **selection do store quản** (dùng chung 2 nền tảng). */
    val selectedIds: List<String> = emptyList(),
    /** Nhóm "Ưu đãi của tôi" đang mở hết hay thu gọn — do store quản (state-machine `SeeMoreMy`). */
    val myExpanded: Boolean = false,
    val errorCode: String? = null,
)

sealed interface ChoosePromotionIntent {
    data object LoadInitial : ChoosePromotionIntent
    data class Preload(
        val myOffers: List<EligibleOffer>,
        val otherOffers: List<EligibleOffer>,
        val myIsLastPage: Boolean,
        val otherIsLastPage: Boolean,
    ) : ChoosePromotionIntent
    data object Refresh : ChoosePromotionIntent
    data class QueryChanged(val keyword: String) : ChoosePromotionIntent
    data object Search : ChoosePromotionIntent
    data object ClearKeyword : ChoosePromotionIntent
    data object LoadMoreMyVouchers : ChoosePromotionIntent
    data object LoadMoreOtherVouchers : ChoosePromotionIntent
    /** Seed các voucher pre-select (từ discount đang áp trước đó). */
    data class SetPreSelected(val ids: List<String>) : ChoosePromotionIntent
    /** Chọn/bỏ chọn 1 ưu đãi theo id. */
    data class ToggleSelection(val id: String) : ChoosePromotionIntent
    /** Bấm "Xem thêm/Thu gọn" nhóm của tôi. */
    data object SeeMoreMy : ChoosePromotionIntent
    data object ConsumeError : ChoosePromotionIntent
}

/** Số item "Ưu đãi của tôi" hiện khi thu gọn — dùng chung 2 nền tảng. */
const val COLLAPSED_MY_COUNT = 2

/** Trạng thái nút "Xem thêm/Thu gọn" nhóm của tôi — quy tắc dùng chung, native chỉ render. */
enum class ChooseSeeMoreState { HIDDEN, EXPAND, COLLAPSE }

/**
 * - `HIDDEN` khi số item đã nạp không vượt [COLLAPSED_MY_COUNT]: lúc thu gọn đã thấy hết, nút không
 *   có gì để mở thêm. **Không** xét [ChoosePromotionState.myIsLastPage] ở nhánh này — nhóm của tôi
 *   nạp theo trang 10 item, nên ≤ [COLLAPSED_MY_COUNT] item nghĩa là server đã trả hết.
 * - `COLLAPSE` khi đang mở hết và không còn trang.
 * - `EXPAND` cho phần còn lại: hoặc còn item chưa hiện, hoặc còn trang để nạp.
 */
fun ChoosePromotionState.mySeeMoreState(): ChooseSeeMoreState = when {
    myOffers.size <= COLLAPSED_MY_COUNT -> ChooseSeeMoreState.HIDDEN
    myExpanded && myIsLastPage -> ChooseSeeMoreState.COLLAPSE
    else -> ChooseSeeMoreState.EXPAND
}

/** Danh sách "Ưu đãi của tôi" đang hiển thị theo trạng thái mở/thu gọn — dùng chung. */
fun ChoosePromotionState.visibleMyOffers(): List<ChooseOffer> =
    if (myExpanded) myOffers else myOffers.take(COLLAPSED_MY_COUNT)

/**
 * View-model 1 ưu đãi eligible: **bọc** domain [EligibleOffer] ([source]) + quyết định hiển thị đã tính.
 * Native format chuỗi ("Giảm X đ" từ `source.estimatedDiscount`, "Còn X ngày" từ [expiringInDays]).
 */
data class ChooseOffer(
    val source: EligibleOffer,
    val isUsable: Boolean,
    val expiringInDays: Int?,
)

internal fun EligibleOffer.toChooseOffer(expireWarningDate: Int?): ChooseOffer {
    // Xem chú thích cùng nội dung ở `VoucherItem.toMyPromotionVoucher`.
    ExpiryWarning.remember(expireWarningDate)
    val days = if (usable && expireWarningDate != null) {
        daysUntil(expireDate)?.takeIf { it in 0..expireWarningDate }
    } else null
    return ChooseOffer(source = this, isUsable = usable, expiringInDays = days)
}
