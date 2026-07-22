package com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion

import androidx.lifecycle.viewModelScope
import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.core.domain.exception.toErrorCode
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffersResult
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleSection
import com.ttcn.promotionsdk.core.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.core.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.core.domain.usecase.ValidateStackableDiscountsUseCase
import com.ttcn.promotionsdk.ui.base.PRMBaseViewModel
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.ChoosePromotionEffect.ApplyValidatedVouchers
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.ChoosePromotionEffect.ShowError
import com.ttcn.promotionsdk.ui.feature.promotion.ext.appliedDiscountFor
import com.ttcn.promotionsdk.ui.feature.promotion.ext.toMyVoucherListItem
import com.ttcn.promotionsdk.ui.feature.promotion.ext.withExpiryWarning
import com.ttcn.promotionsdk.ui.feature.promotion.ext.toValidateDiscountsRequest
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyVoucherListItem
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.toMyVoucherTabUi
import com.ttcn.promotionsdk.ui.feature.promotion.searchmypromotion.SearchMyPromotionViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Màn "Chọn ưu đãi" của luồng checkout.
 *
 * Dùng `findEligible` — **không** phải `searchVouchers`. Hai API trả hai tập khác nhau:
 * `searchVouchers` chỉ có voucher khách đã sở hữu, còn `findEligible` xét đơn hàng và trả thêm
 * nhóm campaign công khai khách chưa nhận (`otherOffers`). Bản Android trước đây gọi `searchVouchers`
 * nên section "Ưu đãi khác" luôn rỗng.
 *
 * Tìm kiếm chạy **server-side**: `keyword` gửi kèm mỗi request `findEligible` (v1.6 §7.3), server
 * lọc cả `myOffers` lẫn `otherOffers`. iOS cùng cơ chế (parity) — không còn lọc client trong bộ nhớ.
 */
