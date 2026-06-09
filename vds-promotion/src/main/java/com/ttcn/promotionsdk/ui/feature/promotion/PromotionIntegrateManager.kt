package com.ttcn.promotionsdk.ui.feature.promotion

import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.data.dto.redemption.CustomerInfo
import com.ttcn.promotionsdk.core.data.dto.redemption.OrderInfo
import com.ttcn.promotionsdk.core.data.dto.redemption.RedemptionSessionRequest
import com.ttcn.promotionsdk.core.data.dto.redemption.SelectedRedeemable
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.DiscountRequest
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableCustomerInfo
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableDiscountsRequest
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableOrderInfo
import com.ttcn.promotionsdk.core.data.remote.PromotionApiException
import com.ttcn.promotionsdk.core.di.inject
import com.ttcn.promotionsdk.core.domain.repository.PromotionRepository
import com.ttcn.promotionsdk.ui.utils.view.PRMEndowView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * SDK Manager — đối tác khởi tạo 1 lần và gọi [confirmRedemption] khi user bấm thanh toán.
 *
 * Toàn bộ logic createRedemption / revalidate / update UI ẩn bên trong,
 * đối tác chỉ nhận callback [onSuccess] hoặc [onError].
 *
 * ```kotlin
 * // Khởi tạo (trong Fragment.setupUI)
 * val promotionManager = PRMPromotionManager(binding.endowView)
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
class PromotionIntegrateManager(
    private val endowView: PRMEndowView,
) {

    private val repository: PromotionRepository by inject()
    private val requestContextProvider: PromotionRequestContextProvider by inject()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

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

            val customerId = requestContextProvider.getCustomerId()
            if (customerId.isNullOrBlank()) {
                onError("missing_customer_id")
                return@launch
            }

            val request = RedemptionSessionRequest(
                idempotencyKey = UUID.randomUUID().toString(),
                customerInfo = CustomerInfo(
                    customerId = customerId,
                ),
                orderInfo = OrderInfo(
                    orderId = requestContextProvider.getOrderId().orEmpty(),
                    orderValue = requestContextProvider.getOrderValue().orEmpty(),
                ),
                selectedRedeemables = discountDetails.mapIndexed { index, detail ->
                    SelectedRedeemable(
                        objectType = detail.objectType,
                        objectId = detail.objectId,
                        priority = index + 1,
                        expectedDiscount = detail.calculatedDiscount,
                    )
                },
            )

            runCatching { repository.createRedemptionSession(request) }
                .onSuccess { response ->
                    val hasBudgetError = response?.validationErrors
                        ?.any { it.code == "INSUFFICIENT_BUDGET" }
                        ?: false

                    if (hasBudgetError) {
                        revalidateAndUpdate(customerId, onError)
                    } else {
                        onSuccess()
                    }
                }
                .onFailure { throwable ->
                    val exception = throwable as? PromotionApiException
                    if (exception?.status == 422 && exception.errorCode == "INSUFFICIENT_BUDGET") {
                        revalidateAndUpdate(customerId, onError)
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
        customerId: String,
        onError: (errorCode: String) -> Unit,
    ) {
        val currentDetails = endowView.discountDetails

        val request = StackableDiscountsRequest(
            idempotencyKey = UUID.randomUUID().toString(),
            customerInfo = StackableCustomerInfo(customerId = customerId),
            orderInfo = StackableOrderInfo(
                orderId = requestContextProvider.getOrderId().orEmpty(),
                orderValue = requestContextProvider.getOrderValue().orEmpty(),
            ),
            discountRequests = currentDetails.mapIndexed { index, detail ->
                DiscountRequest(
                    objectType = detail.objectType,
                    objectId = detail.objectId,
                    priority = index + 1,
                )
            },
        )

        runCatching { repository.validateStackableDiscounts(request) }
            .onSuccess { response ->
                // SDK tự update endowView — đối tác không cần làm gì
                endowView.setDiscountDetails(response?.discountDetails.orEmpty())
                onError("INSUFFICIENT_BUDGET")
            }
            .onFailure { throwable ->
                onError(throwable.toErrorCode())
            }
    }

    private fun Throwable.toErrorCode(): String =
        (this as? PromotionApiException)?.errorCode ?: message ?: "error_general"
}