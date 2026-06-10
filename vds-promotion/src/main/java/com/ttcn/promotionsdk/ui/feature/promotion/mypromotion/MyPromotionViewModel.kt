package com.ttcn.promotionsdk.ui.feature.promotion.mypromotion

import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.data.remote.PromotionApiException
import com.ttcn.promotionsdk.core.domain.model.SearchCustomerVouchersRequest
import com.ttcn.promotionsdk.core.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.core.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.ui.base.PRMBaseViewModel

class MyPromotionViewModel(
    private val searchCustomerVouchersUseCase: SearchCustomerVouchersUseCase,
    private val requestContextProvider: PromotionRequestContextProvider,
) :
    PRMBaseViewModel<MyPromotionUiState, MyPromotionAction, MyPromotionEffect>(
        MyPromotionUiState(),
    ) {
    override fun handleAction(action: MyPromotionAction) {
        when (action) {
            MyPromotionAction.LoadInitialIfNeeded -> {
                if (!uiState.value.hasLoadedInitial) {
                    loadVouchers(reset = true, selectedTabCode = null, keyword = "")
                }
            }

            MyPromotionAction.Refresh -> loadVouchers(
                reset = true,
                selectedTabCode = uiState.value.selectedTabCode,
                keyword = uiState.value.keyword,
                isRefresh = true,
            )

            is MyPromotionAction.SelectTab -> loadVouchers(
                reset = true,
                selectedTabCode = action.tabCode,
                keyword = uiState.value.keyword,
            )

            is MyPromotionAction.SearchKeyword -> {
                val trimmedKeyword = action.keyword.trim()
                if (trimmedKeyword.isNotEmpty() && trimmedKeyword.length < 2) {
                    sendEffect(MyPromotionEffect.ShowError(ErrorCodes.KEYWORD_TOO_SHORT))
                } else {
                    val requestKeyword = trimmedKeyword.takeIf { it.isNotBlank() }.orEmpty()
                    val allTabCode = uiState.value.tabs
                        .firstOrNull { it.code == TAB_ALL }
                        ?.code
                        ?: TAB_ALL
                    loadVouchers(
                        reset = true,
                        selectedTabCode = if (requestKeyword.isBlank()) {
                            uiState.value.selectedTabCode
                        } else {
                            allTabCode
                        },
                        keyword = requestKeyword,
                    )
                }
            }

            MyPromotionAction.LoadMore -> loadVouchers(
                reset = false,
                selectedTabCode = uiState.value.selectedTabCode,
                keyword = uiState.value.keyword,
            )
        }
    }

    private fun loadVouchers(
        reset: Boolean,
        selectedTabCode: String?,
        keyword: String,
        isRefresh: Boolean = false,
    ) {
        val currentState = uiState.value
        if (!reset && (
                    currentState.isLastPage ||
                            currentState.isLoadingMore ||
                            currentState.isLoading ||
                            currentState.isRefreshing
                    )
        ) return

        val nextPage = if (reset) 0 else currentState.page + 1
        launch {
            setState {
                copy(
                    isLoading = reset && !isRefresh,
                    isRefreshing = isRefresh,
                    isLoadingMore = !reset,
                    keyword = keyword,
                )
            }

            val customerId = requestContextProvider.getCustomerId()
            if (customerId.isNullOrBlank()) {
                setState {
                    copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                    )
                }
                sendEffect(MyPromotionEffect.ShowError(ErrorCodes.MISSING_CUSTOMER_ID))
                return@launch
            }

            runCatching {
                searchCustomerVouchersUseCase(
                    SearchCustomerVouchersRequest(
                        customerId = customerId,
                        keyword = keyword.takeIf { it.isNotBlank() },
                        serviceCode = null,
                        tab = selectedTabCode,
                        sectionCode = if (reset) null else "my_vouchers",
                        myVouchersPage = nextPage,
                        myVouchersSize = currentState.size,
                        otherVouchersPage = if (reset) 0 else null,
                        otherVouchersSize = if (reset) currentState.size else null,
                    )
                )
            }.onSuccess { response ->
                val incomingTabs = response?.tabs
                    ?.map { it.toMyVoucherTabUi() }
                    ?.sortedBy { it.order }
                    .orEmpty()
                val tabs = incomingTabs.takeIf { it.isNotEmpty() } ?: currentState.tabs
                val selected = response?.selectedTab
                    ?: response?.defaultTab
                    ?: selectedTabCode
                    ?: tabs.firstOrNull()?.code
                val incoming =
                    response?.myVouchers?.content.orEmpty().map { it.toMyVoucherListItem() }
                val merged = if (reset) incoming else currentState.vouchers + incoming
                val pageInfo = response?.myVouchers

                setState {
                    copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        isEmpty = merged.isEmpty(),
                        tabs = tabs,
                        selectedTabCode = selected,
                        vouchers = merged,
                        page = pageInfo?.number ?: nextPage,
                        size = pageInfo?.size ?: currentState.size,
                        isLastPage = pageInfo?.last ?: true,
                        hasLoadedInitial = true,
                    )
                }
            }.onFailure { throwable ->
                val errorCode = throwable.toErrorCode()
                setState {
                    if (reset) {
                        copy(
                            isLoading = false,
                            isRefreshing = false,
                            isLoadingMore = false,
                            vouchers = emptyList(),
                            isEmpty = true,
                            hasLoadedInitial = true,
                        )
                    } else {
                        copy(
                            isLoading = false,
                            isRefreshing = false,
                            isLoadingMore = false,
                        )
                    }
                }
                sendEffect(MyPromotionEffect.ShowError(errorCode))
            }
        }
    }

    override fun onError(throwable: Throwable) {
        setState {
            copy(
                isLoading = false,
                isRefreshing = false,
                isLoadingMore = false,
            )
        }
        sendEffect(MyPromotionEffect.ShowError(throwable.toErrorCode()))
    }

    private companion object {
        private const val TAB_ALL = "all"
    }

    private fun Throwable.toErrorCode(): String {
        return (this as? PromotionApiException)?.errorCode
            ?: message
            ?: ErrorCodes.GENERAL
    }
}
