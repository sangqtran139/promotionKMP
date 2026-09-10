package com.ttcn.promotionsdk.presentation.offerwidget

import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.domain.model.redemption.RedemptionItemRequest
import com.ttcn.promotionsdk.domain.model.stackablediscount.DiscountItemRequest
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsResult
import com.ttcn.promotionsdk.presentation.common.RejectedOffer

/**
 * Contract của widget ưu đãi — **State / Intent / model hiển thị**, dùng chung Android & iOS.
 *
 * Tách khỏi [OfferWidgetStore] để đọc được "màn này có dữ liệu gì, nhận được lệnh gì" mà không phải
 * lội qua phần điều phối. Logic nằm ở store; ở đây chỉ có cấu trúc, **không** chuỗi hiển thị.
 */
public data class OfferWidgetState(
    val hasLoadedInitial: Boolean = false,
    val isLoading: Boolean = false,
    /** Ưu đãi từ `findEligible` — truyền thẳng sang màn "Chọn ưu đãi" để khỏi gọi API hai lần. */
    val myOffers: List<EligibleOffer> = emptyList(),
    val otherOffers: List<EligibleOffer> = emptyList(),
    /**
     * Cờ phân trang của chính lần `findEligible` này — màn "Chọn ưu đãi" nhận qua
     * `ChoosePromotionIntent.Preload` để biết còn trang nào không.
     */
    val myIsLastPage: Boolean = true,
    val otherIsLastPage: Boolean = true,
    val totalVoucherCount: Int = 0,
    val appliedDiscounts: List<OfferWidgetAppliedDiscount> = emptyList(),
    val discountUnavailable: Boolean = false,
    val isValidating: Boolean = false,
    /** Mã lỗi một-lần; native hiển thị rồi `dispatch(ConsumeError)`. */
    val errorCode: String? = null,
) {
    /**
     * Trạng thái hiển thị của widget — **quyết định dùng chung**, native chỉ render theo.
     *
     * Là **property trong class**, không phải extension: extension property của Kotlin bridge sang
     * Swift thành hàm tĩnh (`OfferWidgetContractKt.widgetState(state)`), nên iOS đã bỏ qua và chép lại
     * nguyên 4 nhánh này bằng Swift — rule đụng tiền mà tồn tại hai bản, và 8 test ở `OfferWidgetStoreTest`
     * chỉ phủ bản Kotlin. Để trong class thì cả hai nền tảng cùng đọc `state.widgetState`.
     *
     * Thứ tự nhánh có ý nghĩa: `UNAVAILABLE` phải xét TRƯỚC `APPLIED` — ưu đãi đã áp nhưng không còn
     * hợp lệ vẫn có `appliedDiscounts` không rỗng.
     *
     * Khai ngoài constructor nên không lọt vào `equals`/`hashCode`/`copy` của data class.
     */
    val widgetState: OfferWidgetDisplayState
        get() = when {
            discountUnavailable && appliedDiscounts.isNotEmpty() -> OfferWidgetDisplayState.UNAVAILABLE
            appliedDiscounts.isNotEmpty() -> OfferWidgetDisplayState.APPLIED
            totalVoucherCount > 0 -> OfferWidgetDisplayState.NOT_APPLIED
            else -> OfferWidgetDisplayState.EMPTY
        }
}

/** Trạng thái hiển thị widget — **quyết định dùng chung** (rule cũ ở `PRMOfferWidget.renderState`). */
public enum class OfferWidgetDisplayState { EMPTY, NOT_APPLIED, APPLIED, UNAVAILABLE }

/**
 * Kết quả validate cho 1 ưu đãi — model **shared** (đối xứng `AppliedDiscount` public của Android /
 * kiểu tương ứng iOS). Mỗi nền tảng map sang model public riêng cho callback/host.
 */
