package com.ttcn.promotionsdk.presentation.endow

import com.ttcn.promotionsdk.core.di.PromotionContainer
import com.ttcn.promotionsdk.core.domain.exception.toErrorCode
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.core.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.DiscountItemRequest
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsResult
import com.ttcn.promotionsdk.core.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.core.domain.usecase.ValidateStackableDiscountsUseCase
import com.ttcn.promotionsdk.presentation.PromotionCancellable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * **Tầng UI-logic dùng chung** cho widget "Ưu đãi" ở màn thanh toán (`PRMEndowView`) — chạy trên cả
 * Android & iOS. Gom toàn bộ nghiệp vụ mà trước đây hai nền tảng làm khác chỗ:
 *  - Android: [PRMEndowViewModel] (load) + `ChoosePromotionViewModel.validateAndApply` (validate).
 *  - iOS: dồn hết trong `PromotionSDKImpl` (load + validate + set widget-state, không có ViewModel).
 *
 * Nay cả hai chỉ còn: quan sát [state], map [EndowAppliedDiscount] → model public riêng, render widget
 * theo [EndowWidgetState]. Cùng khuôn với các store khác: `state`/`dispatch`/`watchState`/`currentState`/`clear`.
 *
 * Order-context (orderId/orderValue) đọc từ [PromotionContainer.requestContextProvider] — nguồn duy nhất.
 */
class EndowStore(
    private val findEligibleCampaignsUseCase: FindEligibleCampaignsUseCase,
    private val validateStackableDiscountsUseCase: ValidateStackableDiscountsUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    /** iOS/Swift: khởi tạo không cần truyền scope (xem `MyPromotionStore`). */
    constructor(
        findEligibleCampaignsUseCase: FindEligibleCampaignsUseCase,
        validateStackableDiscountsUseCase: ValidateStackableDiscountsUseCase,
    ) : this(
        findEligibleCampaignsUseCase,
        validateStackableDiscountsUseCase,
        CoroutineScope(SupervisorJob() + Dispatchers.Default),
    )

    private val _state = MutableStateFlow(EndowState())
    val state: StateFlow<EndowState> = _state.asStateFlow()

    fun currentState(): EndowState = _state.value

    fun watchState(onEach: (EndowState) -> Unit): PromotionCancellable {
        val job = scope.launch { state.collect { onEach(it) } }
        return PromotionCancellable { job.cancel() }
    }

    fun clear() {
        scope.cancel()
    }

    fun dispatch(intent: EndowIntent) {
        when (intent) {
            EndowIntent.LoadInitial -> loadInitial()
            is EndowIntent.ValidateAndApply -> validateAndApply(intent.offers)
            is EndowIntent.SetApplied -> _state.update {
                it.copy(appliedDiscounts = intent.discounts, discountUnavailable = intent.unavailable, errorCode = null)
            }
            EndowIntent.MarkUnavailable -> _state.update { it.copy(discountUnavailable = true) }
            EndowIntent.ClearApplied -> _state.update {
                it.copy(appliedDiscounts = emptyList(), discountUnavailable = false, errorCode = null)
            }
            EndowIntent.ConsumeError -> _state.update { it.copy(errorCode = null) }
        }
    }

    private fun loadInitial() {
        if (_state.value.hasLoadedInitial) return
        val ctx = PromotionContainer.requestContextProvider
        scope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching {
                findEligibleCampaignsUseCase(
                    FindEligibleCampaignsRequest(
                        orderId = ctx.getOrderId().orEmpty(),
                        orderValue = ctx.getOrderValue().orEmpty(),
                        // Order items lấy từ provider (dùng chung 2 nền tảng) → campaign theo SKU.
                        items = ctx.getOrderItems(),
                        mySize = PAGE_SIZE,
                        otherSize = PAGE_SIZE,
                    )
                )
            }.onSuccess { result ->
                val myOffers = result?.myOffers.orEmpty()
                val otherOffers = result?.otherOffers.orEmpty()
                // Đếm theo totalElements (tổng thật từ server), không theo length mảng đã phân trang.
                //
                // `myTotalElements`/`otherTotalElements` là `Long` KHÔNG nullable (default 0), nên
                // `?:` không bao giờ chạy — BFF bỏ trống field là đếm ra 0, widget rơi vào EMPTY và
                // báo "không có ưu đãi" dù vừa trả về offers. Vì vậy coi 0 = "server không trả" và
                // lùi về số phần tử đã nạp. Server thật sự có 0 ưu đãi thì list cũng rỗng → vẫn 0.
                val total = totalOrSize(result?.myTotalElements, myOffers.size) +
                    totalOrSize(result?.otherTotalElements, otherOffers.size)
                _state.update {
                    it.copy(
                        isLoading = false,
                        myOffers = myOffers,
                        otherOffers = otherOffers,
                        totalVoucherCount = total.toInt(),
                        hasLoadedInitial = true,
                        errorCode = null,
                    )
                }
            }.onFailure { throwable ->
                _state.update { it.copy(isLoading = false, hasLoadedInitial = true, errorCode = throwable.toErrorCode()) }
            }
        }
    }

    /**
     * Validate danh sách ưu đãi đã chọn (từ màn "Chọn ưu đãi") với order hiện tại rồi áp:
     * bất kỳ item nào không hợp lệ → [EndowState.discountUnavailable] = true. Rỗng → xoá áp.
     * Rule diễn giải valid/discount ở domain ([ValidateDiscountsResult]) — dùng chung 2 nền tảng.
     */
    private fun validateAndApply(offers: List<EligibleOffer>) {
        if (offers.isEmpty()) {
            _state.update { it.copy(appliedDiscounts = emptyList(), discountUnavailable = false, errorCode = null) }
            return
        }
        val ctx = PromotionContainer.requestContextProvider
        scope.launch {
            _state.update { it.copy(isValidating = true) }
            val request = ValidateDiscountsRequest(
                orderId = ctx.getOrderId().orEmpty(),
                orderValue = ctx.getOrderValue().orEmpty(),
                items = offers.map { DiscountItemRequest(objectId = it.id, objectType = it.objectType) },
            )
            runCatching { validateStackableDiscountsUseCase(request) }
                .onSuccess { result ->
                    val details = result
                        ?.let { r -> offers.map { r.toEndowAppliedDiscount(it.id, it.objectType) } }
                        .orEmpty()
                    val hasInvalid = details.any { !it.valid }
                    _state.update {
                        it.copy(isValidating = false, appliedDiscounts = details, discountUnavailable = hasInvalid, errorCode = null)
                    }
                }
                .onFailure { throwable ->
                    _state.update { it.copy(isValidating = false, errorCode = throwable.toErrorCode()) }
                }
        }
    }

    private companion object {
        /** Trùng `pageSize` màn chọn (COLLAPSED không liên quan) — giữ như PRMEndowViewModel cũ. */
        private const val PAGE_SIZE = 10

        /** `total` từ server nếu có (> 0); không thì lùi về số phần tử đã nạp. */
        private fun totalOrSize(total: Long?, size: Int): Int =
            if (total != null && total > 0) total.toInt() else size
    }
}

// ─── State / Intent / Models ──────────────────────────────────────────────────

data class EndowState(
    val hasLoadedInitial: Boolean = false,
    val isLoading: Boolean = false,
    /** Ưu đãi từ `findEligible` — truyền thẳng sang màn "Chọn ưu đãi" để khỏi gọi API hai lần. */
    val myOffers: List<EligibleOffer> = emptyList(),
    val otherOffers: List<EligibleOffer> = emptyList(),
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
