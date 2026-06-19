package com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion

import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.core.domain.exception.toErrorCode
import com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersRequest
import com.ttcn.promotionsdk.core.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.core.domain.usecase.ValidateStackableDiscountsUseCase
import com.ttcn.promotionsdk.ui.base.PRMBaseViewModel
import com.ttcn.promotionsdk.ui.feature.promotion.ext.toAppliedDiscounts
import com.ttcn.promotionsdk.ui.feature.promotion.ext.toValidateDiscountsRequest
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.ChoosePromotionEffect.ApplyValidatedVouchers
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.ChoosePromotionEffect.ShowError
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyVoucherListItem
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.toMyVoucherListItem

internal class ChoosePromotionViewModel(
    private val searchCustomerVouchersUseCase: SearchCustomerVouchersUseCase,
    private val validateStackableDiscountsUseCase: ValidateStackableDiscountsUseCase,
    private val requestContextProvider: PromotionRequestContextProvider,
) : PRMBaseViewModel<ChoosePromotionUiState, ChoosePromotionAction, ChoosePromotionEffect>(
    ChoosePromotionUiState(),
) {
    override fun handleAction(action: ChoosePromotionAction) {
        when (action) {
            is ChoosePromotionAction.LoadInitial -> loadVouchers(
                reset = true,
                serviceCode = requestContextProvider.getService(),
                keyword = "",
            )

            // Nhận data đã load sẵn từ PRMEndowView → tránh double API call.
            // Nếu cả hai rỗng → gọi API.
            is ChoosePromotionAction.PreloadVouchers -> {
                val hasData = action.myVouchers.isNotEmpty() || action.otherVouchers.isNotEmpty()
                if (hasData) {
                    setState {
                        copy(
                            hasLoadedInitial = true,
                            isLoading = false,
                            vouchers = action.myVouchers,
                            otherVouchers = action.otherVouchers,
                            isEmpty = action.myVouchers.isEmpty() && action.otherVouchers.isEmpty(),
                        )
                    }
                } else {
                    loadVouchers(
                        reset = true,
                        serviceCode = requestContextProvider.getService(),
                        keyword = ""
                    )
                }
            }

            is ChoosePromotionAction.Refresh -> loadVouchers(
                reset = true,
                serviceCode = requestContextProvider.getService(),
                keyword = uiState.value.keyword,
                isRefresh = true,
            )

            is ChoosePromotionAction.SearchKeyword -> onSearch(action.keyword)
            is ChoosePromotionAction.LoadMoreMyVouchers -> loadMoreMyVouchers()
            is ChoosePromotionAction.LoadMoreOtherVouchers -> loadMoreOtherVouchers()
            is ChoosePromotionAction.ValidateAndApply -> validateAndApply(action.selected)
        }
    }

    // ─── Load vouchers ────────────────────────────────────────────────────────

    private fun loadVouchers(
        reset: Boolean,
        serviceCode: String?,
        keyword: String,
        isRefresh: Boolean = false,
    ) {
        val currentState = uiState.value
        launch {
            setState {
                copy(
                    isLoading = reset && !isRefresh,
                    isRefreshing = isRefresh,
                    isLoadingMore = false,
                    isLoadingMoreOther = false,
                    keyword = keyword,
                )
            }

            val customerId = requestContextProvider.getCustomerId()
            if (customerId.isNullOrBlank()) {
                setState { copy(isLoading = false, isRefreshing = false) }
                sendEffect(ShowError(ErrorCodes.MISSING_CUSTOMER_ID))
                return@launch
            }

            runCatching {
                searchCustomerVouchersUseCase(
                    SearchCustomerVouchersRequest(
                        customerId = customerId,
                        keyword = keyword.takeIf { it.isNotBlank() },
                        serviceCode = serviceCode,
                        sectionCode = null,
                        tab = null,
                        myVouchersPage = 0,
                        myVouchersSize = currentState.size,
                        otherVouchersPage = 0,
                        otherVouchersSize = currentState.otherSize,
                    )
                )
            }.onSuccess { response ->
                val myVouchers =
                    response?.myVouchers?.content.orEmpty().map { it.toMyVoucherListItem() }
                val otherVouchers =
                    response?.otherVouchers?.content.orEmpty().map { it.toMyVoucherListItem() }
                val myPageInfo = response?.myVouchers
                val otherPageInfo = response?.otherVouchers
                setState {
                    copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        isLoadingMoreOther = false,
                        isEmpty = myVouchers.isEmpty() && otherVouchers.isEmpty(),
                        tabs = emptyList(),
                        selectedTabCode = null,
                        vouchers = myVouchers,
                        otherVouchers = otherVouchers,
                        page = myPageInfo?.number ?: 0,
                        size = myPageInfo?.size ?: currentState.size,
                        isLastPage = myPageInfo?.last ?: false,
                        otherPage = otherPageInfo?.number ?: 0,
                        otherSize = otherPageInfo?.size ?: currentState.otherSize,
                        isLastOtherPage = otherPageInfo?.last ?: true,
                        hasLoadedInitial = true,
                    )
                }
            }.onFailure { throwable ->
                setState {
                    copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        isLoadingMoreOther = false,
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

    // ─── Search ───────────────────────────────────────────────────────────────

    private fun onSearch(keyword: String) {
        val trimmed = keyword.trim()
        loadVouchers(
            reset = true,
            serviceCode = requestContextProvider.getService(),
            keyword = trimmed
        )
    }

    // ─── Load more ────────────────────────────────────────────────────────────

    private fun loadMoreMyVouchers() {
        val currentState = uiState.value
        if (currentState.isLastPage || currentState.isLoadingMore || currentState.isLoading) return
        launch {
            setState { copy(isLoadingMore = true) }
            val customerId = requestContextProvider.getCustomerId()
            if (customerId.isNullOrBlank()) {
                setState { copy(isLoadingMore = false) }
                sendEffect(ShowError(ErrorCodes.MISSING_CUSTOMER_ID))
                return@launch
            }
            runCatching {
                searchCustomerVouchersUseCase(
                    SearchCustomerVouchersRequest(
                        customerId = customerId,
                        keyword = currentState.keyword.takeIf { it.isNotBlank() },
                        serviceCode = requestContextProvider.getService(),
                        tab = null,
                        sectionCode = "my_vouchers",
                        myVouchersPage = currentState.page + 1,
                        myVouchersSize = currentState.size,
                        otherVouchersPage = null,
                        otherVouchersSize = null,
                    )
                )
            }.onSuccess { response ->
                val incoming =
                    response?.myVouchers?.content.orEmpty().map { it.toMyVoucherListItem() }
                val pageInfo = response?.myVouchers
                setState {
                    copy(
                        isLoadingMore = false,
                        vouchers = currentState.vouchers + incoming,
                        page = pageInfo?.number ?: currentState.page,
                        isLastPage = pageInfo?.last ?: false,
                    )
                }
            }.onFailure { throwable ->
                setState { copy(isLoadingMore = false) }
                sendEffect(ShowError(throwable.toErrorCode()))
            }
        }
    }

    private fun loadMoreOtherVouchers() {
        val currentState = uiState.value
        if (currentState.isLastOtherPage || currentState.isLoadingMoreOther || currentState.isLoading) return
        launch {
            setState { copy(isLoadingMoreOther = true) }
            val customerId = requestContextProvider.getCustomerId()
            if (customerId.isNullOrBlank()) {
                setState { copy(isLoadingMoreOther = false) }
                sendEffect(ShowError(ErrorCodes.MISSING_CUSTOMER_ID))
                return@launch
            }
            runCatching {
                searchCustomerVouchersUseCase(
                    SearchCustomerVouchersRequest(
                        customerId = customerId,
                        keyword = currentState.keyword.takeIf { it.isNotBlank() },
                        serviceCode = requestContextProvider.getService(),
                        tab = null,
                        sectionCode = "other_vouchers",
                        myVouchersPage = null,
                        myVouchersSize = null,
                        otherVouchersPage = currentState.otherPage + 1,
                        otherVouchersSize = currentState.otherSize,
                    )
                )
            }.onSuccess { response ->
                val incoming =
                    response?.otherVouchers?.content.orEmpty().map { it.toMyVoucherListItem() }
                val pageInfo = response?.otherVouchers
                setState {
                    copy(
                        isLoadingMoreOther = false,
                        otherVouchers = currentState.otherVouchers + incoming,
                        otherPage = pageInfo?.number ?: currentState.otherPage,
                        isLastOtherPage = pageInfo?.last ?: true,
                    )
                }
            }.onFailure { throwable ->
                setState { copy(isLoadingMoreOther = false) }
                sendEffect(ShowError(throwable.toErrorCode()))
            }
        }
    }

    // ─── Validate and apply ───────────────────────────────────────────────────

    /**
     * Gọi validateStackableDiscounts với danh sách voucher user đã chọn.
     *
     * Response trả về [discountDetails]:
     *  - [valid] = true  → voucher áp dụng thành công
     *  - [valid] = false → voucher thất bại (conflict, hết budget, v.v.)
     *
     * Dùng thẳng discountDetails từ response, không map lại.
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
                    // Map domain result → AppliedDiscount cho public surface (callback/PRMEndowView)
                    val details = response?.items.orEmpty().toAppliedDiscounts()
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
}