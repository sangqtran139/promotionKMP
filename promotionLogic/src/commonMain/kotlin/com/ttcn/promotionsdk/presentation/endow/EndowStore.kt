package com.ttcn.promotionsdk.presentation.endow

import com.ttcn.promotionsdk.di.PromotionContainer
import com.ttcn.promotionsdk.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.domain.exception.PromotionException
import com.ttcn.promotionsdk.domain.exception.toErrorCode
import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.domain.model.stackablediscount.DiscountItemRequest
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsResult
import com.ttcn.promotionsdk.domain.usecase.CreateRedemptionSessionUseCase
import com.ttcn.promotionsdk.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.domain.usecase.PromotionFeatureGate
import com.ttcn.promotionsdk.domain.usecase.ValidateStackableDiscountsUseCase
import com.ttcn.promotionsdk.presentation.base.PRMStore
import com.ttcn.promotionsdk.presentation.common.ExpiryWarning
import com.ttcn.promotionsdk.presentation.base.PromotionCancellable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.ttcn.promotionsdk.config.eligibleOrderItems


/**
 * **Tầng UI-logic dùng chung** cho widget "Ưu đãi" ở màn thanh toán (`PRMEndowView`) — chạy trên cả
 * Android & iOS. Gom `findEligible`, validate & apply, và quyết định widget-state về một chỗ.
 *
 * Nay cả hai chỉ còn: quan sát [state], map [EndowAppliedDiscount] → model public riêng, render widget
 * theo [EndowWidgetState]. Cùng khuôn với các store khác: `state`/`dispatch`/`watchState`/`currentState`/`clear`.
 *
 * Order-context (orderId/orderValue) đọc từ [PromotionContainer.requestContextProvider] — nguồn duy nhất.
 */
