package com.ttcn.promotionsdk.presentation.choosepromotion

import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.common.daysUntil
import com.ttcn.promotionsdk.presentation.common.ExpiryWarning
import com.ttcn.promotionsdk.presentation.common.RejectedOffer
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionTab

/**
 * Contract của màn "ChoosePromotion" — **State / Intent / model hiển thị**, dùng chung Android & iOS.
 *
 * Tách khỏi [ChoosePromotionStore] để đọc được "màn này có dữ liệu gì, nhận được lệnh gì" mà không phải
 * lội qua phần điều phối. Logic nằm ở store; ở đây chỉ có cấu trúc, **không** chuỗi hiển thị.
 */
public data class ChoosePromotionState(
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
    /**
     * Lượt nạp gần nhất **hỏng** (mạng/HTTP) — khác [errorCode] ở chỗ **bền**.
     *
     * [errorCode] là một-lần: native hiện xong là `dispatch(ConsumeError)` xoá ngay, nên không dùng
     * để quyết định "có hiện view rỗng không" — hiện được một nhịp rồi biến mất, màn trắng trơn.
     */
    val loadFailed: Boolean = false,
    /**
     * Đang chờ `validateStackableDiscounts` của lượt bấm "Áp dụng" — bật từ lúc bấm tới lúc có
     * response (kể cả response lỗi).
     *
     * Chỉ để **khoá nút**, không phải cờ loading của màn: shimmer/pull-to-refresh không đọc cờ này.
     * Việc validate chạy ở `OfferWidgetStore` (widget) chứ không ở store này, nên native phải báo hai đầu
     * bằng [ChoosePromotionIntent.ApplyStarted] / [ChoosePromotionIntent.ApplyFinished].
     */
    val isApplying: Boolean = false,
    /**
     * Id các ưu đãi bị `validateStackableDiscounts` **từ chối** ở một lượt bấm "Áp dụng" trước đó.
     *
     * Hệ quả **duy nhất**: ưu đãi đó `isUsable = false` (mờ đi, mất ô tick, bấm không ăn) và bị bỏ
     * khỏi [selectedIds]. **Không** gắn thêm trạng thái "Chưa đủ điều kiện áp dụng" lên card — đó là
     * trạng thái của `findEligible` (`usable = false`, kèm `unmatchedRules`), nói về điều kiện đơn
     * hàng; bị từ chối lúc validate là chuyện khác và lý do đã hiện ở popup ([applyMessage]).
     *
     * Chỉ giữ **id**, không giữ câu lý do: câu đó chỉ dùng một lần cho popup, giữ lại theo từng item
     * là mời gọi đem nó ra hiển thị lên card.
     *
     * Đây là **override cục bộ của màn**, không phải dữ liệu server trả trong `findEligible`: server
     * vẫn đánh `usable = true` cho ưu đãi này (nó đủ điều kiện *về nguyên tắc*), chỉ khi ghép với bộ
     * ưu đãi đang chọn + đơn hàng hiện tại mới hỏng. Vì vậy nó phải **sống qua mọi lượt nạp lại**
     * trong màn (kể cả kéo-để-tải-lại): xoá đi là ưu đãi vừa báo hỏng lại sáng lên chọn được.
     *
     * Chết theo store, tức theo màn — mở lại màn "Chọn ưu đãi" là hỏi lại server từ đầu.
     */
    val rejectedIds: List<String> = emptyList(),
    /**
     * Câu báo lỗi **một-lần** của lượt "Áp dụng" vừa bị từ chối — native hiện popup rồi
     * `dispatch(ConsumeApplyMessage)`.
     *
     * Tách khỏi [errorCode]: [errorCode] là **mã** để native map sang chuỗi tài nguyên
     * (`mapPromotionError` / `PromotionUIStrings.errorMessage`), còn đây là **chuỗi thô của server**
     * — map nó qua bảng mã là vứt đúng cái thông tin user cần.
     */
    val applyMessage: String? = null,
)

