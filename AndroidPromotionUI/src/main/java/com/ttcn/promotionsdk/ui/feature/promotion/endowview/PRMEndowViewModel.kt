package com.ttcn.promotionsdk.ui.feature.promotion.endowview

import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.core.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.core.domain.usecase.ValidateStackableDiscountsUseCase
import com.ttcn.promotionsdk.presentation.endow.EndowAppliedDiscount
import com.ttcn.promotionsdk.presentation.endow.EndowIntent
import com.ttcn.promotionsdk.presentation.endow.EndowState
import com.ttcn.promotionsdk.presentation.endow.EndowStore
import com.ttcn.promotionsdk.presentation.endow.EndowWidgetState
import com.ttcn.promotionsdk.presentation.endow.widgetState
import com.ttcn.promotionsdk.ui.entry.AppliedDiscount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Lớp bọc mỏng quanh [EndowStore] (tầng UI-logic dùng chung ở `promotionLogic`) cho custom view
 * [PRMEndowView]. **Đồng nhất với iOS**: iOS có `EndowViewModel` tương ứng bọc cùng store, thay cho
 * việc trước đây dồn logic trong `PromotionSDKImpl`.
 *
 * Tạo qua [create] để DI tự resolve; [scope] gắn với vòng đời View.
 */
internal class PRMEndowViewModel(
    private val store: EndowStore,
    scope: CoroutineScope,
) {

    /** Bề mặt view (Android model) — chiếu từ [EndowState] của store. */
    val uiState: StateFlow<PRMEndowUiState> =
        store.state
            .map { it.toUiState() }
            .stateIn(scope, SharingStarted.Eagerly, store.currentState().toUiState())

    // ─── Public API (forward xuống store) ───────────────────────────────────────

    fun loadInitialVouchers() = store.dispatch(EndowIntent.LoadInitial)

    /** Validate + áp các ưu đãi user chọn ở màn "Chọn ưu đãi" (store lo validate). */
    fun validateAndApply(offers: List<EligibleOffer>) =
        store.dispatch(EndowIntent.ValidateAndApply(offers))

    /** Host tự validate rồi đưa kết quả vào (giữ public API `PRMEndowView.setDiscountDetails`). */
    fun applyDiscountDetails(details: List<AppliedDiscount>, unavailable: Boolean = false) =
        store.dispatch(EndowIntent.SetApplied(details.map { it.toEndowAppliedDiscount() }, unavailable))

    fun markDiscountUnavailable() = store.dispatch(EndowIntent.MarkUnavailable)

    fun clearDiscountDetails() = store.dispatch(EndowIntent.ClearApplied)

    fun clearError() = store.dispatch(EndowIntent.ConsumeError)

    companion object {
        /** Tạo instance; use case tự lấy repository từ đồ thị đã init. [scope] gắn với [PRMEndowView]. */
        fun create(scope: CoroutineScope): PRMEndowViewModel =
            PRMEndowViewModel(
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
