package com.ttcn.prm.ui.feature.promotion.endowview

import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.domain.usecase.ValidateStackableDiscountsUseCase
import com.ttcn.promotionsdk.presentation.endow.EndowAppliedDiscount
import com.ttcn.promotionsdk.presentation.endow.EndowIntent
import com.ttcn.promotionsdk.presentation.endow.EndowState
import com.ttcn.promotionsdk.presentation.endow.EndowStore
import com.ttcn.promotionsdk.presentation.endow.EndowWidgetState
import com.ttcn.promotionsdk.presentation.endow.widgetState
import com.ttcn.prm.entry.AppliedDiscount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Lớp bọc mỏng quanh [EndowStore] (tầng UI-logic dùng chung ở `promotionLogic`) cho custom view
 * [PRMEndowView]. **Đồng nhất với iOS**: iOS có `EndowViewModel` tương ứng bọc cùng store.
 *
 * Tạo qua [create] để DI tự resolve; [scope] gắn với vòng đời View.
 */
internal class EndowViewModel(
    private val store: EndowStore,
    private val scope: CoroutineScope,
) {

    /** Bề mặt view (Android model) — chiếu từ [EndowState] của store. */
    val uiState: StateFlow<PRMEndowUiState> =
        store.state
            .map { it.toUiState() }
            .stateIn(scope, SharingStarted.Eagerly, store.currentState().toUiState())

    /** One-shot cho validate&apply: chờ `isValidating` true→false rồi gọi completion đúng một lần. */
    private var settleCompletion: ((EndowState) -> Unit)? = null
    private var sawValidating = false

    init {
        // Theo dõi vòng validate để bắn [settleCompletion] — đối ứng `handleSettle` bên iOS.
        scope.launch { store.state.collect { handleSettle(it) } }
    }

    // ─── Public API (forward xuống store) ───────────────────────────────────────

    fun loadInitial() = store.dispatch(EndowIntent.LoadInitial)

    /**
     * Validate + áp các ưu đãi user chọn ở màn "Chọn ưu đãi" (store lo validate).
     * [completion] gọi **một** lần khi validate xong (thành công/thất bại) để màn chọn quyết định
     * đóng hay báo lỗi — đối ứng `EndowViewModel.validateAndApply(_:completion:)` bên iOS.
     */
    fun validateAndApply(offers: List<EligibleOffer>, completion: ((EndowState) -> Unit)? = null) {
        // Rỗng → store xoá áp NGAY, không có vòng validate nào để chờ; bắn completion luôn cho khỏi treo.
        if (offers.isEmpty()) {
            store.dispatch(EndowIntent.ValidateAndApply(offers))
            completion?.invoke(store.currentState())
            return
        }
        settleCompletion = completion
        sawValidating = false
        store.dispatch(EndowIntent.ValidateAndApply(offers))
    }

    /** Host tự validate rồi đưa kết quả vào (giữ public API `PRMEndowView.setDiscountDetails`). */
    fun setApplied(details: List<AppliedDiscount>, unavailable: Boolean = false) =
        store.dispatch(EndowIntent.SetApplied(details.map { it.toEndowAppliedDiscount() }, unavailable))

    fun markUnavailable() = store.dispatch(EndowIntent.MarkUnavailable)

    fun clearApplied() = store.dispatch(EndowIntent.ClearApplied)

    fun consumeError() = store.dispatch(EndowIntent.ConsumeError)

    /**
     * Xoá lỗi — **trừ khi** đang chờ kết quả một vòng validate.
     *
     * `errorCode` là field dùng chung, có hai nơi quan sát: [handleSettle] (bắn completion cho màn
     * "Chọn ưu đãi") và `PRMEndowView.renderState` (hiện lỗi rồi xoá). `store.state` là `StateFlow`
     * nên **conflated**: widget xoá trước là [handleSettle] có thể nhảy qua luôn state mang lỗi và
     * chỉ thấy bản đã xoá → completion nhận `errorCode = null` → màn chọn đóng như thành công dù
     * validate hỏng. Giữ lỗi lại cho tới khi completion đọc xong; [handleSettle] tự xoá sau đó.
     */
    fun consumeErrorUnlessSettling() {
        if (settleCompletion != null) return
        store.dispatch(EndowIntent.ConsumeError)
    }

    private fun handleSettle(state: EndowState) {
        if (state.isValidating) {
            sawValidating = true
            return
        }
        val completion = settleCompletion ?: return
        if (!sawValidating) return
        settleCompletion = null
        sawValidating = false
        completion(state)
        // Đã giao lỗi cho nơi gọi → giờ mới xoá (widget bị chặn xoá trong lúc chờ).
        if (state.errorCode != null) store.dispatch(EndowIntent.ConsumeError)
    }

    companion object {
        /** Tạo instance; use case tự lấy repository từ đồ thị đã init. [scope] gắn với [PRMEndowView]. */
        fun create(scope: CoroutineScope): EndowViewModel =
            EndowViewModel(
                store = EndowStore(
                    findEligibleCampaignsUseCase = FindEligibleCampaignsUseCase(),
                    validateStackableDiscountsUseCase = ValidateStackableDiscountsUseCase(),
                    scope = scope,
                ),
                scope = scope,
            )
    }
}

// ─── Map EndowState (store) → model UI Android ────────────────────────────────

private fun EndowState.toUiState() = PRMEndowUiState(
    myVouchers = myOffers,
    otherVouchers = otherOffers,
    myIsLastPage = myIsLastPage,
    otherIsLastPage = otherIsLastPage,
    discountDetails = appliedDiscounts.map { it.toAppliedDiscount() },
    discountUnavailable = discountUnavailable,
    totalVoucherCount = totalVoucherCount,
    hasLoadedInitial = hasLoadedInitial,
    error = errorCode,
    widgetState = widgetState.toEndowViewState(),
)

private fun EndowWidgetState.toEndowViewState(): EndowViewState = when (this) {
    EndowWidgetState.EMPTY -> EndowViewState.EMPTY
    EndowWidgetState.NOT_APPLIED -> EndowViewState.NOT_APPLIED
    EndowWidgetState.APPLIED -> EndowViewState.APPLIED
    EndowWidgetState.UNAVAILABLE -> EndowViewState.UNAVAILABLE
}

private fun EndowAppliedDiscount.toAppliedDiscount() = AppliedDiscount(
    objectId = objectId,
    objectType = objectType,
    valid = valid,
    calculatedDiscount = calculatedDiscount,
    eligibilityStatus = eligibilityStatus,
)

private fun AppliedDiscount.toEndowAppliedDiscount() = EndowAppliedDiscount(
    objectId = objectId,
    objectType = objectType,
    valid = valid,
    calculatedDiscount = calculatedDiscount,
    eligibilityStatus = eligibilityStatus,
)
