package com.ttcn.promotionsdk.entry.api

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
 * Việc nó làm, và chỉ một việc: đổi model của lõi sang DTO của `AndroidPromotionSDK`
 * ([PRMVoucher], [PRMEligibleOffer], …), và đổi `PromotionResult` sang [PRMApiResult].
 *
 * **Vì sao bắt buộc phải map, không trả thẳng model của lõi?**
 * Host chỉ tích hợp `AndroidPromotionSDK`, không có `com.ttcn.promotionsdk.core.*` trên compile
 * classpath. Type nào của lõi xuất hiện trong chữ ký public thì host **không resolve được** — chính
 * là lý do `PRMSDK.useCases` (trả thẳng `PromotionUseCases`) đã bị gỡ.
 *
 * Cùng vai trò với `PRMSDKApi.swift`; bên iOS ràng buộc còn cứng hơn vì type Kotlin lọt vào
 * public API sẽ kéo `import PromotionKit` vào `.swiftinterface` và app host không build được.
 *
 * Lấy qua `PRMSDK.api` sau khi đã `PRMSDK.initialize(...)`:
 * ```kotlin
 * when (val r = PRMSDK.api.getVouchers()) {
 *     is PRMApiResult.Success -> render(r.data.vouchers)
 *     is PRMApiResult.Failure -> showError(r.error)
 * }
 * ```
 */
