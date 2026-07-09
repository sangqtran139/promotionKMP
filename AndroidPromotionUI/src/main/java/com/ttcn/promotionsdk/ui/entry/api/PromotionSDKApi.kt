package com.ttcn.promotionsdk.ui.entry.api

import com.ttcn.promotionsdk.core.di.PromotionContainer
import com.ttcn.promotionsdk.core.domain.exception.PromotionErrorCodes
import com.ttcn.promotionsdk.core.domain.model.PromotionResult
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleFilterOptions
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOrderItem
import com.ttcn.promotionsdk.core.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.core.domain.model.redemption.RedemptionItemRequest
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.DiscountItemRequest
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersRequest
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherDetail
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherDisplayState
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherItem
import com.ttcn.promotionsdk.core.domain.model.voucher.displayState
import com.ttcn.promotionsdk.core.domain.usecase.PromotionUseCases
import kotlin.coroutines.cancellation.CancellationException

/**
 * Bề mặt API headless cho đối tác. **Không phải use case** — nó là **ranh giới chuyển đổi** giữa
 * lõi Kotlin (`promotionLogic`) và app host. Toàn bộ nghiệp vụ (gác feature flag, bắt lỗi, chuẩn hoá
 * `errorCode`) nằm trong `PromotionUseCases` của lõi, dùng chung với iOS; lớp này chỉ uỷ quyền rồi map.
 *
 * Việc nó làm, và chỉ một việc: đổi model của lõi sang DTO của `AndroidPromotionUI`
 * ([PromotionVoucher], [PromotionEligibleOffer], …), và đổi `PromotionResult` sang [PromotionApiResult].
 *
 * **Vì sao bắt buộc phải map, không trả thẳng model của lõi?**
 * Host chỉ tích hợp `AndroidPromotionUI`, không có `com.ttcn.promotionsdk.core.*` trên compile
 * classpath. Type nào của lõi xuất hiện trong chữ ký public thì host **không resolve được** — chính
 * là lý do `PromotionSDK.useCases` (trả thẳng `PromotionUseCases`) đã bị gỡ.
 *
 * Cùng vai trò với `PromotionSDKApi.swift`; bên iOS ràng buộc còn cứng hơn vì type Kotlin lọt vào
 * public API sẽ kéo `import PromotionKit` vào `.swiftinterface` và app host không build được.
 *
 * Lấy qua `PromotionSDK.api` sau khi đã `PromotionSDK.init(...)`:
 * ```kotlin
 * when (val r = PromotionSDK.api.getVouchers()) {
 *     is PromotionApiResult.Success -> render(r.data.vouchers)
 *     is PromotionApiResult.Failure -> showError(r.error)
 * }
 * ```
 */
