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
    val mySize: Int = 20,
    val myIsLastPage: Boolean = false,
    val otherPage: Int = 0,
    val otherSize: Int = 20,
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
    /**
     * Xoá trắng từ khoá → nạp lại danh sách đầy đủ ngay, không chờ debounce.
     *
     * Native **không cần** tự rẽ nhánh "gõ trắng thì gửi cái này": [QueryChanged] với chuỗi rỗng đã
     * chạy đúng đường đó rồi (xem `ChoosePromotionStore.onQueryChanged`). Trước đây cả
     * `ChoosePromotionFragment.setupSearch` lẫn `ChoosePromotionViewModel.query(_:)` bên iOS đều
     * chép cùng một `if keyword.isEmpty()`. Giữ intent này cho nút "X" xoá tường minh.
     */
    data object ClearKeyword : ChoosePromotionIntent
    data object LoadMoreMyVouchers : ChoosePromotionIntent
    data object LoadMoreOtherVouchers : ChoosePromotionIntent
    /** Seed các voucher pre-select (từ discount đang áp trước đó). */
    data class SetPreSelected(val ids: List<String>) : ChoosePromotionIntent
    /**
     * Seed pre-select + preload **đúng một lần cho cả vòng đời store**.
     *
     * Android gọi `SetPreSelected` + `Preload` trong `observeData()` (chạy ở `onViewCreated`), nên
     * view dựng lại là ghi đè `selectedIds` về bộ đã áp ban đầu — tick mới của user biến mất — và
     * `preload()` rewind `otherOffers` về trang đầu, xoá các trang đã cuộn. iOS chặn bằng cờ
     * `didStart` trong VM. Nay cờ nằm ở store nên hai bên không thể lệch.
     */
    data class SeedOnce(
        val preSelectedIds: List<String>,
        val myOffers: List<EligibleOffer>,
        val otherOffers: List<EligibleOffer>,
        val myIsLastPage: Boolean,
        val otherIsLastPage: Boolean,
    ) : ChoosePromotionIntent
    /** Chọn/bỏ chọn 1 ưu đãi theo id. */
    data class ToggleSelection(val id: String) : ChoosePromotionIntent
    /** Bấm "Xem thêm/Thu gọn" nhóm của tôi. */
    data object SeeMoreMy : ChoosePromotionIntent
    data object ConsumeError : ChoosePromotionIntent
}

/**
 * Còn phải nạp trang kế của nhóm "Ưu đãi khác" không, khi item thứ [visibleIndex] sắp hiện.
 *
 * Rule dùng chung, thay hai điều kiện từng lệch nhau: iOS bắn từ `willDisplay` khi
 * `row == items.count - 1` (đếm trong section, chạy cả lúc list ngắn không cuộn được), Android bắn
 * từ `onScrolled` với `dy > 0` và ngưỡng `findLastVisibleItemPosition() >= itemCount - 2` (đếm trên
 * TOÀN list gồm header/divider/see-more). Cùng dữ liệu, hai thời điểm nạp khác nhau.
 *
 * Chốt theo cách iOS — nạp khi chạm item cuối, **không** đòi phải có cú cuộn: list ngắn hơn màn hình
 * thì Android cũ không bao giờ nạp thêm được.
 */
fun ChoosePromotionState.shouldLoadMoreOther(visibleIndex: Int): Boolean =
    !otherIsLastPage && !isLoadingMoreOther && !isLoading &&
        otherOffers.isNotEmpty() && visibleIndex >= otherOffers.lastIndex

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

/** Toàn bộ ưu đãi đã nạp của cả hai nhóm, đã bóc về domain model. */
fun ChoosePromotionState.allOffers(): List<EligibleOffer> =
    (myOffers + otherOffers).map { it.source }

/**
 * Ưu đãi user **đang chọn** — thứ bấm "Áp dụng" trả về widget (`EndowStore` lo validate & áp).
 *
 * Trước đây mỗi nền tảng tự lọc: `ChoosePromotionViewModel.selectedOffers()` bên Android và
 * `ChoosePromotionViewModel.selectedOffers()` + `allLoaded(_:)` bên iOS — hai đoạn code khác ngôn
 * ngữ nhưng cùng một luật, và luật này quyết định **ưu đãi nào được áp vào đơn**. Lệch một chỗ là
 * lệch tiền.
 */