public data class OfferWidgetAppliedDiscount(
    val objectId: String,
    val objectType: String,
    val valid: Boolean,
    val calculatedDiscount: String,
    val eligibilityStatus: String,
    val tags: List<String> = emptyList(),
    /**
     * Tên voucher, lấy từ chính [EligibleOffer] mà user vừa chọn — **không** phải từ response validate.
     *
     * Trước đây model này chỉ có id + số tiền, nên chip trên widget chỉ hiện được `tags[0]` (nhãn
     * server gửi kèm) hoặc số tiền giảm. Server trả `tags: []` và `calculatedDiscount: 0` là chip ra
     * "0đ" — user chọn voucher xong nhìn widget không thấy voucher đâu.
     *
     * Nghịch lý: lúc gọi `validateAndApply(offers)` SDK đang cầm sẵn `voucherName`, rồi vứt đi và đi
     * xin lại chữ từ server. Nay giữ luôn.
     */
    val voucherName: String? = null,
    /** Logo voucher, cùng nguồn với [voucherName]. */
    val logoUrl: String? = null,
    /** Lý do server từ chối (nếu [valid] = false) — để native hiện/log, không còn im lặng. */
    val validationMessages: List<String> = emptyList(),
)

/**
 * Nhận thẳng [offer] thay vì hai chuỗi id/type rời: offer mang sẵn tên + logo, mà đó chính là thứ
 * widget cần hiển thị. Bản cũ chỉ nhận `objectId`/`objectType` nên phần nhận dạng voucher bị rơi
 * ngay tại đây.
 */
internal fun ValidateDiscountsResult.toOfferWidgetAppliedDiscount(offer: EligibleOffer) =
    OfferWidgetAppliedDiscount(
        objectId = offer.id,
        objectType = offer.objectType,
        valid = isValidFor(offer.id),
        calculatedDiscount = discountFor(offer.id),
        eligibilityStatus = itemFor(offer.id)?.eligibilityStatus.orEmpty(),
        tags = itemFor(offer.id)?.tags.orEmpty(),
        voucherName = offer.voucherName ?: offer.campaignName,
        logoUrl = offer.logoUrl,
        validationMessages = reasonFor(offer.id),
    )

/**
 * Ưu đãi bị từ chối → [RejectedOffer] cho màn "Chọn ưu đãi" disable tại chỗ.
 *
 * Lấy câu **đầu tiên không rỗng** trong [OfferWidgetAppliedDiscount.validationMessages] — chuỗi đó đã gộp
 * sẵn lý do cấp-dòng và cấp-đơn ở [ValidateDiscountsResult.reasonFor]. Không có câu nào thì để rỗng;
 * native tự lùi về câu lỗi chung của mình, lõi không dựng chuỗi tiếng Việt.
 */
internal fun OfferWidgetAppliedDiscount.toRejectedOffer() = RejectedOffer(
    objectId = objectId,
    message = validationMessages.firstOrNull { it.isNotBlank() }.orEmpty(),
)

/**
 * Bản dùng cho lượt **validate lại** (hết ngân sách → hỏi giá mới): lúc đó trong tay chỉ còn
 * [previous] chứ không còn `EligibleOffer` gốc. Giữ nguyên tên + logo đã có, chỉ cập nhật phần
 * server vừa tính lại — không thì mỗi lần revalidate là chip mất tên, quay về hiện số tiền.
 */
internal fun ValidateDiscountsResult.toOfferWidgetAppliedDiscount(previous: OfferWidgetAppliedDiscount) =
    previous.copy(
        valid = isValidFor(previous.objectId),
        calculatedDiscount = discountFor(previous.objectId),
        eligibilityStatus = itemFor(previous.objectId)?.eligibilityStatus.orEmpty(),
        tags = itemFor(previous.objectId)?.tags.orEmpty(),
        validationMessages = reasonFor(previous.objectId),
    )

public sealed interface OfferWidgetIntent {
    /** Nạp ưu đãi widget (findEligible) — idempotent, chỉ chạy lần đầu. */
    public data object LoadInitial : OfferWidgetIntent
    /** Validate + áp danh sách ưu đãi đã chọn (từ màn Chọn). */
    public data class ValidateAndApply(val offers: List<EligibleOffer>) : OfferWidgetIntent
    /** Áp trực tiếp kết quả đã validate sẵn (host tự validate rồi đưa vào). */
    public data class SetApplied(val discounts: List<OfferWidgetAppliedDiscount>, val unavailable: Boolean) : OfferWidgetIntent
    /** Đánh dấu ưu đãi đang áp không còn khả dụng (không đổi danh sách). */
    public data object MarkUnavailable : OfferWidgetIntent
    /** Xoá toàn bộ ưu đãi đã áp → quay về NOT_APPLIED. */
    public data object ClearApplied : OfferWidgetIntent
    public data object ConsumeError : OfferWidgetIntent
}