class EndowStore(
    private val findEligibleCampaignsUseCase: FindEligibleCampaignsUseCase,
    private val validateStackableDiscountsUseCase: ValidateStackableDiscountsUseCase,
    private val createRedemptionSessionUseCase: CreateRedemptionSessionUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : PRMStore<EndowState, EndowIntent> {
    /** iOS/Swift: khởi tạo không cần truyền scope (xem `MyPromotionStore`). */
    constructor(
        findEligibleCampaignsUseCase: FindEligibleCampaignsUseCase,
        validateStackableDiscountsUseCase: ValidateStackableDiscountsUseCase,
        createRedemptionSessionUseCase: CreateRedemptionSessionUseCase,
    ) : this(
        findEligibleCampaignsUseCase,
        validateStackableDiscountsUseCase,
        createRedemptionSessionUseCase,
        CoroutineScope(SupervisorJob() + Dispatchers.Default),
    )

    private val _state = MutableStateFlow(EndowState())
    override val state: StateFlow<EndowState> = _state.asStateFlow()

    fun currentState(): EndowState = _state.value

    fun watchState(onEach: (EndowState) -> Unit): PromotionCancellable {
        val job = scope.launch { state.collect { onEach(it) } }
        return PromotionCancellable { job.cancel() }
    }

    fun clear() {
        scope.cancel()
    }

    override fun errorOf(state: EndowState): String? = state.errorCode

    override val consumeErrorIntent: EndowIntent = EndowIntent.ConsumeError

    override fun dispatch(intent: EndowIntent) {
        when (intent) {
            EndowIntent.LoadInitial -> loadInitial()
            is EndowIntent.ValidateAndApply -> scope.launch { validateAndApply(intent.offers) }
            is EndowIntent.SetApplied -> _state.update {
                it.copy(appliedDiscounts = intent.discounts, discountUnavailable = intent.unavailable, errorCode = null)
            }
            EndowIntent.MarkUnavailable -> _state.update { it.copy(discountUnavailable = true) }
            EndowIntent.ClearApplied -> _state.update {
                it.copy(appliedDiscounts = emptyList(), discountUnavailable = false, errorCode = null)
            }
            EndowIntent.ConsumeError -> _state.update { it.copy(errorCode = null) }
        }
    }


    /**
     * User bấm "Thanh toán": tạo phiên redemption cho các ưu đãi đang áp.
     *
     * Trước đây nằm ở `PromotionIntegrateManager` **chỉ bên Android** — nghĩa là luồng đụng tiền
     * này chưa từng dùng chung, và iOS không có. Nay ở đây, hai nền tảng chạy một đường.
     *
     * Thứ tự có ý nghĩa, đừng đảo:
     *  1. Không áp ưu đãi nào → [EndowConfirmResult.Success] **bất kể cờ**. Đơn không dính khuyến
     *     mãi thì kill-switch không có lý do chặn thanh toán.
     *  2. Cờ `VOUCHER_REDEEM` tắt → [EndowConfirmResult.Failure] `PRM_MOB_021`, **không gọi mạng**.
     *  3. `INSUFFICIENT_BUDGET` (trong body **hoặc** HTTP 422) → validate lại để lấy giá mới, cập
     *     nhật [state] rồi báo lỗi. Widget tự re-render vì đọc [state].
     *
     * `suspend` chứ không phải intent: đây là câu hỏi có câu trả lời một-lần ("cho đi tiếp không?"),
     * nhét vào [EndowState] thì native lại phải dựng máy trạng thái để bắt đúng một lần.
     */
    /**
     * Widget có được hiện không, theo **cache** cờ tính năng — trả lời ngay, không chờ mạng.
     *
     * Tên cờ (`VOUCHER_SELECTION`) và luật fail-open nằm ở đây chứ không ở native: trước đây
     * `PRMEndowView.onAttachedToWindow` (Android) và `PromotionSDKImpl.applyFlag` (iOS) mỗi bên tự
     * gọi `PromotionFeatureGate.canShowVoucherSelection()`, tức cùng một quyết định "widget sống hay
     * chết" viết hai lần. Chưa có cache → gate bật lạc quan, widget hiện rồi tự ẩn nếu server nói không.
     */
    fun availabilityFromCache(): Boolean = PromotionFeatureGate.canShowVoucherSelection()

    /** Làm mới cờ từ server rồi trả giá trị thật. Hỏng thì [PromotionFeatureGate.refresh] giữ cache. */
    suspend fun refreshAvailability(): Boolean {
        PromotionFeatureGate.refresh()
        return availabilityFromCache()
    }

    suspend fun confirmRedemption(): EndowConfirmResult {
        val applied = _state.value.appliedDiscounts
        if (applied.isEmpty()) return EndowConfirmResult.Success
        if (!PromotionFeatureGate.canRedeemVoucher()) {
            return EndowConfirmResult.Failure(ErrorCodes.FEATURE_DISABLED)
        }

        val ctx = PromotionContainer.requestContextProvider
        val request = applied.toCreateRedemptionRequest(
            orderId = ctx.getOrderId().orEmpty(),
            orderValue = ctx.getOrderValue().orEmpty(),
        )

        return runCatching { createRedemptionSessionUseCase(request) }
            .fold(
                onSuccess = { response ->
                    val hasBudgetError = response?.validationErrors
                        ?.any { it.code == ErrorCodes.INSUFFICIENT_BUDGET } ?: false
                    if (hasBudgetError) revalidateAfterBudgetError() else EndowConfirmResult.Success
                },
                onFailure = { throwable ->
                    val e = throwable as? PromotionException
                    if (e?.httpStatus == HTTP_UNPROCESSABLE && e.errorCode == ErrorCodes.INSUFFICIENT_BUDGET) {
                        revalidateAfterBudgetError()
                    } else {
                        EndowConfirmResult.Failure(throwable.toErrorCode())
                    }
                },
            )
    }

    /**
     * Ngân sách hết giữa chừng: validate lại để lấy giá đúng rồi mới báo lỗi, nhờ vậy widget hiện số
     * mới thay vì số đã sai.
     *
     * Gác riêng bằng `VOUCHER_APPLY` — đây là lời gọi mạng **thứ hai**, mang cờ khác (chỉ rơi vào
     * đây khi `VOUCHER_REDEEM` bật mà `VOUCHER_APPLY` tắt). Cờ tắt thì không lấy được giá mới, nên
     * **không đụng** [state]: để giá cũ còn hơn ghi đè bằng dữ liệu không có.
     */
    private suspend fun revalidateAfterBudgetError(): EndowConfirmResult {
        if (!PromotionFeatureGate.canApplyVoucher()) {
            return EndowConfirmResult.Failure(ErrorCodes.FEATURE_DISABLED)
        }
        val current = _state.value.appliedDiscounts
        val ctx = PromotionContainer.requestContextProvider
        val request = current.toValidateDiscountsRequest(
            orderId = ctx.getOrderId().orEmpty(),
            orderValue = ctx.getOrderValue().orEmpty(),
        )
        return runCatching { validateStackableDiscountsUseCase(request) }
            .fold(
                onSuccess = { result ->
                    if (result != null) {
                        val details = current.map { result.toEndowAppliedDiscount(it) }
                        _state.update {
                            it.copy(appliedDiscounts = details, discountUnavailable = details.any { d -> !d.valid })
                        }
                    }
                    EndowConfirmResult.Failure(ErrorCodes.INSUFFICIENT_BUDGET)
                },
                onFailure = { EndowConfirmResult.Failure(it.toErrorCode()) },
            )
    }

    /**
     * Khoá đơn hàng của lần nạp gần nhất — `null` = chưa nạp lần nào.
     *
     * Có nó vì store nay sống theo `ViewModelStore` của host (Android) / `PromotionSDKImpl` (iOS),
     * tức **lâu hơn vòng đời một màn thanh toán**. Chỉ gác bằng `hasLoadedInitial` thì host mở lại
     * widget cho ĐƠN KHÁC sẽ thấy nguyên ưu đãi + discount đã áp của đơn cũ, và không có lời gọi
     * mạng nào để sửa. Bản cũ vô tình không dính vì store chết theo `onDetachedFromWindow`.
     */
    private var loadedOrderKey: String? = null

    private fun loadInitial() {
        val ctx = PromotionContainer.requestContextProvider
        // Gồm cả dịch vụ: hai điểm vào cùng đơn nhưng khác `serviceCode` là hai danh sách ưu
        // đãi khác nhau — thiếu nó thì widget giữ nguyên kết quả của dịch vụ trước.
        val orderKey = "${ctx.getOrderId().orEmpty()}|${ctx.getOrderValue().orEmpty()}|${ctx.getService().orEmpty()}"
        if (_state.value.hasLoadedInitial && loadedOrderKey == orderKey) return
        if (loadedOrderKey != null && loadedOrderKey != orderKey) {
            // Đơn khác → vứt sạch kết quả của đơn cũ. Không giữ lại gì: `appliedDiscounts` mang số
            // tiền giảm tính theo orderValue cũ, hiện tiếp là hiện số sai.
            _state.value = EndowState()
        }
        loadedOrderKey = orderKey
        scope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching {
                findEligibleCampaignsUseCase(
                    FindEligibleCampaignsRequest(
                        orderId = ctx.getOrderId().orEmpty(),
                        orderValue = ctx.getOrderValue().orEmpty(),
                        // Kèm `serviceCode` (đổ vào `productId`) — xem `eligibleOrderItems()`.
                        // Phải trùng hàm màn Chọn dùng, nếu không widget và màn chọn hỏi
                        // server hai câu khác nhau rồi ra hai danh sách khác nhau.
                        items = ctx.eligibleOrderItems(),
                        // `metaData` (từ `updateOrderInfo`) — đổ vào orderInfo.metadata (free map),
                        // giống ChoosePromotionStore.buildRequest().
                        orderMetadata = ctx.getMetaData()?.let { mapOf("metaData" to it) },
                        mySize = PAGE_SIZE,
                        otherSize = PAGE_SIZE,
                    )
                )
            }.onSuccess { result ->
                // Ngưỡng "sắp hết hạn" chỉ có ở response DANH SÁCH. Widget không tự hiển thị số ngày,
                // nhưng nó là nơi DUY NHẤT gọi findEligible trước khi màn "Chọn ưu đãi" mở ra bằng dữ
                // liệu preload — không nhớ ở đây thì `EligibleOffer.toChooseOffer` không có ngưỡng nào
                // để dùng và dòng "HSD còn X ngày" bên màn Chọn im lặng biến mất. Idempotent, bỏ qua
                // null — xem [ExpiryWarning].
                ExpiryWarning.remember(result?.expireWarningDate)
                val myOffers = result?.myOffers.orEmpty()
                val otherOffers = result?.otherOffers.orEmpty()
                // Đếm theo totalElements (tổng thật từ server), không theo length mảng đã phân trang.
                //
                // `myTotalElements`/`otherTotalElements` là `Long` KHÔNG nullable (default 0), nên
                // `?:` không bao giờ chạy — BFF bỏ trống field là đếm ra 0, widget rơi vào EMPTY và
                // báo "không có ưu đãi" dù vừa trả về offers. Vì vậy coi 0 = "server không trả" và
                // lùi về số phần tử đã nạp. Server thật sự có 0 ưu đãi thì list cũng rỗng → vẫn 0.
                val total = totalOrSize(result?.myTotalElements, myOffers.size) +
                    totalOrSize(result?.otherTotalElements, otherOffers.size)
                _state.update {
                    it.copy(
                        isLoading = false,
                        myOffers = myOffers,
                        otherOffers = otherOffers,
                        myIsLastPage = result?.myIsLastPage ?: true,
                        otherIsLastPage = result?.otherIsLastPage ?: true,
                        totalVoucherCount = total.toInt(),
                        hasLoadedInitial = true,
                        errorCode = null,
                    )
                }
            }.onFailure { throwable ->
                _state.update { it.copy(isLoading = false, hasLoadedInitial = true, errorCode = throwable.toErrorCode()) }
            }
        }
    }

    /**
     * Validate danh sách ưu đãi đã chọn (từ màn "Chọn ưu đãi") với order hiện tại rồi áp:
     * bất kỳ item nào không hợp lệ → [EndowState.discountUnavailable] = true. Rỗng → xoá áp.
     * Rule diễn giải valid/discount ở domain ([ValidateDiscountsResult]) — dùng chung 2 nền tảng.
     */
    /**
     * Validate + áp, **trả về state cuối** của vòng validate này.
     *
     * `suspend` chứ không phải fire-and-forget là điểm mấu chốt: trước đây `dispatch` chỉ bắn đi, nên
     * màn "Chọn ưu đãi" muốn biết kết quả phải tự dựng máy trạng thái rình `isValidating` true→false
     * — và **cả hai nền tảng đều phải dựng một bản** (`settleCompletion` + `sawValidating` ở
     * `EndowViewModel` Android lẫn iOS), kèm `consumeErrorUnlessSettling` để lỗi không bị widget xoá
     * mất trước khi nơi gọi kịp đọc (`state` là StateFlow nên **conflated**).
     *
     * Trả thẳng state thì cả ba thứ đó biến mất: nơi gọi nhận đúng kết quả của lượt mình gọi, không
     * ai đọc nhờ dòng state chung nữa.
     */
    suspend fun validateAndApply(offers: List<EligibleOffer>): EndowState {
        if (offers.isEmpty()) {
            _state.update { it.copy(appliedDiscounts = emptyList(), discountUnavailable = false, errorCode = null) }
            return _state.value
        }
        val ctx = PromotionContainer.requestContextProvider
        run {
            _state.update { it.copy(isValidating = true) }
            val request = ValidateDiscountsRequest(
                orderId = ctx.getOrderId().orEmpty(),
                orderValue = ctx.getOrderValue().orEmpty(),
                items = offers.map { DiscountItemRequest(objectId = it.id, objectType = it.objectType) },
            )
            runCatching { validateStackableDiscountsUseCase(request) }
                .onSuccess { result ->
                    // Không có kết quả (HTTP 200 nhưng `data` rỗng/parse hỏng) → **báo lỗi**, KHÔNG
                    // coi là áp thành công. Trước đây `?.let{}.orEmpty()` biến null thành list rỗng:
                    // widget về trạng thái "chưa áp gì" còn màn "Chọn ưu đãi" đóng như thành công —
                    // user chọn voucher xong thấy widget không đổi, không có thông báo nào.
                    if (result == null) {
                        _state.update { it.copy(isValidating = false, errorCode = ErrorCodes.NO_RESULT) }
                        return@onSuccess
                    }
                    val details = offers.map { result.toEndowAppliedDiscount(it) }
                    val hasInvalid = details.any { !it.valid }
                    _state.update {
                        it.copy(isValidating = false, appliedDiscounts = details, discountUnavailable = hasInvalid, errorCode = null)
                    }
                }
                .onFailure { throwable ->
                    _state.update { it.copy(isValidating = false, errorCode = throwable.toErrorCode()) }
                }
        }
        return _state.value
    }

    private companion object {
        /** Trùng `pageSize` màn chọn (COLLAPSED không liên quan) — giữ như PRMEndowViewModel cũ. */
        private const val PAGE_SIZE = 20

        /** 422 — server báo hết ngân sách bằng HTTP status thay vì trong body. */
        private const val HTTP_UNPROCESSABLE = 422

        /** `total` từ server nếu có (> 0); không thì lùi về số phần tử đã nạp. */
        private fun totalOrSize(total: Long?, size: Int): Int =
            if (total != null && total > 0) total.toInt() else size
    }
}
