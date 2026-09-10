package com.ttcn.promotionsdk.domain.usecase

import com.ttcn.promotionsdk.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.domain.exception.NetworkException
import com.ttcn.promotionsdk.domain.exception.PromotionException
import com.ttcn.promotionsdk.domain.model.PromotionResult
import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffersResult
import com.ttcn.promotionsdk.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.domain.model.featureflag.PromotionFeatureFlag
import com.ttcn.promotionsdk.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.domain.model.redemption.CreateRedemptionResult
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsResult
import com.ttcn.promotionsdk.domain.model.voucher.SearchCustomerVouchersRequest
import com.ttcn.promotionsdk.domain.model.voucher.SearchCustomerVouchersResult
import com.ttcn.promotionsdk.domain.model.voucher.VoucherDetail
import kotlin.coroutines.cancellation.CancellationException

/**
 * Facade headless gom năm use case, dành cho host tự dựng UI (và cho cầu nối Swift / Flutter).
 * Mọi hàm trả [PromotionResult] — **không ném exception ra ngoài**.
 *
 * Mỗi hàm được gác bởi một feature flag; cờ TẮT → [PromotionResult.Failure] mã
 * [ErrorCodes.FEATURE_DISABLED], **không gọi mạng**. Đây là kill-switch, nên nó nằm ở lõi Kotlin
 * để Android và iOS không thể lệch nhau.
 *
 * UI native không bắt buộc dùng lớp này: nó có thể dựng thẳng use case đơn lẻ
 * (`SearchCustomerVouchersUseCase()`), lúc đó phải tự bắt [PromotionException] — và tự gác cờ
 * ở điểm điều hướng bằng [PromotionFeatureGate], thứ mà lớp này cũng dùng làm nguồn sự thật.
 *
 * ```kotlin
 * when (val r = PromotionUseCases().searchVouchers(SearchCustomerVouchersRequest(...))) {
 *     is PromotionResult.Success -> render(r.data)
 *     is PromotionResult.Failure -> showError(r.errorCode)
 * }
 * ```
 */
public class PromotionUseCases internal constructor(
    private val searchVouchersUseCase: SearchCustomerVouchersUseCase,
    private val voucherDetailUseCase: GetCustomerVoucherDetailUseCase,
    private val validateDiscountsUseCase: ValidateStackableDiscountsUseCase,
    private val createRedemptionUseCase: CreateRedemptionSessionUseCase,
    private val findEligibleCampaignsUseCase: FindEligibleCampaignsUseCase,
    /** Mặc định đọc qua [PromotionFeatureGate] — cùng nguồn sự thật với điểm gác của UI. */
    private val isFeatureEnabled: (String) -> Boolean = PromotionFeatureGate::isEnabled,
) {
    /** Dựng thẳng sau khi `PromotionContainer.initialize(...)`: `PromotionUseCases()`. */
    public constructor() : this(
        SearchCustomerVouchersUseCase(),
        GetCustomerVoucherDetailUseCase(),
        ValidateStackableDiscountsUseCase(),
        CreateRedemptionSessionUseCase(),
        FindEligibleCampaignsUseCase(),
    )

    /**
     * Tìm kiếm danh sách voucher của khách hàng. Gác bởi [PromotionFeatureFlag.VOUCHER_LIST].
     */
    public suspend fun searchVouchers(
        request: SearchCustomerVouchersRequest,
    ): PromotionResult<SearchCustomerVouchersResult> =
        gated(PromotionFeatureFlag.VOUCHER_LIST) { searchVouchersUseCase(request) }

    /**
     * Lấy chi tiết một voucher. Gác bởi [PromotionFeatureFlag.VOUCHER_DETAIL].
     */
    public suspend fun getVoucherDetail(
        voucherId: String,
        service: String? = null,
    ): PromotionResult<VoucherDetail> =
        gated(PromotionFeatureFlag.VOUCHER_DETAIL) { voucherDetailUseCase(voucherId, service) }

    /**
     * Tìm ưu đãi **đủ điều kiện cho một đơn hàng** (luồng checkout), trả hai nhóm: voucher khách đã
     * sở hữu và campaign công khai chưa sở hữu, phân trang độc lập.
     * Gác bởi [PromotionFeatureFlag.VOUCHER_SELECTION].
     *
     * Khác [searchVouchers] — hàm đó chỉ trả voucher khách **đã sở hữu**, không xét đơn hàng.
     */
    public suspend fun findEligible(
        request: FindEligibleCampaignsRequest,
    ): PromotionResult<EligibleOffersResult> =
        gated(PromotionFeatureFlag.VOUCHER_SELECTION) { findEligibleCampaignsUseCase(request) }

    /**
     * Validate danh sách voucher trước khi áp dụng vào đơn hàng.
     * Gác bởi [PromotionFeatureFlag.VOUCHER_APPLY].
     */
    public suspend fun validateDiscounts(
        request: ValidateDiscountsRequest,
    ): PromotionResult<ValidateDiscountsResult> =
        gated(PromotionFeatureFlag.VOUCHER_APPLY) { validateDiscountsUseCase(request) }

    /**
     * Tạo redemption session để xác nhận thanh toán với voucher đã chọn.
     * Gác bởi [PromotionFeatureFlag.VOUCHER_REDEEM].
     */
    public suspend fun createRedemption(
        request: CreateRedemptionRequest,
    ): PromotionResult<CreateRedemptionResult> =
        gated(PromotionFeatureFlag.VOUCHER_REDEEM) { createRedemptionUseCase(request) }

    /** Cờ TẮT → trả lỗi ngay, không chạm tới [block] (và do đó không gọi mạng). */
    private suspend fun <T : Any> gated(
        flag: String,
        block: suspend () -> T?,
    ): PromotionResult<T> =
        if (isFeatureEnabled(flag)) headlessCall(block)
        else PromotionResult.Failure(ErrorCodes.FEATURE_DISABLED)

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
