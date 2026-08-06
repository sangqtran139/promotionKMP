package com.ttcn.prm.ui.feature.promotion

import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.di.PromotionContainer
import com.ttcn.promotionsdk.core.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.core.domain.exception.PromotionException
import com.ttcn.promotionsdk.core.domain.exception.toErrorCode
import com.ttcn.promotionsdk.core.domain.model.featureflag.PromotionFeatureFlag
import com.ttcn.promotionsdk.core.domain.usecase.CreateRedemptionSessionUseCase
import com.ttcn.promotionsdk.core.domain.usecase.PromotionFeatureGate
import com.ttcn.promotionsdk.core.domain.usecase.ValidateStackableDiscountsUseCase
import com.ttcn.prm.ui.feature.promotion.endowview.PRMEndowView
import com.ttcn.prm.ui.feature.promotion.ext.toCreateRedemptionRequest
import com.ttcn.prm.ui.feature.promotion.ext.appliedDiscountFor
import com.ttcn.prm.ui.feature.promotion.ext.toValidateDiscountsRequest
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
 * Dùng [PromotionIntegrateManager.create] để khởi tạo — DI được resolve tự động bên trong,
 * Fragment không cần biết gì về dependency.
 *
 * ```kotlin
 * // Khởi tạo (trong Fragment.setupUI)
 * val promotionManager = PromotionIntegrateManager.create(binding.endowView)
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
class PromotionIntegrateManager internal constructor(
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
     * 3. Cờ [PromotionFeatureFlag.VOUCHER_REDEEM] TẮT → [onError] `PRM_MOB_021`, **không gọi mạng**
     * 4. Có voucher → gọi createRedemptionSession
     *    - Success (không có lỗi INSUFFICIENT_BUDGET) → [onSuccess]
     *    - INSUFFICIENT_BUDGET (body hoặc HTTP 422) → revalidate → tự update [endowView] → [onError]
     *    - Lỗi khác → [onError]
     *
     * Bước 3 nằm **sau** bước 2: đơn không có voucher nào thì [onSuccess] chạy bất kể cờ.
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

        if (!PromotionFeatureGate.canRedeemVoucher()) {
            onError(ErrorCodes.FEATURE_DISABLED)
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
     *
     * Gác bởi [PromotionFeatureFlag.VOUCHER_APPLY] — đây là lời gọi mạng thứ hai, mang cờ riêng, nên
     * phải hỏi riêng (chỉ rơi vào đây khi `VOUCHER_REDEEM` bật mà `VOUCHER_APPLY` tắt). Cờ tắt thì
     * không lấy được discountDetails mới, nên **không** đụng vào [endowView]: để giá hiển thị cũ còn
     * hơn ghi đè bằng dữ liệu không có.
     */
    private suspend fun revalidateAndUpdate(
        onError: (errorCode: String) -> Unit,
    ) {
        if (!PromotionFeatureGate.canApplyVoucher()) {
            onError(ErrorCodes.FEATURE_DISABLED)
            return
        }

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
        fun create(endowView: PRMEndowView): PromotionIntegrateManager =
            PromotionIntegrateManager(
                endowView = endowView,
                createRedemptionSessionUseCase = CreateRedemptionSessionUseCase(),
                validateStackableDiscountsUseCase = ValidateStackableDiscountsUseCase(),
                requestContextProvider = PromotionContainer.requestContextProvider,
            )
    }
}