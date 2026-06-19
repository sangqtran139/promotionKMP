package com.ttcn.promotionsdk.ui.feature.promotion.mypromotion

import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.data.remote.PromotionApiException
import com.ttcn.promotionsdk.core.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.core.domain.model.SearchCustomerVouchersRequest
import com.ttcn.promotionsdk.core.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.ui.base.PRMBaseViewModel

class MyPromotionViewModel(
    private val searchCustomerVouchersUseCase: SearchCustomerVouchersUseCase,
    private val requestContextProvider: PromotionRequestContextProvider,
) :
    PRMBaseViewModel<MyPromotionUiState, MyPromotionAction, MyPromotionEffect>(
        MyPromotionUiState(),
    ) {
    private data class TabCache(
        val vouchers: List<MyVoucherListItem> = emptyList(),
        val page: Int = 0,
        val isLastPage: Boolean = false,
    )

    private val tabCaches = mutableMapOf<String, TabCache>()
    private var latestTabRequestId = 0L

    override fun handleAction(action: MyPromotionAction) {
        when (action) {
            MyPromotionAction.LoadInitialIfNeeded -> {
                if (!uiState.value.hasLoadedInitial) {
                    loadVouchers(
                        tabCode = null,
                        reset = true,
                        keyword = "",
                        showFullLoading = true,
                    )
                }
            }

            MyPromotionAction.Refresh -> refreshCurrentTab()

            is MyPromotionAction.SelectTab -> onTabSelected(action.tabCode)

            is MyPromotionAction.SearchKeyword -> {
                val trimmedKeyword = action.keyword.trim()
                if (trimmedKeyword.isNotEmpty()) {
                    val requestKeyword = trimmedKeyword.takeIf { it.isNotBlank() }.orEmpty()
                    val allTabCode = uiState.value.tabs
                        .firstOrNull { it.code == TAB_ALL }
                        ?.code
                        ?: TAB_ALL
                    loadVouchers(
                        tabCode = if (requestKeyword.isBlank()) {
                            uiState.value.selectedTabCode
                        } else {
                            allTabCode
                        },
                        reset = true,
                        keyword = requestKeyword,
                        showFullLoading = true,
                    )
                }
            }

            MyPromotionAction.LoadMore -> loadVouchers(
                tabCode = uiState.value.selectedTabCode,
                reset = false,
                keyword = uiState.value.keyword,
            )
        }
    }

    private fun onTabSelected(tabCode: String) {
        val cache = tabCaches[tabCode]

        if (cache != null) {
            setState {
                copy(
                    selectedTabCode = tabCode,
                    vouchers = cache.vouchers.toList(),
                    page = cache.page,
                    isLastPage = cache.isLastPage,
                    isEmpty = cache.vouchers.isEmpty(),
                    isLoading = false,
                    isRefreshing = false,
                    isRefreshingTab = true,
                    isLoadingMore = false,
                )
            }

            loadVouchers(
                tabCode = tabCode,
                reset = true,
                keyword = uiState.value.keyword,
                showFullLoading = false,
                isRefreshTab = true,
            )
            return
        }

        setState {
            copy(
                selectedTabCode = tabCode,
                isEmpty = false,
                isLoadingMore = false,
                isRefreshingTab = false,
                isLoading = true,
            )
        }
        loadVouchers(
            tabCode = tabCode,
            reset = true,
            keyword = uiState.value.keyword,
            showFullLoading = false,
            keepCurrentListWhileLoading = true,
        )
    }

    private fun refreshCurrentTab() {
        loadVouchers(
            tabCode = uiState.value.selectedTabCode,
            reset = true,
            keyword = uiState.value.keyword,
            showFullLoading = false,
            isPullRefresh = true,
        )
    }

    private fun loadVouchers(
        tabCode: String?,
        reset: Boolean,
        keyword: String,
        showFullLoading: Boolean = false,
        isPullRefresh: Boolean = false,
        isRefreshTab: Boolean = false,
        keepCurrentListWhileLoading: Boolean = false,
    ) {
        val currentState = uiState.value
        if (!reset && (
                    currentState.isLastPage ||
                            currentState.isLoadingMore ||
                            currentState.isLoading ||
                            currentState.isRefreshing ||
                            currentState.isRefreshingTab ||
                            currentState.vouchers.isEmpty()
                    )
        ) return

        val requestTabCode = tabCode ?: currentState.selectedTabCode
        val nextPage = if (reset) 0 else currentState.page + 1
        val requestId = if (reset) ++latestTabRequestId else latestTabRequestId

        launch {
            if (reset) {
                setState {
                    copy(
                        isLoading = showFullLoading || keepCurrentListWhileLoading,
                        isRefreshing = isPullRefresh,
                        isRefreshingTab = isRefreshTab,
                        isLoadingMore = false,
                        keyword = keyword,
                        selectedTabCode = tabCode ?: selectedTabCode,
                        vouchers = when {
                            keepCurrentListWhileLoading -> vouchers
                            isRefreshTab || isPullRefresh -> vouchers
                            showFullLoading -> emptyList()
                            else -> vouchers
                        },
                    )
                }
            } else {
                setState {
                    copy(
                        isLoadingMore = true,
                        keyword = keyword,
                    )
                }
            }

            val customerId = requestContextProvider.getCustomerId()
            if (customerId.isNullOrBlank()) {
                if (!shouldApplyResponse(requestId, requestTabCode, reset)) return@launch
                setState {
                    copy(
                        isLoading = false,
                        isRefreshing = false,
                        isRefreshingTab = false,
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
                        tab = requestTabCode,
                        sectionCode = if (reset) null else "my_vouchers",
                        myVouchersPage = nextPage,
                        myVouchersSize = currentState.size,
                        otherVouchersPage = if (reset) 0 else null,
                        otherVouchersSize = if (reset) currentState.size else null,
                    )
                )
            }.onSuccess { response ->
                if (!shouldApplyResponse(requestId, requestTabCode, reset)) return@onSuccess

                val incomingTabs = response?.tabs
                    ?.map { it.toMyVoucherTabUi() }
                    ?.sortedBy { it.order }
                    .orEmpty()
                val tabs = incomingTabs.takeIf { it.isNotEmpty() } ?: uiState.value.tabs
                val selected = response?.selectedTab
                    ?: response?.defaultTab
                    ?: requestTabCode
                    ?: tabs.firstOrNull()?.code
                val incoming =
                    response?.myVouchers?.content.orEmpty().map { it.toMyVoucherListItem() }
                val pageInfo = response?.myVouchers
                val resolvedPage = pageInfo?.number ?: nextPage
                val resolvedIsLastPage = pageInfo?.last ?: true

                val merged = if (reset) {
                    incoming.toList()
                } else {
                    (uiState.value.vouchers + incoming).toList()
                }

                val cacheTabCode = requestTabCode ?: selected
                if (!cacheTabCode.isNullOrBlank()) {
                    tabCaches[cacheTabCode] = TabCache(
                        vouchers = merged.toList(),
                        page = resolvedPage,
                        isLastPage = resolvedIsLastPage,
                    )
                }

                setState {
                    copy(
                        isLoading = false,
                        isRefreshing = false,
                        isRefreshingTab = false,
                        isLoadingMore = false,
                        isEmpty = merged.isEmpty(),
                        tabs = tabs,
                        selectedTabCode = selected,
                        vouchers = merged.toList(),
                        page = resolvedPage,
                        size = pageInfo?.size ?: currentState.size,
                        isLastPage = resolvedIsLastPage,
                        hasLoadedInitial = true,
                    )
                }
            }.onFailure { throwable ->
                if (!shouldApplyResponse(requestId, requestTabCode, reset)) return@onFailure

                val errorCode = throwable.toErrorCode()
                val hasCache = requestTabCode?.let { tabCaches[it] } != null
                setState {
                    when {
                        reset && hasCache -> {
                            copy(
                                isLoading = false,
                                isRefreshing = false,
                                isRefreshingTab = false,
                                isLoadingMore = false,
                                hasLoadedInitial = true,
                            )
                        }

                        reset -> {
                            copy(
                                isLoading = false,
                                isRefreshing = false,
                                isRefreshingTab = false,
                                isLoadingMore = false,
                                vouchers = if (vouchers.isNotEmpty()) vouchers else emptyList(),
                                isEmpty = vouchers.isEmpty(),
                                hasLoadedInitial = true,
                            )
                        }

                        else -> {
                            copy(
                                isLoading = false,
                                isRefreshing = false,
                                isRefreshingTab = false,
                                isLoadingMore = false,
                            )
                        }
                    }
                }
                sendEffect(MyPromotionEffect.ShowError(errorCode))
            }
        }
    }

    private fun shouldApplyResponse(
        requestId: Long,
        requestTabCode: String?,
        reset: Boolean,
    ): Boolean {
        if (requestId != latestTabRequestId) return false
        if (requestTabCode != null && uiState.value.selectedTabCode != requestTabCode) return false
        if (!reset && uiState.value.isRefreshingTab) return false
        return true
    }

    override fun onError(throwable: Throwable) {
        setState {
            copy(
                isLoading = false,
                isRefreshing = false,
                isRefreshingTab = false,
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