/**
 * Kết quả một lượt [OfferWidgetStore.validateAndApply] — **một-lần**, nên trả thẳng chứ không nhét vào
 * [OfferWidgetState] (cùng lý lẽ với [OfferWidgetConfirmResult]).
 *
 * Trước đây hàm đó trả [OfferWidgetState] và nơi gọi chỉ đọc được `errorCode`, tức chỉ phân biệt được
 * "mạng hỏng" với "mọi thứ khác". Ba kết cục thật sự khác nhau về hành động thì bị gộp làm hai:
 * server **từ chối** ưu đãi (`valid = false`) rơi vào cùng nhánh với thành công, nên màn "Chọn ưu
 * đãi" đóng lại như thể đã áp xong, còn widget lặng lẽ chuyển sang `UNAVAILABLE` — user chọn voucher
 * rồi thấy nó gạch đi mà không ai nói vì sao.
 */
public sealed interface OfferWidgetApplyOutcome {
    /** Áp xong (hoặc danh sách rỗng = xoá áp). Widget đã mang [OfferWidgetState.appliedDiscounts] mới. */
    public data object Applied : OfferWidgetApplyOutcome

    /**
     * Server trả `valid = false` cho [items] → **không áp gì cả**: [OfferWidgetState.appliedDiscounts] giữ
     * nguyên bộ cũ, widget không đổi.
     *
     * Cố ý không commit: nơi gọi (màn "Chọn ưu đãi") sẽ **ở lại** để user chọn ưu đãi khác, mà widget
     * ở màn sau lưng thì đã đổi sang một bộ discount user chưa hề xác nhận. Ưu đãi đang áp trước đó
     * vẫn còn nguyên giá trị cho tới khi user chọn được cái thay thế.
     */
    public data class Rejected(val items: List<RejectedOffer>) : OfferWidgetApplyOutcome

    /** Không hỏi được server (mạng/HTTP/`data` rỗng). [errorCode] để native map sang chuỗi. */
    public data class Failed(val errorCode: String) : OfferWidgetApplyOutcome
}

/**
 * Kết quả bấm "Thanh toán" — xem [OfferWidgetStore.confirmRedemption].
 *
 * Một-lần, nên trả thẳng chứ không nhét vào [OfferWidgetState]: host hỏi "cho tôi đi tiếp không?" và cần
 * đúng một câu trả lời. Thay đổi hiển thị (giá đã revalidate) thì vẫn đi qua state như thường.
 */
public sealed interface OfferWidgetConfirmResult {
    /** Cho phép tiến hành thanh toán. Cũng là kết quả khi đơn không áp ưu đãi nào. */
    public data object Success : OfferWidgetConfirmResult
    /** Không cho đi tiếp; [errorCode] để native map sang chuỗi hiển thị. */
    public data class Failure(val errorCode: String) : OfferWidgetConfirmResult
}

internal fun List<OfferWidgetAppliedDiscount>.toCreateRedemptionRequest(orderId: String, orderValue: String) =
    CreateRedemptionRequest(
        orderId = orderId,
        orderValue = orderValue,
        items = map {
            RedemptionItemRequest(
                objectId = it.objectId,
                objectType = it.objectType,
                expectedDiscount = it.calculatedDiscount,
            )
        },
    )

internal fun List<OfferWidgetAppliedDiscount>.toValidateDiscountsRequest(orderId: String, orderValue: String) =
    ValidateDiscountsRequest(
        orderId = orderId,
        orderValue = orderValue,
        items = map { DiscountItemRequest(objectId = it.objectId, objectType = it.objectType) },
    )
