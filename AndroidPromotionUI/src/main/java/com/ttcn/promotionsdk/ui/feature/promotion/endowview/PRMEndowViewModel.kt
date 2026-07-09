package com.ttcn.promotionsdk.ui.feature.promotion.endowview

import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.ui.entry.AppliedDiscount
import com.ttcn.promotionsdk.core.di.PromotionContainer
import com.ttcn.promotionsdk.core.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.core.domain.exception.toErrorCode
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.core.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.core.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.core.domain.usecase.ValidateStackableDiscountsUseCase
import com.ttcn.promotionsdk.ui.feature.promotion.ext.toAppliedDiscounts
import com.ttcn.promotionsdk.ui.feature.promotion.ext.toValidateDiscountsRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Toàn bộ business logic của [PRMEndowView] — tách ra để unit test độc lập với Android View.
 *
 * Tạo instance qua [PRMEndowViewModel.create] để DI tự resolve,
 * hoặc inject thẳng constructor khi viết test.
 *
 * ```kotlin
 * // Production (trong PRMEndowView)
 * private val viewModel = PRMEndowViewModel.create()
 *
 * // Test
 * val viewModel = PRMEndowViewModel(
 *     findEligibleCampaignsUseCase = fakeFindEligibleUseCase,
 *     validateStackableDiscountsUseCase = fakeValidateUseCase,
 *     requestContextProvider = fakeContextProvider,
 *     scope = TestScope(),
 * )
 * ```
 *
 * Dùng `findEligible` (ưu đãi đủ điều kiện cho đơn hàng) chứ không phải `searchVouchers`
 * (voucher khách đã sở hữu) — xem ghi chú ở [ChoosePromotionViewModel].
 */
internal class PRMEndowViewModel(
    private val findEligibleCampaignsUseCase: FindEligibleCampaignsUseCase,
    private val validateStackableDiscountsUseCase: ValidateStackableDiscountsUseCase,
    private val requestContextProvider: PromotionRequestContextProvider,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {

    // ─── State ────────────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow(PRMEndowUiState())
    val uiState: StateFlow<PRMEndowUiState> = _uiState.asStateFlow()

    // ─── Public API ───────────────────────────────────────────────────────────

    fun loadInitialVouchers() {
        if (_uiState.value.hasLoadedInitial) return
        scope.launch {
            val customerId = requestContextProvider.getCustomerId()
            if (customerId.isNullOrBlank()) {
                _uiState.update { it.copy(error = ErrorCodes.MISSING_CUSTOMER_ID) }
                return@launch
            }

            runCatching {
                findEligibleCampaignsUseCase(
                    FindEligibleCampaignsRequest(
                        customerId = customerId,
                        orderId = requestContextProvider.getOrderId().orEmpty(),
                        orderValue = requestContextProvider.getOrderValue().orEmpty(),
                        // TODO(order-items): xem ghi chú cùng tên ở ChoosePromotionViewModel.
                        items = emptyList(),
                        mySize = PAGE_SIZE,
                        otherSize = PAGE_SIZE,
                    )
                )
            }.onSuccess { result ->
                val myOffers = result?.myOffers.orEmpty()
                val otherOffers = result?.otherOffers.orEmpty()
                val total = (result?.myTotalElements ?: myOffers.size.toLong()) +
                    (result?.otherTotalElements ?: otherOffers.size.toLong())

                _uiState.update {
                    it.copy(
                        myVouchers = myOffers,
                        otherVouchers = otherOffers,
                        totalVoucherCount = total.toInt(),
                        hasLoadedInitial = true,
                        error = null,
                    )
                }

                // TODO(auto-apply): `findEligible` không trả `isAutoApplied` — xem ghi chú ở
                // `EligibleOfferMapper`. Voucher tự-áp-dụng vì thế không chạy ở luồng checkout,
                // trên cả Android lẫn iOS (`PromotionSDKImpl.autoApply`). Khi backend bổ sung
                // field, gọi lại [validateAndAutoApply] với offer đầu tiên có cờ bật.
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(hasLoadedInitial = true, error = throwable.toErrorCode())
                }
            }
        }
    }

    fun applyDiscountDetails(details: List<AppliedDiscount>, unavailable: Boolean = false) {
        _uiState.update {
            it.copy(
                discountDetails = details,
                discountUnavailable = unavailable,
                error = null
            )
        }
    }

    /** Đánh dấu ưu đãi hiện tại không khả dụng mà không thay đổi danh sách. */
    fun markDiscountUnavailable() {
        _uiState.update { it.copy(discountUnavailable = true) }
    }

    /** Xoá toàn bộ ưu đãi đã chọn → quay về NOT_APPLIED. */
    fun clearDiscountDetails() {
        _uiState.update {
            it.copy(
                discountDetails = emptyList(),
                discountUnavailable = false,
                error = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ─── Internal ─────────────────────────────────────────────────────────────

    @Suppress("unused") // Bật lại khi `findEligible` trả `isAutoApplied` — xem TODO(auto-apply).
    private fun validateAndAutoApply(
        customerId: String,
        offers: List<EligibleOffer>,
    ) {
        scope.launch {
            val request = offers.toValidateDiscountsRequest(
                customerId = customerId,
                orderId = requestContextProvider.getOrderId().orEmpty(),
                orderValue = requestContextProvider.getOrderValue().orEmpty(),
            )

            runCatching { validateStackableDiscountsUseCase(request) }
                .onSuccess { response ->
                    val details = response?.items.orEmpty().toAppliedDiscounts()
                    val hasInvalid = details.isNotEmpty() && details.any { !it.valid }
                    _uiState.update {
                        it.copy(
                            discountDetails = details,
                            discountUnavailable = hasInvalid,
                            error = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(error = throwable.toErrorCode()) }
                }
        }
    }

    // ─── Factory ──────────────────────────────────────────────────────────────

    companion object {
        /** Trùng `pageSize` của `ChoosePromotionViewModel` bên iOS. */
        private const val PAGE_SIZE = 10

        /**
         * Tạo instance; use case tự lấy repository từ đồ thị đã init.
         * [scope] được truyền từ [PRMEndowView] (gắn với View lifecycle).
         */
        fun create(scope: CoroutineScope): PRMEndowViewModel =
            PRMEndowViewModel(
                findEligibleCampaignsUseCase = FindEligibleCampaignsUseCase(),
                validateStackableDiscountsUseCase = ValidateStackableDiscountsUseCase(),
                requestContextProvider = PromotionContainer.requestContextProvider,
                scope = scope,
            )
    }
}