class PromotionSDKApi internal constructor(
    private val useCases: PromotionUseCases = PromotionUseCases(),
) {

    /** Đọc lại ở mỗi lời gọi: host refresh token / đổi đơn là SDK thấy ngay. */
    private val customerId: String
        get() = PromotionContainer.requestContextProvider.getCustomerId().orEmpty()

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Lấy voucher **của khách** (Search Customer Vouchers) — chỉ voucher đã sở hữu.
     * Để lấy "Ưu đãi khác" (campaign chưa sở hữu, đủ điều kiện cho đơn) dùng [findEligible].
     *
     * Tăng [page] để load thêm. Cờ `VOUCHER_LIST` TẮT → [PromotionSDKError.FeatureDisabled].
     *
     * @param tab "all" hoặc "expiring_soon".
     */
    suspend fun getVouchers(
        keyword: String? = null,
        serviceCode: String? = null,
        tab: String? = null,
        page: Int = 0,
        size: Int = DEFAULT_PAGE_SIZE,
    ): PromotionApiResult<PromotionVoucherPage> {
        val request = SearchCustomerVouchersRequest(
            customerId = customerId,
            keyword = keyword,
            serviceCode = serviceCode,
            tab = tab ?: DEFAULT_TAB,
            page = page,
            size = size,
        )
        return handle(
            call = { useCases.searchVouchers(request) },
            onEmpty = { PromotionVoucherPage(vouchers = emptyList(), isLastPage = true) },
            map = { model ->
                PromotionVoucherPage(
                    vouchers = model.content.map(::toVoucher),
                    isLastPage = model.last ?: true,
                )
            },
        )
    }

    /**
     * Tìm ưu đãi đủ điều kiện cho đơn (Find Eligible Campaigns) — luồng "Chọn ưu đãi" khi checkout.
     * Trả 2 nhóm: "của tôi" (voucher đã sở hữu) và "khác" (campaign công khai chưa sở hữu), phân
     * trang ĐỘC LẬP — tăng [myPage] / [otherPage] để load thêm từng nhóm.
     *
     * Cờ `VOUCHER_SELECTION` TẮT → [PromotionSDKError.FeatureDisabled].
     *
     * @param items dòng đơn hàng — bắt buộc để lấy campaign theo SKU (rỗng → chỉ campaign cấp đơn).
     */
    suspend fun findEligible(
        orderId: String,
        orderValue: String,
        items: List<PromotionOrderItem> = emptyList(),
        tabCode: String? = null,
        myPage: Int = 0,
        mySize: Int = DEFAULT_PAGE_SIZE,
        otherPage: Int = 0,
        otherSize: Int = DEFAULT_PAGE_SIZE,
    ): PromotionApiResult<PromotionEligibleResult> {
        val request = FindEligibleCampaignsRequest(
            customerId = customerId,
            orderId = orderId,
            orderValue = orderValue,
            items = items.map {
                EligibleOrderItem(
                    skuId = it.skuId,
                    quantity = it.quantity,
                    unitPrice = it.unitPrice,
                    productId = it.productId,
                    productName = it.productName,
                    productCategory = it.productCategory,
                )
            },
            tabCode = tabCode,
            myPage = myPage,
            mySize = mySize,
            otherPage = otherPage,
            otherSize = otherSize,
            filterOptions = EligibleFilterOptions(),
        )
        return handle(
            call = { useCases.findEligible(request) },
            onEmpty = {
                PromotionEligibleResult(
                    myOffers = emptyList(),
                    otherOffers = emptyList(),
                    myIsLastPage = true,
                    otherIsLastPage = true,
                )
            },
            map = { model ->
                PromotionEligibleResult(
                    myOffers = model.myOffers.map(::toOffer),
                    otherOffers = model.otherOffers.map(::toOffer),
                    myIsLastPage = model.myIsLastPage,
                    otherIsLastPage = model.otherIsLastPage,
                )
            },
        )
    }

    /**
     * Lấy chi tiết 1 voucher của khách (Get Customer Voucher Detail).
     * Cờ `VOUCHER_DETAIL` TẮT → [PromotionSDKError.FeatureDisabled].
     *
     * @param serviceCode lọc thông tin theo dịch vụ đang thanh toán.
     */
    suspend fun getVoucherDetail(
        voucherId: String,
        serviceCode: String? = null,
    ): PromotionApiResult<PromotionVoucherDetail> = handle(
        call = { useCases.getVoucherDetail(voucherId, customerId, serviceCode) },
        map = ::toVoucherDetail,
    )

    /**
     * Validate một tập voucher với đơn hàng trước khi áp.
     * Cờ `VOUCHER_APPLY` TẮT → [PromotionSDKError.FeatureDisabled].
     */
    suspend fun validateDiscounts(
        orderId: String,
        orderValue: String,
        voucherIds: List<String>,
        objectType: String = DEFAULT_OBJECT_TYPE,
    ): PromotionApiResult<PromotionValidationResult> {
        val request = ValidateDiscountsRequest(
            customerId = customerId,
            orderId = orderId,
            orderValue = orderValue,
            items = voucherIds.map { DiscountItemRequest(objectId = it, objectType = objectType) },
        )
        return handle(
            call = { useCases.validateDiscounts(request) },
            map = { result ->
                PromotionValidationResult(
                    overallValid = result.overallValid,
                    totalDiscountAmount = result.totalDiscountAmount,
                    finalAmount = result.finalAmount,
                    items = result.items.map {
                        PromotionDiscountItem(
                            objectId = it.objectId,
                            discountAmount = it.calculatedDiscount,
                            isValid = it.valid,
                            eligibilityStatus = it.eligibilityStatus,
                        )
                    },
                )
            },
        )
    }

    /**
     * Tạo redemption session để xác nhận thanh toán với voucher đã chọn.
     * Cờ `VOUCHER_REDEEM` TẮT → [PromotionSDKError.FeatureDisabled].
     */
    suspend fun createRedemption(
        orderId: String,
        orderValue: String,
        voucherIds: List<String>,
        objectType: String = DEFAULT_OBJECT_TYPE,
    ): PromotionApiResult<PromotionRedemptionResult> {
        val request = CreateRedemptionRequest(
            customerId = customerId,
            orderId = orderId,
            orderValue = orderValue,
            items = voucherIds.map { RedemptionItemRequest(objectId = it, objectType = objectType) },
        )
        return handle(
            call = { useCases.createRedemption(request) },
            map = { result ->
                PromotionRedemptionResult(
                    sessionId = result.sessionId,
                    totalDiscount = result.totalDiscount,
                    finalAmount = result.finalAmount,
                    validationErrors = result.validationErrors.map {
                        PromotionRedemptionError(code = it.code, message = it.message)
                    },
                )
            },
        )
    }

    // ─── PromotionResult → PromotionApiResult ─────────────────────────────────

    /**
     * `PromotionUseCases` không ném lỗi nghiệp vụ: nó trả `PromotionResult.Failure` kèm `errorCode`.
     * Chỉ [CancellationException] mới thoát ra — nuốt nó sẽ phá structured concurrency của host.
     *
     * @param onEmpty xử lý `NO_RESULT` (server trả `data: null`). Danh sách coi là rỗng và vẫn thành
     * công; còn chi tiết / validate / redemption thì đó là [PromotionSDKError.ParseFailed]. Giữ đúng
     * hành vi của `PromotionSDKApi.swift`.
     */
    private suspend fun <T : Any, R> handle(
        call: suspend () -> PromotionResult<T>,
        onEmpty: (() -> R)? = null,
        map: (T) -> R,
    ): PromotionApiResult<R> = try {
        when (val result = call()) {
            is PromotionResult.Success -> PromotionApiResult.Success(map(result.data))
            is PromotionResult.Failure ->
                if (result.errorCode == PromotionErrorCodes.NO_RESULT && onEmpty != null) {
                    PromotionApiResult.Success(onEmpty())
                } else {
                    PromotionApiResult.Failure(toSdkError(result))
                }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        PromotionApiResult.Failure(PromotionSDKError.Unknown(e))
    }

    private fun toSdkError(failure: PromotionResult.Failure): PromotionSDKError =
        when (failure.errorCode) {
            PromotionErrorCodes.FEATURE_DISABLED -> PromotionSDKError.FeatureDisabled
            PromotionErrorCodes.TIMEOUT -> PromotionSDKError.Timeout
            PromotionErrorCodes.NO_RESULT -> PromotionSDKError.ParseFailed
            PromotionErrorCodes.NETWORK_ERROR ->
                PromotionSDKError.NetworkFailure(code = null, serverMessage = failure.message.orEmpty())
            // Mã nghiệp vụ của server (vd VOUCHER_EXPIRED) đi kèm httpStatus nếu có.
            else -> PromotionSDKError.NetworkFailure(
                code = failure.httpStatus,
                serverMessage = failure.message.orEmpty(),
            )
        }

    // ─── Lõi → DTO public ─────────────────────────────────────────────────────

    private fun toVoucher(model: VoucherItem) = PromotionVoucher(
        id = model.voucherId,
        merchantName = model.merchantName.orEmpty(),
        title = model.title.orEmpty(),
        imageUrl = model.logo,
        expireDate = model.expirationDate,
        isUsed = model.displayState() == VoucherDisplayState.USED,
        status = model.status,
        displayStatusLabel = model.displayStatusLabel,
    )

    private fun toVoucherDetail(model: VoucherDetail) = PromotionVoucherDetail(
        id = model.voucherId,
        merchantName = model.merchantName.orEmpty(),
        title = model.title.orEmpty(),
        description = model.description.orEmpty(),
        guideline = model.guideline.orEmpty(),
        startDate = model.startDate,
        expireDate = model.expirationDate,
        bannerUrl = model.banner,
        logoUrl = model.logo,
        status = model.status.orEmpty(),
        displayStatusLabel = model.displayStatusLabel,
    )

    private fun toOffer(model: EligibleOffer) = PromotionEligibleOffer(
        id = model.id,
        name = model.campaignName.orEmpty(),
        objectType = model.objectType,
        usable = model.usable,
        estimatedDiscount = model.estimatedDiscount,
        expireDate = model.expireDate,
        // Lõi không dựng sẵn câu tiếng Việt — trả rule thô cho host tự hiển thị.
        ineligibleReason = if (model.usable) null else model.unmatchedRules.firstOrNull(),
    )

    private companion object {
        const val DEFAULT_PAGE_SIZE = 10
        const val DEFAULT_TAB = "all"
        const val DEFAULT_OBJECT_TYPE = "CAMPAIGN"
    }
}