class PRMSDKApi internal constructor(
    private val useCases: PromotionUseCases = PromotionUseCases(),
) {
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
    ): PRMApiResult<PRMVoucherPage> {
        val request = SearchCustomerVouchersRequest(
            keyword = keyword,
            serviceCode = serviceCode,
            tab = tab ?: DEFAULT_TAB,
            page = page,
            size = size,
        )
        return handle(
            call = { useCases.searchVouchers(request) },
            onEmpty = { PRMVoucherPage(vouchers = emptyList(), isLastPage = true) },
            map = { model ->
                PRMVoucherPage(
                    vouchers = model.content.map(::toVoucher),
                    isLastPage = model.last ?: true,
                    expireWarningDate = model.expireWarningDate,
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
        items: List<PRMOrderItem> = emptyList(),
        tabCode: String? = null,
        myPage: Int = 0,
        mySize: Int = DEFAULT_PAGE_SIZE,
        otherPage: Int = 0,
        otherSize: Int = DEFAULT_PAGE_SIZE,
    ): PRMApiResult<PRMEligibleResult> {
        val request = FindEligibleCampaignsRequest(
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
                PRMEligibleResult(
                    myOffers = emptyList(),
                    otherOffers = emptyList(),
                    myIsLastPage = true,
                    otherIsLastPage = true,
                )
            },
            map = { model ->
                PRMEligibleResult(
                    myOffers = model.myOffers.map(::toOffer),
                    otherOffers = model.otherOffers.map(::toOffer),
                    myIsLastPage = model.myIsLastPage,
                    otherIsLastPage = model.otherIsLastPage,
                    expireWarningDate = model.expireWarningDate,
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
    ): PRMApiResult<PRMVoucherDetail> = handle(
        call = { useCases.getVoucherDetail(voucherId, serviceCode) },
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
    ): PRMApiResult<PRMValidationResult> {
        val request = ValidateDiscountsRequest(
            orderId = orderId,
            orderValue = orderValue,
            items = voucherIds.map { DiscountItemRequest(objectId = it, objectType = objectType) },
        )
        return handle(
            call = { useCases.validateDiscounts(request) },
            map = { result ->
                PRMValidationResult(
                    overallValid = result.overallValid,
                    totalDiscountAmount = result.totalDiscountAmount,
                    finalAmount = result.finalAmount,
                    items = result.items.map {
                        PRMDiscountItem(
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
    ): PRMApiResult<PRMRedemptionResult> {
        val request = CreateRedemptionRequest(
            orderId = orderId,
            orderValue = orderValue,
            items = voucherIds.map { RedemptionItemRequest(objectId = it, objectType = objectType) },
        )
        return handle(
            call = { useCases.createRedemption(request) },
            map = { result ->
                PRMRedemptionResult(
                    sessionId = result.sessionId,
                    totalDiscount = result.totalDiscount,
                    finalAmount = result.finalAmount,
                    validationErrors = result.validationErrors.map {
                        PRMRedemptionError(code = it.code, message = it.message)
                    },
                )
            },
        )
    }

    // ─── PromotionResult → PRMApiResult ─────────────────────────────────

    /**
     * `PromotionUseCases` không ném lỗi nghiệp vụ: nó trả `PromotionResult.Failure` kèm `errorCode`.
     * Chỉ [CancellationException] mới thoát ra — nuốt nó sẽ phá structured concurrency của host.
     *
     * @param onEmpty xử lý `NO_RESULT` (server trả `data: null`). Danh sách coi là rỗng và vẫn thành
     * công; còn chi tiết / validate / redemption thì đó là [PromotionSDKError.ParseFailed]. Giữ đúng
     * hành vi của `PRMSDKApi.swift`.
     */
    private suspend fun <T : Any, R> handle(
        call: suspend () -> PromotionResult<T>,
        onEmpty: (() -> R)? = null,
        map: (T) -> R,
    ): PRMApiResult<R> = try {
        when (val result = call()) {
            is PromotionResult.Success -> PRMApiResult.Success(map(result.data))
            is PromotionResult.Failure ->
                if (result.errorCode == PromotionErrorCodes.NO_RESULT && onEmpty != null) {
                    PRMApiResult.Success(onEmpty())
                } else {
                    PRMApiResult.Failure(toSdkError(result))
                }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        PRMApiResult.Failure(PromotionSDKError.Unknown(e))
    }

    private fun toSdkError(failure: PromotionResult.Failure): PromotionSDKError =
        when (failure.errorCode) {
            PromotionErrorCodes.FEATURE_DISABLED -> PromotionSDKError.FeatureDisabled
            PromotionErrorCodes.TIMEOUT -> PromotionSDKError.Timeout
            PromotionErrorCodes.NO_RESULT -> PromotionSDKError.ParseFailed
            PromotionErrorCodes.NETWORK_ERROR ->
                PromotionSDKError.NetworkFailure(code = null, message = failure.message.orEmpty())
            // Mã nghiệp vụ của server (vd VOUCHER_EXPIRED) đi kèm httpStatus nếu có.
            else -> PromotionSDKError.NetworkFailure(
                code = failure.httpStatus,
                message = failure.message.orEmpty(),
            )
        }

    // ─── Lõi → DTO public ─────────────────────────────────────────────────────

    private fun toVoucher(model: VoucherItem) = PRMVoucher(
        id = model.voucherId,
        merchantName = model.merchantName.orEmpty(),
        title = model.title.orEmpty(),
        imageURL = model.logo,
        expireDate = model.expirationDate,
        isUsed = model.displayState() == VoucherDisplayState.USED,
        status = model.status,
        displayStatusLabel = model.displayStatusLabel,
    )

    private fun toVoucherDetail(model: VoucherDetail) = PRMVoucherDetail(
        id = model.voucherId,
        merchantName = model.merchantName.orEmpty(),
        title = model.title.orEmpty(),
        description = model.description.orEmpty(),
        guideline = model.guideline.orEmpty(),
        startDate = model.startDate,
        expireDate = model.expirationDate,
        bannerURL = model.banner,
        logoURL = model.logo,
        status = model.status.orEmpty(),
        displayStatusLabel = model.displayStatusLabel,
        codes = model.codes,
        usageGuideUrl = model.usageGuideUrl,
    )

    private fun toOffer(model: EligibleOffer) = PRMEligibleOffer(
        id = model.id,
        name = model.campaignName.orEmpty(),
        objectType = model.objectType,
        usable = model.usable,
        estimatedDiscount = model.estimatedDiscount,
        expireDate = model.expireDate,
        // Lõi không dựng sẵn câu tiếng Việt — trả rule thô cho host tự hiển thị.
        ineligibleReason = if (model.usable) null else model.unmatchedRules.firstOrNull(),
        logoUrl = model.logoUrl,
        partnerName = model.partnerName,
        voucherCode = model.voucherCode,
    )

    private companion object {
        const val DEFAULT_PAGE_SIZE = 10
        const val DEFAULT_TAB = "all"
        const val DEFAULT_OBJECT_TYPE = "CAMPAIGN"
    }
}
