package com.ttcn.promotionsdk.core.domain.usecase

import com.ttcn.promotionsdk.core.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.core.domain.exception.NetworkException
import com.ttcn.promotionsdk.core.domain.exception.PromotionException
import com.ttcn.promotionsdk.core.domain.model.PromotionResult
import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionResult
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsResult
import com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersRequest
import com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersResult
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherDetail
import kotlin.coroutines.cancellation.CancellationException

/**
 * Tập hợp tất cả use case của Promotion SDK (headless API).
 *
 * Lấy instance qua [com.ttcn.promotionsdk.ui.entry.PromotionSDK.useCases] sau khi đã gọi `init()`.
 * Mọi hàm trả [PromotionResult] — không ném exception ra ngoài.
 *
 * ```kotlin
 * when (val r = PromotionSDK.useCases.searchVouchers(SearchCustomerVouchersRequest(...))) {
 *     is PromotionResult.Success -> render(r.data)
 *     is PromotionResult.Failure -> showError(r.errorCode)
 * }
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
    ): PromotionResult<SearchCustomerVouchersResult> =
        headlessCall { searchVouchersUseCase(request) }

    /**
     * Lấy chi tiết một voucher.
     */
    suspend fun getVoucherDetail(
        voucherId: String,
        customerId: String,
        service: String? = null,
    ): PromotionResult<VoucherDetail> =
        headlessCall { voucherDetailUseCase(voucherId, customerId, service) }

    /**
     * Validate danh sách voucher trước khi áp dụng vào đơn hàng.
     */
    suspend fun validateDiscounts(
        request: ValidateDiscountsRequest,
    ): PromotionResult<ValidateDiscountsResult> =
        headlessCall { validateDiscountsUseCase(request) }

    /**
     * Tạo redemption session để xác nhận thanh toán với voucher đã chọn.
     */
    suspend fun createRedemption(
        request: CreateRedemptionRequest,
    ): PromotionResult<CreateRedemptionResult> =
        headlessCall { createRedemptionUseCase(request) }

    /**
     * Bọc một lệnh gọi use case thành [PromotionResult]: null → [ErrorCodes.NO_RESULT],
     * [PromotionException] → [PromotionResult.Failure] (giữ errorCode/status), lỗi khác → [ErrorCodes.GENERAL].
     */
    private suspend fun <T : Any> headlessCall(
        block: suspend () -> T?,
    ): PromotionResult<T> =
        try {
            block()?.let { PromotionResult.Success(it) }
                ?: PromotionResult.Failure(ErrorCodes.NO_RESULT)
        } catch (e: CancellationException) {
            throw e
        } catch (e: PromotionException) {
            PromotionResult.Failure(e.errorCode ?: ErrorCodes.GENERAL, e.message, e.httpStatus)
        } catch (e: NetworkException) {
            PromotionResult.Failure(e.errorCode, e.message)
        } catch (e: Throwable) {
            PromotionResult.Failure(ErrorCodes.GENERAL, e.message)
        }
}