fun ChoosePromotionState.selectedOffers(): List<EligibleOffer> =
    allOffers().filter { it.id in selectedIds }

/**
 * Thanh "Đã chọn N voucher" — chỉ ở chế độ chọn nhiều **và** đang có item được chọn.
 * (Android `updateApplyButtonState`, iOS `Display.showsSelectedCount`.)
 */
fun ChoosePromotionState.showsSelectedCount(): Boolean =
    isMultiSelection && selectedIds.isNotEmpty()

/**
 * Nút "Áp dụng" bấm được chưa. Chưa chọn gì thì **disable**: cả hai nền tảng đều bỏ qua danh sách
 * rỗng, để nút bấm được chỉ tạo cảm giác app treo.
 */
fun ChoosePromotionState.canApply(): Boolean = selectedIds.isNotEmpty()

/**
 * Hiện view "không tìm thấy kết quả" thay cho list.
 *
 * **Có từ khoá** mới tính — list rỗng lúc không tìm kiếm là "chưa có ưu đãi nào", không phải "tìm
 * không ra". Cùng luật với [SearchMyPromotionState.showsNoResult][com.ttcn.promotionsdk.presentation.searchmypromotion.showsNoResult].
 */
fun ChoosePromotionState.showsNoResult(): Boolean =
    keyword.isNotBlank() && !isLoading && isEmpty

/** Từ khoá dùng để tô đậm đoạn khớp trên card; rỗng = không tô. */
fun ChoosePromotionState.highlightKeyword(): String = keyword.trim()

/** Ưu đãi [id] có đang được tick không — cả hai nền tảng dựng card bằng cờ này. */
fun ChoosePromotionState.isSelected(id: String): Boolean = id in selectedIds

/**
 * View-model 1 ưu đãi eligible: **bọc** domain [EligibleOffer] ([source]) + quyết định hiển thị đã tính.
 * Native format chuỗi ("Giảm X đ" từ `source.estimatedDiscount`, "Còn X ngày" từ [expiringInDays]).
 */
data class ChooseOffer(
    val source: EligibleOffer,
    val isUsable: Boolean,
    val expiringInDays: Int?,
    /**
     * Quá hạn theo [EligibleOffer.expireDate]. Tách khỏi [isUsable] vì native cần **lý do** để chọn
     * nhãn: hết hạn → "Đã hết hạn", còn `usable=false` từ server → câu trong `unmatchedRules`. Thiếu
     * cờ này thì item hết hạn hiện badge RỖNG (nhãn bên Android bám `source.usable`, vẫn là true).
     */
    val isExpired: Boolean,
)

internal fun EligibleOffer.toChooseOffer(expireWarningDate: Int?): ChooseOffer {
    // Xem chú thích cùng nội dung ở `VoucherItem.toMyPromotionVoucher`.
    ExpiryWarning.remember(expireWarningDate)

    // **Hết hạn thì không dùng được**, kể cả khi server chưa đánh `displayMode = "DISABLED"`
    // (`EligibleOffer.usable`). Trước đây màn này tin cờ server tuyệt đối nên ưu đãi quá hạn vẫn hiện
    // sáng và tick chọn được — lệch hẳn "Ưu đãi của tôi", nơi `VoucherStatus.EXPIRED` cho `isUsable`
    // = false (`VoucherItem.toMyPromotionVoucher`).
    //
    // Mốc là `< 0`, KHÔNG phải `<= 0`: `daysUntil` làm tròn lên nên 0 nghĩa là "hết hạn trong hôm
    // nay" — vẫn dùng được, và đó cũng là ngày đầu của dải "sắp hết hạn" (`it in 0..warn`).
    val expired = daysUntil(expireDate)?.let { it < 0 } == true
    val enabled = usable && !expired

    // Ngưỡng "sắp hết hạn": [ExpiryWarning] ưu tiên giá trị vừa `remember` ở trên, thiếu thì lùi về
    // bản nhớ gần nhất. Cần cái lùi này vì luồng THƯỜNG của màn Chọn là nhận dữ liệu preload từ
    // widget — không có response nào để lấy ngưỡng, nên trước đây `expireWarningDate` luôn null và
    // dòng "HSD còn X ngày" không bao giờ hiện.
    val days = ExpiryWarning.daysIfExpiringSoon(expireDate, enabled)

    return ChooseOffer(source = this, isUsable = enabled, expiringInDays = days, isExpired = expired)
}