public sealed interface ChoosePromotionIntent {
    public data object LoadInitial : ChoosePromotionIntent
    public data class Preload(
        val myOffers: List<EligibleOffer>,
        val otherOffers: List<EligibleOffer>,
        val myIsLastPage: Boolean,
        val otherIsLastPage: Boolean,
    ) : ChoosePromotionIntent
    public data object Refresh : ChoosePromotionIntent
    public data class QueryChanged(val keyword: String) : ChoosePromotionIntent
    public data object Search : ChoosePromotionIntent
    /**
     * Xoá trắng từ khoá → nạp lại danh sách đầy đủ ngay, không chờ debounce.
     *
     * Native **không cần** tự rẽ nhánh "gõ trắng thì gửi cái này": [QueryChanged] với chuỗi rỗng đã
     * chạy đúng đường đó rồi (xem `ChoosePromotionStore.onQueryChanged`). Trước đây cả
     * `ChoosePromotionFragment.setupSearch` lẫn `ChoosePromotionViewModel.query(_:)` bên iOS đều
     * chép cùng một `if keyword.isEmpty()`. Giữ intent này cho nút "X" xoá tường minh.
     */
    public data object ClearKeyword : ChoosePromotionIntent
    public data object LoadMoreMyVouchers : ChoosePromotionIntent
    public data object LoadMoreOtherVouchers : ChoosePromotionIntent
    /** Seed các voucher pre-select (từ discount đang áp trước đó). */
    public data class SetPreSelected(val ids: List<String>) : ChoosePromotionIntent
    /**
     * Seed pre-select rồi **nạp lại danh sách từ server**, đúng một lần cho cả vòng đời store.
     *
     * Cờ gác vẫn cần: Android gọi trong `observeData()` (chạy ở `onViewCreated`), nên view dựng lại
     * là bắn thêm lần nữa — ghi đè `selectedIds` về bộ đã áp ban đầu (tick mới của user biến mất) và
     * gọi thừa một lượt `findEligible`. iOS từng chặn bằng cờ `didStart` riêng trong VM; nay cờ nằm
     * ở store nên hai bên không thể lệch.
     *
     * **Không còn nhận danh sách preload.** Trước đây intent này ôm cả `myOffers`/`otherOffers` +
     * cờ phân trang mà widget đã nạp, và store dùng thẳng chúng thay vì gọi mạng — mở màn ra là thấy
     * dữ liệu của thời điểm widget nạp, có thể đã cũ (ngân sách hết, voucher vừa bị dùng ở thiết bị
     * khác). Nay **luôn** gọi `findEligible`, nên cờ phân trang cũng lấy từ chính response đó.
     * [Preload] vẫn còn cho nơi nào thật sự có dữ liệu sẵn và không muốn gọi mạng.
     */
    public data class SeedOnce(val preSelectedIds: List<String>) : ChoosePromotionIntent
    /** Chọn/bỏ chọn 1 ưu đãi theo id. */
    public data class ToggleSelection(val id: String) : ChoosePromotionIntent
    /** Bấm "Xem thêm/Thu gọn" nhóm của tôi. */
    public data object SeeMoreMy : ChoosePromotionIntent
    public data object ConsumeError : ChoosePromotionIntent

    /**
     * User vừa bấm "Áp dụng" và lượt validate đã gửi đi → khoá nút ([ChoosePromotionState.isApplying]).
     *
     * Phải do native bắn vì lượt validate không chạy ở store này (nó nằm ở `OfferWidgetStore` của widget).
     * **Bắt buộc có [ApplyFinished] đối xứng ở MỌI nhánh kết thúc** — quên một nhánh là nút chết luôn.
     */
    public data object ApplyStarted : ChoosePromotionIntent

    /** Lượt validate đã có kết quả (thành công hay lỗi đều tính) → mở khoá nút. */
    public data object ApplyFinished : ChoosePromotionIntent