internal class ChoosePromotionViewModel(
    private val findEligibleCampaignsUseCase: FindEligibleCampaignsUseCase,
    private val validateStackableDiscountsUseCase: ValidateStackableDiscountsUseCase,
    private val requestContextProvider: PromotionRequestContextProvider,
) : PRMBaseViewModel<ChoosePromotionUiState, ChoosePromotionAction, ChoosePromotionEffect>(
    ChoosePromotionUiState(),
) {

    /** Nguồn sự thật của danh sách; [ChoosePromotionUiState] giữ bản đã map sang model UI. */
    private var myLoaded: List<EligibleOffer> = emptyList()
    private var otherLoaded: List<EligibleOffer> = emptyList()
    /** Ngưỡng cảnh báo sắp hết hạn (ngày) từ result — dùng gán "Còn X ngày" lúc publish. */
    private var expireWarningDate: Int? = null

    private var debounceJob: Job? = null

    override fun handleAction(action: ChoosePromotionAction) {
        when (action) {
            is ChoosePromotionAction.LoadInitial -> loadOffers(isRefresh = false)

            // Nhận data đã load sẵn từ PRMEndowView → tránh double API call.
            // Nếu cả hai rỗng → gọi API.
            is ChoosePromotionAction.PreloadVouchers -> {
                val hasData = action.myOffers.isNotEmpty() || action.otherOffers.isNotEmpty()
                if (hasData) {
                    myLoaded = action.myOffers
                    otherLoaded = action.otherOffers
                    setState { copy(hasLoadedInitial = true, isLoading = false) }
                    publishLists()
                } else {
                    loadOffers(isRefresh = false)
                }
            }

            is ChoosePromotionAction.Refresh -> loadOffers(isRefresh = true)
            is ChoosePromotionAction.QueryChanged -> onQueryChanged(action.keyword)
            is ChoosePromotionAction.Search -> performSearch(immediate = true)
            is ChoosePromotionAction.ClearKeyword -> onClearKeyword()
            is ChoosePromotionAction.LoadMoreMyVouchers -> loadMore(EligibleSection.MY_OFFERS)
            is ChoosePromotionAction.LoadMoreOtherVouchers -> loadMore(EligibleSection.OTHER_OFFERS)
            is ChoosePromotionAction.ValidateAndApply -> validateAndApply(action.selected)
        }
    }

    // ─── Load offers ──────────────────────────────────────────────────────────

    private fun loadOffers(isRefresh: Boolean) {
        launch {
            setState {
                copy(
                    isLoading = !isRefresh,
                    isRefreshing = isRefresh,
                    isLoadingMore = false,
                    isLoadingMoreOther = false,
                )
            }

            val customerId = requestContextProvider.getCustomerId()
            if (customerId.isNullOrBlank()) {
                setState { copy(isLoading = false, isRefreshing = false) }
                sendEffect(ShowError(ErrorCodes.MISSING_CUSTOMER_ID))
                return@launch
            }

            runCatching {
                findEligibleCampaignsUseCase(buildRequest(customerId, section = null))
            }.onSuccess { result ->
                myLoaded = result?.myOffers.orEmpty()
                otherLoaded = result?.otherOffers.orEmpty()
                expireWarningDate = result?.expireWarningDate
                setState {
                    copy(
                        isLoading = false,
                        isRefreshing = false,
                        tabs = result?.tabs.orEmpty().map { it.toMyVoucherTabUi() }.sortedBy { it.order },
                        selectedTabCode = result?.activeTab,
                        page = 0,
                        otherPage = 0,
                        isLastPage = result?.myIsLastPage ?: true,
                        isLastOtherPage = result?.otherIsLastPage ?: true,
                        hasLoadedInitial = true,
                    )
                }
                publishLists()
            }.onFailure { throwable ->
                myLoaded = emptyList()
                otherLoaded = emptyList()
                setState {
                    copy(
                        isLoading = false,
                        isRefreshing = false,
                        vouchers = emptyList(),
                        otherVouchers = emptyList(),
                        isEmpty = true,
                        hasLoadedInitial = true,
                    )
                }
                sendEffect(ShowError(throwable.toErrorCode()))
            }
        }
    }

    // ─── Load more ────────────────────────────────────────────────────────────

    /**
     * Hai nhóm phân trang **độc lập**: chỉ nhóm được yêu cầu mới có dữ liệu trong response,
     * nhóm kia trả null (xem [EligibleOffersResult]).
     */
    private fun loadMore(section: EligibleSection) {
        val state = uiState.value
        if (state.isLoading) return

        val isMine = section == EligibleSection.MY_OFFERS
        if (isMine && (state.isLastPage || state.isLoadingMore)) return
        if (!isMine && (state.isLastOtherPage || state.isLoadingMoreOther)) return

        launch {
            setState {
                if (isMine) copy(isLoadingMore = true) else copy(isLoadingMoreOther = true)
            }

            val customerId = requestContextProvider.getCustomerId()
            if (customerId.isNullOrBlank()) {
                setState { copy(isLoadingMore = false, isLoadingMoreOther = false) }
                sendEffect(ShowError(ErrorCodes.MISSING_CUSTOMER_ID))
                return@launch
            }

            val nextPage = if (isMine) state.page + 1 else state.otherPage + 1
            runCatching {
                findEligibleCampaignsUseCase(buildRequest(customerId, section, nextPage))
            }.onSuccess { result ->
                if (isMine) {
                    myLoaded = myLoaded + result?.myOffers.orEmpty()
                    setState {
                        copy(
                            isLoadingMore = false,
                            page = nextPage,
                            isLastPage = result?.myIsLastPage ?: true,
                        )
                    }
                } else {
                    otherLoaded = otherLoaded + result?.otherOffers.orEmpty()
                    setState {
                        copy(
                            isLoadingMoreOther = false,
                            otherPage = nextPage,
                            isLastOtherPage = result?.otherIsLastPage ?: true,
                        )
                    }
                }
                publishLists()
            }.onFailure { throwable ->
                setState { copy(isLoadingMore = false, isLoadingMoreOther = false) }
                sendEffect(ShowError(throwable.toErrorCode()))
            }
        }
    }

    /**
     * [section] null → lấy cả hai nhóm từ trang 0. Rule phân trang độc lập 2 nhóm nằm ở domain
     * ([FindEligibleCampaignsRequest.forSectionPage]) — dùng chung Android & iOS.
     */
    private fun buildRequest(
        customerId: String,
        section: EligibleSection?,
        nextPage: Int = 0,
    ): FindEligibleCampaignsRequest {
        val state = uiState.value
        return FindEligibleCampaignsRequest(
            customerId = customerId,
            orderId = requestContextProvider.getOrderId().orEmpty(),
            orderValue = requestContextProvider.getOrderValue().orEmpty(),
            // TODO(order-items): PromotionRequestContextProvider chưa cung cấp danh sách sản phẩm,
            // nên server chỉ trả campaign cấp đơn, bỏ qua campaign yêu cầu SKU. iOS lấy từ
            // PromotionCheckoutData.orderItems. Cần bổ sung `getOrderItems()` vào provider.
            items = emptyList(),
            tabCode = null,
            // Server lọc theo tên/mã voucher (v1.6 §7.3); áp cho cả load đầu lẫn load-more.
            keyword = state.keyword,
            mySize = state.size,
            otherSize = state.otherSize,
        ).forSectionPage(
            section = section,
            nextPage = nextPage,
            currentMyPage = state.page,
            currentOtherPage = state.otherPage,
        )
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    /**
     * Khung tìm kiếm, dựng theo đúng khuôn [SearchMyPromotionViewModel]: gõ mỗi ký tự →
     * debounce [DEBOUNCE_MS] → [search]; xoá trắng thì reset ngay, không chờ.
     */
    private fun onQueryChanged(keyword: String) {
        debounceJob?.cancel()
        setState { copy(keyword = keyword) }

        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) resetSearchResults() else scheduleDebouncedSearch(trimmed)
    }

    private fun performSearch(immediate: Boolean) {
        debounceJob?.cancel()
        val trimmed = uiState.value.keyword.trim()
        when {
            trimmed.isEmpty() -> resetSearchResults()
            immediate -> search(trimmed)
            else -> scheduleDebouncedSearch(trimmed)
        }
    }

    private fun scheduleDebouncedSearch(keyword: String) {
        debounceJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            search(keyword)
        }
    }

    private fun onClearKeyword() {
        debounceJob?.cancel()
        setState { copy(keyword = "") }
        resetSearchResults()
    }

    /**
     * Tìm kiếm phía server (v1.6 §7.3): `state.keyword` đã set ở [onQueryChanged]; reload lại 2 nhóm
     * từ trang 0 kèm `keyword` (buildRequest tự gửi). Server lọc cả myOffers lẫn otherOffers.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun search(keyword: String) {
        loadOffers(isRefresh = false)
    }

    /** Xoá keyword → reload danh sách đầy đủ (state.keyword rỗng → không gửi keyword). */
    private fun resetSearchResults() {
        loadOffers(isRefresh = false)
    }

    /** Đổ [myLoaded] / [otherLoaded] sang model UI. */
    private fun publishLists() {
        val my = myLoaded.map { it.toMyVoucherListItem().withExpiryWarning(expireWarningDate) }
        val other = otherLoaded.map { it.toMyVoucherListItem().withExpiryWarning(expireWarningDate) }
        setState {
            copy(
                vouchers = my,
                otherVouchers = other,
                isEmpty = my.isEmpty() && other.isEmpty(),
            )
        }
    }

    // ─── Validate and apply ───────────────────────────────────────────────────

    /**
     * Gọi validateStackableDiscounts với danh sách voucher user đã chọn.
     *
     * Response trả về `discountDetails`:
     *  - `valid` = true  → voucher áp dụng thành công
     *  - `valid` = false → voucher thất bại (conflict, hết budget, v.v.)
     */
    private fun validateAndApply(selected: List<MyVoucherListItem>) {
        if (selected.isEmpty()) {
            sendEffect(ApplyValidatedVouchers(emptyList()))
            return
        }
        launch {
            setState { copy(isValidating = true) }

            val customerId = requestContextProvider.getCustomerId()
            if (customerId.isNullOrBlank()) {
                setState { copy(isValidating = false) }
                sendEffect(ShowError(ErrorCodes.MISSING_CUSTOMER_ID))
                return@launch
            }

            val request = selected.toValidateDiscountsRequest(
                customerId = customerId,
                orderId = requestContextProvider.getOrderId().orEmpty(),
                orderValue = requestContextProvider.getOrderValue().orEmpty(),
            )

            runCatching { validateStackableDiscountsUseCase(request) }
                .onSuccess { response ->
                    // Lặp theo offer đã chọn + diễn giải qua isValidFor/discountFor (đối xứng iOS).
                    val details = response
                        ?.let { r -> selected.map { r.appliedDiscountFor(it.voucherId, it.objectType) } }
                        .orEmpty()
                    setState { copy(isValidating = false) }
                    sendEffect(ApplyValidatedVouchers(details))
                }
                .onFailure { throwable ->
                    setState { copy(isValidating = false) }
                    sendEffect(ShowError(throwable.toErrorCode()))
                }
        }
    }

    override fun onError(throwable: Throwable) {
        setState {
            copy(
                isLoading = false,
                isRefreshing = false,
                isLoadingMore = false,
                isLoadingMoreOther = false,
                isValidating = false,
            )
        }
        sendEffect(ShowError(throwable.toErrorCode()))
    }

    private companion object {
        /** Trùng `SearchMyPromotionViewModel.DEBOUNCE_MS` và `ChoosePromotionViewModel.searchDebounceMs` (iOS). */
        private const val DEBOUNCE_MS = 400L
    }
}
