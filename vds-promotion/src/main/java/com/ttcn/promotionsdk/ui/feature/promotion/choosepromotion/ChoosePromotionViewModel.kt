package com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion

import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.data.remote.PromotionApiException
import com.ttcn.promotionsdk.core.domain.repository.PromotionRepository
import com.ttcn.promotionsdk.ui.base.PRMBaseViewModel
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.ChoosePromotionEffect.*
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.FakeVoucherData
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.toMyVoucherListItem

class ChoosePromotionViewModel(
    private val repository: PromotionRepository,
    private val requestContextProvider: PromotionRequestContextProvider,
) : PRMBaseViewModel<ChoosePromotionUiState, ChoosePromotionAction, ChoosePromotionEffect>(
    ChoosePromotionUiState(),
) {
    override fun handleAction(action: ChoosePromotionAction) {
        when (action) {
            is ChoosePromotionAction.InitWithData -> {
                if (uiState.value.hasLoadedInitial) return
                setState {
                    copy(
                        vouchers = action.myVouchers,
                        otherVouchers = action.otherVouchers,
                        hasLoadedInitial = true,
                        isEmpty = action.myVouchers.isEmpty() && action.otherVouchers.isEmpty(),
                    )
                }
            }

            ChoosePromotionAction.LoadInitialIfNeeded -> {
                if (!uiState.value.hasLoadedInitial) {
                    loadVouchers(reset = true, serviceCode = "vay", keyword = "")
                }
            }
            ChoosePromotionAction.Refresh -> loadVouchers(
                reset = true,
                serviceCode = "vay",
                keyword = uiState.value.keyword,
                isRefresh = true,
            )
            is ChoosePromotionAction.SearchKeyword -> {
                val trimmedKeyword = action.keyword.trim()
                if (trimmedKeyword.isNotEmpty() && trimmedKeyword.length < 2) {
                    sendEffect(ShowError("keyword_too_short"))
                } else {
                    loadVouchers(
                        reset = true,
                        serviceCode = "vay",
                        keyword = trimmedKeyword.takeIf { it.isNotBlank() }.orEmpty(),
                    )
                }
            }
            ChoosePromotionAction.LoadMoreMyVouchers -> {
                val nextPage = uiState.value.page + 1
                if (FakeVoucherData.isLastPage(nextPage)) return
                setState {
                    copy(
                        vouchers = vouchers + FakeVoucherData.getMyVouchers(page = nextPage),
                        page = nextPage,
                        isLastPage = FakeVoucherData.isNextLastPage(nextPage),
                    )
                }
//                loadMoreMyVouchers()
            }
            ChoosePromotionAction.LoadMoreOtherVouchers -> {
                // TODO: xóa khi có API
                val nextPage = uiState.value.otherPage + 1
                if (FakeVoucherData.isLastPage(nextPage)) return
                setState {
                    copy(
                        otherVouchers = otherVouchers + FakeVoucherData.getOtherVouchers(page = nextPage),
                        otherPage = nextPage,
                        isLastOtherPage = FakeVoucherData.isLastPage(nextPage),
                    )
                }
                // loadMoreOtherVouchers()
            }
        }
    }

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
                sendEffect(ShowError("missing_customer_id"))
                return@launch
            }

            runCatching {
                repository.searchCustomerVouchers(
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
            }.onSuccess { response ->
                val myVouchers = response?.myVouchers?.content.orEmpty().map { it.toMyVoucherListItem() }
                val otherVouchers = response?.otherVouchers?.content.orEmpty().map { it.toMyVoucherListItem() }
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
                        isLastPage = myPageInfo?.last ?: true,
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

    private fun loadMoreMyVouchers() {
        val currentState = uiState.value
        if (currentState.isLastPage || currentState.isLoadingMore || currentState.isLoading) return

        val nextPage = currentState.page + 1
        launch {
            setState { copy(isLoadingMore = true) }

            val customerId = requestContextProvider.getCustomerId()
            if (customerId.isNullOrBlank()) {
                setState { copy(isLoadingMore = false) }
                sendEffect(ShowError("missing_customer_id"))
                return@launch
            }

            runCatching {
                repository.searchCustomerVouchers(
                    customerId = customerId,
                    keyword = currentState.keyword.takeIf { it.isNotBlank() },
                    serviceCode = "vay",
                    tab = null,
                    sectionCode = "my_vouchers",
                    myVouchersPage = nextPage,
                    myVouchersSize = currentState.size,
                    otherVouchersPage = null,
                    otherVouchersSize = null,
                )
            }.onSuccess { response ->
                val incoming = response?.myVouchers?.content.orEmpty().map { it.toMyVoucherListItem() }
                val pageInfo = response?.myVouchers
                setState {
                    copy(
                        isLoadingMore = false,
                        vouchers = currentState.vouchers + incoming,
                        page = pageInfo?.number ?: nextPage,
                        isLastPage = pageInfo?.last ?: true,
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

        val nextPage = currentState.otherPage + 1
        launch {
            setState { copy(isLoadingMoreOther = true) }

            val customerId = requestContextProvider.getCustomerId()
            if (customerId.isNullOrBlank()) {
                setState { copy(isLoadingMoreOther = false) }
                sendEffect(ShowError("missing_customer_id"))
                return@launch
            }

            runCatching {
                repository.searchCustomerVouchers(
                    customerId = customerId,
                    keyword = currentState.keyword.takeIf { it.isNotBlank() },
                    serviceCode = "vay",
                    tab = null,
                    sectionCode = "other_vouchers",
                    myVouchersPage = null,
                    myVouchersSize = null,
                    otherVouchersPage = nextPage,
                    otherVouchersSize = currentState.otherSize,
                )
            }.onSuccess { response ->
                val incoming = response?.otherVouchers?.content.orEmpty().map { it.toMyVoucherListItem() }
                val pageInfo = response?.otherVouchers
                setState {
                    copy(
                        isLoadingMoreOther = false,
                        otherVouchers = currentState.otherVouchers + incoming,
                        otherPage = pageInfo?.number ?: nextPage,
                        isLastOtherPage = pageInfo?.last ?: true,
                    )
                }
            }.onFailure { throwable ->
                setState { copy(isLoadingMoreOther = false) }
                sendEffect(ShowError(throwable.toErrorCode()))
            }
        }
    }

    override fun onError(throwable: Throwable) {
        setState { copy(isLoading = false, isRefreshing = false, isLoadingMore = false, isLoadingMoreOther = false) }
        sendEffect(ShowError(throwable.toErrorCode()))
    }

    private fun Throwable.toErrorCode(): String {
        return (this as? PromotionApiException)?.errorCode ?: message ?: "error_general"
    }
}