    /**
     * `validateStackableDiscounts` trả `valid = false` cho [items] → **disable tại chỗ** những ưu đãi
     * đó (kèm câu của server) và bỏ tick chúng; màn KHÔNG đóng.
     *
     * Bao luôn phần việc của [ApplyFinished] (mở khoá nút), nên native chỉ bắn một intent cho nhánh
     * này — bắn cả hai cũng vô hại nhưng thừa.
     *
     * Vì sao là intent chứ không phải store tự biết: lượt validate chạy ở `OfferWidgetStore` (widget), y
     * như [ApplyStarted]/[ApplyFinished]. `OfferWidgetStore.validateAndApply` trả
     * `OfferWidgetApplyOutcome.Rejected` và native chuyển thẳng danh sách sang đây.
     */
    public data class ApplyRejected(val items: List<RejectedOffer>) : ChoosePromotionIntent

    /** Đã hiện xong [ChoosePromotionState.applyMessage] → xoá, để lượt sau không thấy câu cũ. */
    public data object ConsumeApplyMessage : ChoosePromotionIntent
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
public fun ChoosePromotionState.shouldLoadMoreOther(visibleIndex: Int): Boolean =
    !otherIsLastPage && !isLoadingMoreOther && !isLoading &&
        otherOffers.isNotEmpty() && visibleIndex >= otherOffers.lastIndex

/** Số item "Ưu đãi của tôi" hiện khi thu gọn — dùng chung 2 nền tảng. */
public const val COLLAPSED_MY_COUNT: Int = 2

/** Trạng thái nút "Xem thêm/Thu gọn" nhóm của tôi — quy tắc dùng chung, native chỉ render. */
public enum class ChooseSeeMoreState { HIDDEN, EXPAND, COLLAPSE }

/**
 * - `HIDDEN` khi số item đã nạp không vượt [COLLAPSED_MY_COUNT]: lúc thu gọn đã thấy hết, nút không
 *   có gì để mở thêm. **Không** xét [ChoosePromotionState.myIsLastPage] ở nhánh này — nhóm của tôi
 *   nạp theo trang 10 item, nên ≤ [COLLAPSED_MY_COUNT] item nghĩa là server đã trả hết.
 * - `COLLAPSE` khi đang mở hết và không còn trang.
 * - `EXPAND` cho phần còn lại: hoặc còn item chưa hiện, hoặc còn trang để nạp.
 */
public fun ChoosePromotionState.mySeeMoreState(): ChooseSeeMoreState = when {
    myOffers.size <= COLLAPSED_MY_COUNT -> ChooseSeeMoreState.HIDDEN
    myExpanded && myIsLastPage -> ChooseSeeMoreState.COLLAPSE
    else -> ChooseSeeMoreState.EXPAND
}

/** Danh sách "Ưu đãi của tôi" đang hiển thị theo trạng thái mở/thu gọn — dùng chung. */
public fun ChoosePromotionState.visibleMyOffers(): List<ChooseOffer> =
    if (myExpanded) myOffers else myOffers.take(COLLAPSED_MY_COUNT)

/** Toàn bộ ưu đãi đã nạp của cả hai nhóm, đã bóc về domain model. */
public fun ChoosePromotionState.allOffers(): List<EligibleOffer> =
    (myOffers + otherOffers).map { it.source }

/**
 * Ưu đãi user **đang chọn** — thứ bấm "Áp dụng" trả về widget (`OfferWidgetStore` lo validate & áp).
 *
 * Trước đây mỗi nền tảng tự lọc: `ChoosePromotionViewModel.selectedOffers()` bên Android và
 * `ChoosePromotionViewModel.selectedOffers()` + `allLoaded(_:)` bên iOS — hai đoạn code khác ngôn
 * ngữ nhưng cùng một luật, và luật này quyết định **ưu đãi nào được áp vào đơn**. Lệch một chỗ là
 * lệch tiền.
 */
public fun ChoosePromotionState.selectedOffers(): List<EligibleOffer> =
    allOffers().filter { it.id in selectedIds }

/**
 * Thanh "Đã chọn N voucher" — chỉ ở chế độ chọn nhiều **và** đang có item được chọn.
 * (Android `updateApplyButtonState`, iOS `Display.showsSelectedCount`.)
 */
public fun ChoosePromotionState.showsSelectedCount(): Boolean =
    isMultiSelection && selectedIds.isNotEmpty()

/**
 * Nút "Áp dụng" bấm được chưa — **rule dùng chung**, hai nền tảng chỉ render theo.
 *
 * Ba điều kiện, mỗi cái chặn một tình huống thật:
 *
 * 1. **Chưa chọn gì** → disable. Cả hai nền tảng đều bỏ qua danh sách rỗng, để nút bấm được chỉ tạo
 *    cảm giác app treo.
 * 2. **Đang kéo-để-tải-lại** → disable tạm. Giữa lúc nạp lại, danh sách cũ vẫn nằm trên màn nhưng
 *    server chưa trả lời voucher đang tick còn hợp lệ không. Cho bấm lúc này là gửi validate một
 *    voucher mà chính mình sắp biết là hỏng.
 * 3. **Voucher đang tick không còn dùng được** sau khi nạp lại → disable. Hai kiểu "không còn":
 *    server trả về không dùng được nữa, hoặc **biến mất hẳn** khỏi danh sách — trường hợp sau lộ ra
 *    ở chỗ số offer khớp được ít hơn số id đang tick.
 *
 * 4. **Đang chờ kết quả áp** ([ChoosePromotionState.isApplying]) → disable tới khi có response.
 *    Không có nhánh này thì user bấm liên tiếp là gửi n lượt `validateStackableDiscounts` chồng nhau
 *    cho cùng một bộ voucher, và n callback cùng chạy về (n popup lỗi, hoặc pop màn nhiều lần).
 *
 * Xét [ChooseOffer.isUsable] chứ không phải cờ thô `source.usable`: `isUsable` đã gộp cả **hết hạn**
 * và cả ưu đãi vừa bị server từ chối ở lượt "Áp dụng" trước ([ChoosePromotionState.rejectedIds]
 * — xem [toChooseOffer]). Voucher vừa hết hạn ngay lúc nạp lại thì `source.usable` vẫn `true`, chỉ
 * `isUsable` bắt được.
 */
public fun ChoosePromotionState.canApply(): Boolean {
    val ids = selectedIds.toSet()
    if (ids.isEmpty()) return false
    if (isRefreshing) return false
    if (isApplying) return false
    val selected = (myOffers + otherOffers).filter { it.source.id in ids }
    return selected.map { it.source.id }.toSet().size == ids.size && selected.all { it.isUsable }
}

/**
 * Hiện view "không tìm thấy kết quả" thay cho list.
 *
 * **Có từ khoá** mới tính — list rỗng lúc không tìm kiếm là "chưa có ưu đãi nào", không phải "tìm
 * không ra". Cùng luật với [SearchMyPromotionState.showsNoResult][com.ttcn.promotionsdk.presentation.searchmypromotion.showsNoResult].
 */
public fun ChoosePromotionState.showsNoResult(): Boolean =
    keyword.isNotBlank() && !isLoading && isEmpty

/**
 * Hiện view rỗng thay cho list: tìm không ra kết quả, **hoặc** lượt nạp vừa hỏng.
 *
 * Nhánh thứ hai là bắt buộc cho kéo-để-tải-lại: refresh lỗi thì store đã xoá sạch danh sách cũ, mà
 * [showsNoResult] lại đòi có từ khoá — không có nhánh này thì user kéo, API hỏng, và nhận về một màn
 * trắng hoàn toàn không giải thích gì.
 */
public fun ChoosePromotionState.showsEmptyView(): Boolean =
    showsNoResult() || (loadFailed && !isLoading && !isRefreshing)

/** Từ khoá dùng để tô đậm đoạn khớp trên card; rỗng = không tô. */
public fun ChoosePromotionState.highlightKeyword(): String = keyword.trim()

/** Ưu đãi [id] có đang được tick không — cả hai nền tảng dựng card bằng cờ này. */
public fun ChoosePromotionState.isSelected(id: String): Boolean = id in selectedIds

/**
 * View-model 1 ưu đãi eligible: **bọc** domain [EligibleOffer] ([source]) + quyết định hiển thị đã tính.
 * Native format chuỗi ("Giảm X đ" từ `source.estimatedDiscount`, "Còn X ngày" từ [expiringInDays]).
 */
public data class ChooseOffer(
    val source: EligibleOffer,
    val isUsable: Boolean,
    val expiringInDays: Int?,
    /**
     * Quá hạn theo [EligibleOffer.expireDate]. Tách khỏi [isUsable] vì native cần **lý do** để chọn
     * nhãn: hết hạn → "Đã hết hạn", còn `usable=false` từ server → câu trong `unmatchedRules`. Thiếu
     * cờ này thì item hết hạn hiện badge RỖNG (nhãn bên Android bám `source.usable`, vẫn là true).
     */
    val isExpired: Boolean,
    /**
     * Ưu đãi này vừa bị `validateStackableDiscounts` từ chối ở một lượt "Áp dụng" — **lý do thứ ba**
     * làm [isUsable] = false, bên cạnh `usable` của server và hết hạn.
     *
     * Native dùng nó để **không** hiện nhãn trạng thái nào: card chỉ mờ đi. Ba lý do khác nhau về
     * cách hiển thị — hết hạn có nhãn "Đã hết hạn", `usable = false` có dải "Chưa đủ điều kiện áp
     * dụng" + câu `unmatchedRules`, còn bị từ chối thì **không nhãn nào cả** (lý do đã nói ở popup).
     */
    val isRejected: Boolean = false,
)

/**
 * Hiện dải "Chưa đủ điều kiện áp dụng" dưới card hay không.
 *
 * **Chỉ hỏi server, đúng một cờ [EligibleOffer.usable]** (`displayMode = "DISABLED"`; lý do nằm ở
 * `unmatchedRules`). Dải nói về *điều kiện của đơn hàng* — server là bên duy nhất biết chuyện đó.
 *
 * **Không** dùng [ChooseOffer.isUsable]: cờ đó là `usable && !expired`, tức đã trộn thêm hạn dùng do
 * client tự tính, nên bám vào nó là ưu đãi hết hạn cũng bị treo dải — sai nghĩa, vì hết hạn thì sửa
 * đơn kiểu gì cũng vô ích. Hết hạn đi đường riêng: mờ card + badge "Đã hết hạn" ([ChooseOffer.isExpired]).
 *
 * **Không** dùng [ChooseOffer.isRejected] ở đây: ưu đãi bị validate từ chối chỉ mờ đi, không đeo
 * thêm nhãn trạng thái nào — lý do đã hiện ở popup ([ChoosePromotionState.applyMessage]). Gắn dải
 * này vào là nói với user "đơn hàng chưa thoả điều kiện", trong khi đơn hàng không đổi gì.
 *
 * Quyết định nằm ở **một chỗ duy nhất** này, Fragment/Cell chỉ đọc.
 */
public fun ChooseOffer.showsIneligibleWarning(): Boolean = !source.usable

internal fun EligibleOffer.toChooseOffer(
    expireWarningDate: Int?,
    rejectedIds: Set<String> = emptySet(),
): ChooseOffer {
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
    // Lý do thứ ba: lượt "Áp dụng" trước đó server đã từ chối đúng ưu đãi này. Phải áp lại ở MỌI lần
    // map — kể cả trang kế và lượt kéo-để-tải-lại — nếu không nó sáng lên chọn được ngay sau khi
    // user kéo màn, và bấm "Áp dụng" lần nữa lại nhận đúng câu từ chối cũ.
    val rejected = id in rejectedIds
    val enabled = usable && !expired && !rejected

    // Ngưỡng "sắp hết hạn": [ExpiryWarning] ưu tiên giá trị vừa `remember` ở trên, thiếu thì lùi về
    // bản nhớ gần nhất. Cần cái lùi này vì luồng THƯỜNG của màn Chọn là nhận dữ liệu preload từ
    // widget — không có response nào để lấy ngưỡng, nên trước đây `expireWarningDate` luôn null và
    // dòng "HSD còn X ngày" không bao giờ hiện.
    val days = ExpiryWarning.daysIfExpiringSoon(expireDate, enabled)

    return ChooseOffer(
        source = this,
        isUsable = enabled,
        expiringInDays = days,
        isExpired = expired,
        isRejected = rejected,
    )
}
