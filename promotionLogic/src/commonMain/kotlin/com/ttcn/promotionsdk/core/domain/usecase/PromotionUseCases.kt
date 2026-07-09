package com.ttcn.promotionsdk.core.domain.usecase

import com.ttcn.promotionsdk.core.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.core.domain.exception.NetworkException
import com.ttcn.promotionsdk.core.domain.exception.PromotionException
import com.ttcn.promotionsdk.core.domain.model.PromotionResult
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffersResult
import com.ttcn.promotionsdk.core.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionResult
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsResult
import com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersRequest
import com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersResult
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherDetail
import kotlin.coroutines.cancellation.CancellationException

/**
 * Facade headless gom năm use case, dành cho host tự dựng UI (và cho cầu nối Swift / Flutter).
 * Mọi hàm trả [PromotionResult] — **không ném exception ra ngoài**.
 *
 * UI native không bắt buộc dùng lớp này: nó có thể dựng thẳng use case đơn lẻ
 * (`SearchCustomerVouchersUseCase()`), lúc đó phải tự bắt [PromotionException].
 *
 * ```kotlin
 * when (val r = PromotionUseCases().searchVouchers(SearchCustomerVouchersRequest(...))) {
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
    private val findEligibleCampaignsUseCase: FindEligibleCampaignsUseCase,
) {
    /** Dựng thẳng sau khi `PromotionContainer.initialize(...)`: `PromotionUseCases()`. */
    constructor() : this(
        SearchCustomerVouchersUseCase(),
        GetCustomerVoucherDetailUseCase(),
        ValidateStackableDiscountsUseCase(),
        CreateRedemptionSessionUseCase(),
        FindEligibleCampaignsUseCase(),
    )

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
     * Tìm ưu đãi **đủ điều kiện cho một đơn hàng** (luồng checkout), trả hai nhóm: voucher khách đã
     * sở hữu và campaign công khai chưa sở hữu, phân trang độc lập.
     *
     * Khác [searchVouchers] — hàm đó chỉ trả voucher khách **đã sở hữu**, không xét đơn hàng.
     */
    suspend fun findEligible(
        request: FindEligibleCampaignsRequest,
    ): PromotionResult<EligibleOffersResult> =
        headlessCall { findEligibleCampaignsUseCase(request) }

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
