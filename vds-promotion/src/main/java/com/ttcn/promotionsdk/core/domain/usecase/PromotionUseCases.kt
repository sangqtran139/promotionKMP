package com.ttcn.promotionsdk.core.domain.usecase

import com.ttcn.promotionsdk.core.data.dto.redemption.CustomerInfo
import com.ttcn.promotionsdk.core.data.dto.redemption.OrderInfo
import com.ttcn.promotionsdk.core.data.dto.redemption.RedemptionSessionRequest
import com.ttcn.promotionsdk.core.data.dto.redemption.SelectedRedeemable
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.DiscountRequest
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableCustomerInfo
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableDiscountsRequest
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableOrderInfo
import com.ttcn.promotionsdk.core.domain.model.CreateRedemptionRequest
import com.ttcn.promotionsdk.core.domain.model.DiscountItemResult
import com.ttcn.promotionsdk.core.domain.model.DiscountValidationResult
import com.ttcn.promotionsdk.core.domain.model.RedemptionSessionResult
import com.ttcn.promotionsdk.core.domain.model.RedemptionValidationError
import com.ttcn.promotionsdk.core.domain.model.SearchCustomerVouchersRequest
import com.ttcn.promotionsdk.core.domain.model.ValidateDiscountsRequest
import com.ttcn.promotionsdk.core.domain.model.VoucherDetail
import com.ttcn.promotionsdk.core.domain.model.VoucherSearchResult
import java.util.UUID

/**
 * Tập hợp tất cả use case của Promotion SDK.
 *
 * Lấy instance qua [com.ttcn.promotionsdk.ui.entry.PromotionSDK.useCases] sau khi đã gọi `init()`.
 *
 * ```kotlin
 * val useCases = PromotionSDK.useCases
 * val result = useCases.searchVouchers(SearchCustomerVouchersRequest(...))
 * ```
 */
class PromotionUseCases internal constructor(
    private val searchVouchersUseCase: SearchCustomerVouchersUseCase,
    private val voucherDetailUseCase: GetCustomerVoucherDetailUseCase,
    private val validateDiscountsUseCase: ValidateStackableDiscountsUseCase,
    private val createRedemptionUseCase: CreateRedemptionSessionUseCase,
) {

    /**
     * Tìm kiếm danh sách voucher của khách hàng.
     */
    suspend fun searchVouchers(
        request: SearchCustomerVouchersRequest,
    ): VoucherSearchResult? = searchVouchersUseCase(request)

    /**
     * Lấy chi tiết một voucher.
     */
    suspend fun getVoucherDetail(
        voucherId: String,
        customerId: String,
        service: String? = null,
    ): VoucherDetail? = voucherDetailUseCase(voucherId, customerId, service)

    /**
     * Validate danh sách voucher trước khi áp dụng vào đơn hàng.
     */
    suspend fun validateDiscounts(
        request: ValidateDiscountsRequest,
    ): DiscountValidationResult? {
        val response = validateDiscountsUseCase(request.toDto())
        return response?.let {
            DiscountValidationResult(
                overallValid = it.validationResult.overallValid,
                totalDiscountAmount = it.validationResult.totalDiscountAmount,
                finalAmount = it.validationResult.finalAmount,
                items = it.discountDetails.map { detail ->
                    DiscountItemResult(
                        objectId = detail.objectId,
                        objectType = detail.objectType,
                        valid = detail.valid,
                        calculatedDiscount = detail.calculatedDiscount,
                        eligibilityStatus = detail.eligibilityStatus,
                    )
                },
            )
        }
    }

    /**
     * Tạo redemption session để xác nhận thanh toán với voucher đã chọn.
     */
    suspend fun createRedemption(
        request: CreateRedemptionRequest,
    ): RedemptionSessionResult? {
        val response = createRedemptionUseCase(request.toDto())
        return response?.let {
            RedemptionSessionResult(
                sessionId = it.sessionId,
                totalDiscount = it.preview?.totalDiscount.orEmpty(),
                finalAmount = it.preview?.finalAmount.orEmpty(),
                validationErrors = it.validationErrors.map { error ->
                    RedemptionValidationError(
                        code = error.code,
                        message = error.message,
                    )
                },
            )
        }
    }

    // ─── Internal mapping ─────────────────────────────────────────────────────

    private fun ValidateDiscountsRequest.toDto() = StackableDiscountsRequest(
        idempotencyKey = UUID.randomUUID().toString(),
        customerInfo = StackableCustomerInfo(customerId = customerId),
        orderInfo = StackableOrderInfo(orderId = orderId, orderValue = orderValue),
        discountRequests = items.mapIndexed { index, item ->
            DiscountRequest(
                objectType = item.objectType,
                objectId = item.objectId,
                priority = index + 1,
            )
        },
    )

    private fun CreateRedemptionRequest.toDto() = RedemptionSessionRequest(
        idempotencyKey = UUID.randomUUID().toString(),
        customerInfo = CustomerInfo(customerId = customerId),
        orderInfo = OrderInfo(orderId = orderId, orderValue = orderValue),
        selectedRedeemables = items.mapIndexed { index, item ->
            SelectedRedeemable(
                objectType = item.objectType,
                objectId = item.objectId,
                priority = index + 1,
                expectedDiscount = item.expectedDiscount ?: "",
            )
        },
    )
}
