package com.ttcn.promotionsdk.promotionsdkui.feature.promotion

import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.di.PromotionContainer
import com.ttcn.promotionsdk.core.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.core.domain.exception.PromotionException
import com.ttcn.promotionsdk.core.domain.exception.toErrorCode
import com.ttcn.promotionsdk.core.domain.usecase.CreateRedemptionSessionUseCase
import com.ttcn.promotionsdk.core.domain.usecase.ValidateStackableDiscountsUseCase
import com.ttcn.promotionsdk.promotionsdkui.feature.promotion.endowview.PRMEndowView
import com.ttcn.promotionsdk.promotionsdkui.feature.promotion.ext.toCreateRedemptionRequest
import com.ttcn.promotionsdk.promotionsdkui.feature.promotion.ext.appliedDiscountFor
import com.ttcn.promotionsdk.promotionsdkui.feature.promotion.ext.toValidateDiscountsRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * SDK Manager — đối tác khởi tạo 1 lần và gọi [confirmRedemption] khi user bấm thanh toán.
 *
 * Toàn bộ logic createRedemption / revalidate / update UI ẩn bên trong,
 * đối tác chỉ nhận callback [onSuccess] hoặc [onError].
 *
 * Dùng [PRMIntegrateManager.create] để khởi tạo — DI được resolve tự động bên trong,
 * Fragment không cần biết gì về dependency.
 *
 * ```kotlin
 * // Khởi tạo (trong Fragment.setupUI)
 * val promotionManager = PRMIntegrateManager.create(binding.endowView)
 *
 * // Gọi khi bấm thanh toán
 * btnConfirmPayment.setOnClickListener {
 *     promotionManager.confirmRedemption(
 *         onSuccess = { proceedPayment() },
 *         onError   = { errorCode -> showError(errorCode) },
 *     )
 * }
 *
 * // Giải phóng khi Fragment destroy
 * override fun onDestroyView() {
 *     super.onDestroyView()
 *     promotionManager.clear()
 * }
 * ```
 */
class PRMIntegrateManager internal constructor(
    private val endowView: PRMEndowView,
    private val createRedemptionSessionUseCase: CreateRedemptionSessionUseCase,
    private val validateStackableDiscountsUseCase: ValidateStackableDiscountsUseCase,
    private val requestContextProvider: PromotionRequestContextProvider,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Gọi khi user bấm nút confirm thanh toán.
     *
     * Flow nội bộ:
     * 1. Lấy [discountDetails] hiện tại từ [endowView]
     * 2. Không có voucher → [onSuccess] ngay
     * 3. Có voucher → gọi createRedemptionSession
     *    - Success (không có lỗi INSUFFICIENT_BUDGET) → [onSuccess]
     *    - INSUFFICIENT_BUDGET (body hoặc HTTP 422) → revalidate → tự update [endowView] → [onError]
     *    - Lỗi khác → [onError]
     *
     * @param onSuccess   Thanh toán được phép tiến hành — đối tác gọi logic payment của mình
     * @param onError     Có lỗi — đối tác hiển thị thông báo với [errorCode]
     */
    fun confirmRedemption(
        onSuccess: () -> Unit,
        onError: (errorCode: String) -> Unit,
    ) {
        val discountDetails = endowView.discountDetails

        if (discountDetails.isEmpty()) {
            onSuccess()
            return
        }

        scope.launch {

            val request = discountDetails.toCreateRedemptionRequest(
                orderId = requestContextProvider.getOrderId().orEmpty(),
                orderValue = requestContextProvider.getOrderValue().orEmpty(),
            )

            runCatching { createRedemptionSessionUseCase(request) }
                .onSuccess { response ->
                    val hasBudgetError = response?.validationErrors
                        ?.any { it.code == ErrorCodes.INSUFFICIENT_BUDGET }
                        ?: false

                    if (hasBudgetError) {
                        revalidateAndUpdate(onError)
                    } else {
                        onSuccess()
                    }
                }
                .onFailure { throwable ->
                    val exception = throwable as? PromotionException
                    if (exception?.httpStatus == 422 && exception.errorCode == ErrorCodes.INSUFFICIENT_BUDGET) {
                        revalidateAndUpdate(onError)
                    } else {
                        onError(throwable.toErrorCode())
                    }
                }
        }
    }

    /**
     * Giải phóng coroutine scope — gọi trong [Fragment.onDestroyView].
     */
    fun clear() {
        scope.cancel()
    }

    // ─── Internal ─────────────────────────────────────────────────────────────

    /**
     * Gọi lại validateStackableDiscounts sau INSUFFICIENT_BUDGET.
     * Tự update [endowView] với discountDetails mới rồi báo lỗi về đối tác.
     */
    private suspend fun revalidateAndUpdate(
        onError: (errorCode: String) -> Unit,
    ) {
        val currentDetails = endowView.discountDetails

        val request = currentDetails.toValidateDiscountsRequest(
            orderId = requestContextProvider.getOrderId().orEmpty(),
            orderValue = requestContextProvider.getOrderValue().orEmpty(),
        )

        runCatching { validateStackableDiscountsUseCase(request) }
            .onSuccess { response ->
                // Lặp theo detail đang áp + diễn giải qua isValidFor/discountFor (đối xứng iOS).
                val newDetails = response
                    ?.let { r -> currentDetails.map { r.appliedDiscountFor(it.objectId, it.objectType) } }
                    .orEmpty()
                endowView.setDiscountDetails(newDetails)
                onError(ErrorCodes.INSUFFICIENT_BUDGET)
            }
            .onFailure { throwable ->
                onError(throwable.toErrorCode())
            }
    }

    companion object {
        fun create(endowView: PRMEndowView): PRMIntegrateManager =
            PRMIntegrateManager(
                endowView = endowView,
                createRedemptionSessionUseCase = CreateRedemptionSessionUseCase(),
                validateStackableDiscountsUseCase = ValidateStackableDiscountsUseCase(),
                requestContextProvider = PromotionContainer.requestContextProvider,
            )
    }
}