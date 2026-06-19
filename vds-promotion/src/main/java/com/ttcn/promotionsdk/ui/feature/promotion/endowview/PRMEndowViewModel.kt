package com.ttcn.promotionsdk.ui.feature.promotion.endowview

import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.ui.entry.AppliedDiscount
import com.ttcn.promotionsdk.core.di.internal.get
import com.ttcn.promotionsdk.core.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.core.domain.exception.toErrorCode
import com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersRequest
import com.ttcn.promotionsdk.core.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.core.domain.usecase.ValidateStackableDiscountsUseCase
import com.ttcn.promotionsdk.ui.feature.promotion.ext.toAppliedDiscounts
import com.ttcn.promotionsdk.ui.feature.promotion.ext.toValidateDiscountsRequest
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyVoucherListItem
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.toMyVoucherListItem
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
 *     searchCustomerVouchersUseCase = fakeSearchUseCase,
 *     validateStackableDiscountsUseCase = fakeValidateUseCase,
 *     requestContextProvider = fakeContextProvider,
 *     scope = TestScope(),
 * )
 * ```
 */
internal class PRMEndowViewModel(
    private val searchCustomerVouchersUseCase: SearchCustomerVouchersUseCase,
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
                searchCustomerVouchersUseCase(
                    SearchCustomerVouchersRequest(
                        customerId = customerId,
                        keyword = null,
                        serviceCode = requestContextProvider.getService() ?: "vay",
                        sectionCode = null,
                        tab = null,
                        myVouchersPage = 0,
                        myVouchersSize = 10,
                        otherVouchersPage = 0,
                        otherVouchersSize = 10,
                    )
                )
            }.onSuccess { response ->
                val myVouchers =
                    response?.myVouchers?.content.orEmpty().map { it.toMyVoucherListItem() }
                val otherVouchers =
                    response?.otherVouchers?.content.orEmpty().map { it.toMyVoucherListItem() }
                val total =
                    (response?.myVouchers?.totalElements ?: myVouchers.size.toLong()).toInt() +
                            (response?.otherVouchers?.totalElements
                                ?: otherVouchers.size.toLong()).toInt()

                _uiState.update {
                    it.copy(
                        myVouchers = myVouchers,
                        otherVouchers = otherVouchers,
                        totalVoucherCount = total,
                        hasLoadedInitial = true,
                        error = null,
                    )
                }

                // Auto-apply nếu discountDetails chưa có và có voucher isAutoApplied
                if (_uiState.value.discountDetails.isEmpty()) {
                    val autoApplied = (myVouchers + otherVouchers).firstOrNull { it.isAutoApplied }
                    if (autoApplied != null) {
                        validateAndAutoApply(customerId, listOf(autoApplied))
                    }
                }
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

    private fun validateAndAutoApply(
        customerId: String,
        vouchers: List<MyVoucherListItem>,
    ) {
        scope.launch {
            val request = vouchers.toValidateDiscountsRequest(
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
        /**
         * Tạo instance với dep resolve từ DI.
         * [scope] được truyền từ [PRMEndowView] (gắn với View lifecycle).
         */
        fun create(scope: CoroutineScope): PRMEndowViewModel =
            PRMEndowViewModel(
                searchCustomerVouchersUseCase = get(),
                validateStackableDiscountsUseCase = get(),
                requestContextProvider = get(),
                scope = scope,
            )
    }
}