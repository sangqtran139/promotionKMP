package com.ttcn.promotionsdk.presentation.endow

import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.domain.model.redemption.RedemptionItemRequest
import com.ttcn.promotionsdk.domain.model.stackablediscount.DiscountItemRequest
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsResult

/**
 * Contract của màn "Endow" — **State / Intent / model hiển thị**, dùng chung Android & iOS.
 *
 * Tách khỏi [EndowStore] để đọc được "màn này có dữ liệu gì, nhận được lệnh gì" mà không phải
 * lội qua phần điều phối. Logic nằm ở store; ở đây chỉ có cấu trúc, **không** chuỗi hiển thị.
 */
data class EndowState(
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
    val appliedDiscounts: List<EndowAppliedDiscount> = emptyList(),
    val discountUnavailable: Boolean = false,
    val isValidating: Boolean = false,
    /** Mã lỗi một-lần; native hiển thị rồi `dispatch(ConsumeError)`. */
    val errorCode: String? = null,
)

/** Trạng thái hiển thị widget — **quyết định dùng chung** (rule cũ ở `PRMEndowView.renderState`). */
enum class EndowWidgetState { EMPTY, NOT_APPLIED, APPLIED, UNAVAILABLE }

val EndowState.widgetState: EndowWidgetState
    get() = when {
        discountUnavailable && appliedDiscounts.isNotEmpty() -> EndowWidgetState.UNAVAILABLE
        appliedDiscounts.isNotEmpty() -> EndowWidgetState.APPLIED
        totalVoucherCount > 0 -> EndowWidgetState.NOT_APPLIED
        else -> EndowWidgetState.EMPTY
    }

/**
 * Kết quả validate cho 1 ưu đãi — model **shared** (đối xứng `AppliedDiscount` public của Android /
 * kiểu tương ứng iOS). Mỗi nền tảng map sang model public riêng cho callback/host.
 */
data class EndowAppliedDiscount(
    val objectId: String,
    val objectType: String,
    val valid: Boolean,
    val calculatedDiscount: String,
    val eligibilityStatus: String,
)

internal fun ValidateDiscountsResult.toEndowAppliedDiscount(objectId: String, objectType: String) =
    EndowAppliedDiscount(
        objectId = objectId,
        objectType = objectType,
        valid = isValidFor(objectId),
        calculatedDiscount = discountFor(objectId),
        eligibilityStatus = itemFor(objectId)?.eligibilityStatus.orEmpty(),
    )

sealed interface EndowIntent {
    /** Nạp ưu đãi widget (findEligible) — idempotent, chỉ chạy lần đầu. */
    data object LoadInitial : EndowIntent
    /** Validate + áp danh sách ưu đãi đã chọn (từ màn Chọn). */
    data class ValidateAndApply(val offers: List<EligibleOffer>) : EndowIntent
    /** Áp trực tiếp kết quả đã validate sẵn (host tự validate rồi đưa vào). */
    data class SetApplied(val discounts: List<EndowAppliedDiscount>, val unavailable: Boolean) : EndowIntent
    /** Đánh dấu ưu đãi đang áp không còn khả dụng (không đổi danh sách). */
    data object MarkUnavailable : EndowIntent
    /** Xoá toàn bộ ưu đãi đã áp → quay về NOT_APPLIED. */
    data object ClearApplied : EndowIntent
    data object ConsumeError : EndowIntent
}

/**
 * Kết quả bấm "Thanh toán" — xem [EndowStore.confirmRedemption].
 *
 * Một-lần, nên trả thẳng chứ không nhét vào [EndowState]: host hỏi "cho tôi đi tiếp không?" và cần
 * đúng một câu trả lời. Thay đổi hiển thị (giá đã revalidate) thì vẫn đi qua state như thường.
 */
sealed interface EndowConfirmResult {
    /** Cho phép tiến hành thanh toán. Cũng là kết quả khi đơn không áp ưu đãi nào. */
    data object Success : EndowConfirmResult
    /** Không cho đi tiếp; [errorCode] để native map sang chuỗi hiển thị. */
    data class Failure(val errorCode: String) : EndowConfirmResult
}

internal fun List<EndowAppliedDiscount>.toCreateRedemptionRequest(orderId: String, orderValue: String) =
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

internal fun List<EndowAppliedDiscount>.toValidateDiscountsRequest(orderId: String, orderValue: String) =
    ValidateDiscountsRequest(
        orderId = orderId,
        orderValue = orderValue,
        items = map { DiscountItemRequest(objectId = it.objectId, objectType = it.objectType) },
    )
