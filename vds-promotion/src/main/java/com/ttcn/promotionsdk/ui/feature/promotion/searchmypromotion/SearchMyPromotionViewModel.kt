package com.ttcn.promotionsdk.ui.feature.promotion.searchmypromotion

import androidx.lifecycle.viewModelScope
import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersRequest
import com.ttcn.promotionsdk.core.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.core.domain.exception.toErrorCode
import com.ttcn.promotionsdk.core.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.ui.base.PRMBaseViewModel
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.toMyVoucherListItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class SearchMyPromotionViewModel(
    private val searchCustomerVouchersUseCase: SearchCustomerVouchersUseCase,
    private val requestContextProvider: PromotionRequestContextProvider,
) : PRMBaseViewModel<SearchMyPromotionUiState, SearchMyPromotionAction, SearchMyPromotionEffect>(
    SearchMyPromotionUiState(),
) {

    private var debounceJob: Job? = null

    override fun handleAction(action: SearchMyPromotionAction) {
        when (action) {
            is SearchMyPromotionAction.QueryChanged -> onQueryChanged(action.keyword)
            SearchMyPromotionAction.Search -> performSearch(immediate = true)
            SearchMyPromotionAction.LoadMore -> loadMore()
            SearchMyPromotionAction.ClearKeyword -> onClearKeyword()
            SearchMyPromotionAction.Retry -> retrySearch()
        }
    }

    private fun onQueryChanged(keyword: String) {
        debounceJob?.cancel()
        val trimmedKeyword = keyword.trim()
        setState {
            copy(
                keyword = keyword
            )
        }

        when {
            trimmedKeyword.isEmpty() -> resetSearchResults()
            trimmedKeyword.length < MIN_KEYWORD_LENGTH -> showKeywordTooShort()
            else -> scheduleDebouncedSearch(trimmedKeyword)
        }
    }

    private fun performSearch(immediate: Boolean) {
        debounceJob?.cancel()
        val trimmedKeyword = uiState.value.keyword.trim()
        when {
            trimmedKeyword.isEmpty() -> resetSearchResults()
            trimmedKeyword.length < MIN_KEYWORD_LENGTH -> showKeywordTooShort()
            immediate -> search(reset = true, keyword = trimmedKeyword)
            else -> scheduleDebouncedSearch(trimmedKeyword)
        }
    }

    private fun scheduleDebouncedSearch(keyword: String) {
        debounceJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            search(reset = true, keyword = keyword)
        }
    }

    private fun onClearKeyword() {
        debounceJob?.cancel()
        setState {
            copy(
                keyword = "",
                vouchers = emptyList(),
                isLoading = false,
                isLoadingMore = false,
                isEmpty = false,
                isLastPage = true,
                page = 0
            )
        }
    }

    private fun retrySearch() {
        val trimmedKeyword = uiState.value.keyword.trim()
        if (trimmedKeyword.length < MIN_KEYWORD_LENGTH) return
        search(reset = true, keyword = trimmedKeyword)
    }

    private fun loadMore() {
        val currentState = uiState.value
        val trimmedKeyword = currentState.keyword.trim()
        if (trimmedKeyword.length < MIN_KEYWORD_LENGTH) return
        if (
            currentState.isLastPage ||
            currentState.isLoadingMore ||
            currentState.isLoading
        ) {
            return
        }
        search(reset = false, keyword = trimmedKeyword)
    }

    private fun resetSearchResults() {
        debounceJob?.cancel()
        setState {
            copy(
                vouchers = emptyList(),
                isLoading = false,
                isLoadingMore = false,
                isEmpty = false,
                isLastPage = true,
                page = 0
            )
        }
    }

    private fun showKeywordTooShort() {
        debounceJob?.cancel()
        setState {
            copy(
                vouchers = emptyList(),
                isLoading = false,
                isLoadingMore = false,
                isEmpty = false,
                isLastPage = true,
                page = 0
            )
        }
    }

    private fun search(reset: Boolean, keyword: String) {
        val currentState = uiState.value
        val nextPage = if (reset) 0 else currentState.page + 1

        launch {
            setState {
                copy(
                    isLoading = reset,
                    isLoadingMore = !reset,
                    isEmpty = if (reset) false else isEmpty,
                )
            }

            val customerId = requestContextProvider.getCustomerId()
            if (customerId.isNullOrBlank()) {
                setState {
                    copy(
                        isLoading = false,
                        isLoadingMore = false,
                    )
                }
                sendEffect(SearchMyPromotionEffect.ShowError(ErrorCodes.MISSING_CUSTOMER_ID))
                return@launch
            }

            runCatching {
                searchCustomerVouchersUseCase(
                    SearchCustomerVouchersRequest(
                        customerId = customerId,
                        keyword = keyword,
                        serviceCode = null,
                        tab = TAB_ALL,
                        sectionCode = null,
                        myVouchersPage = nextPage,
                        myVouchersSize = currentState.pageSize,
                        otherVouchersPage = null,
                        otherVouchersSize = null,
                    )
                )
            }.onSuccess { response ->
                val incoming =
                    response?.myVouchers?.content.orEmpty().map { it.toMyVoucherListItem() }
                val merged = if (reset) incoming else currentState.vouchers + incoming
                val pageInfo = response?.myVouchers

                setState {
                    copy(
                        isLoading = false,
                        isLoadingMore = false,
                        vouchers = merged,
                        isEmpty = merged.isEmpty(),
                        page = pageInfo?.number ?: nextPage,
                        pageSize = pageInfo?.size ?: pageSize,
                        isLastPage = pageInfo?.last ?: true,
                    )
                }
            }.onFailure { throwable ->
                setState {
                    if (reset) {
                        copy(
                            isLoading = false,
                            isLoadingMore = false,
                            vouchers = emptyList(),
                            isEmpty = true,
                        )
                    } else {
                        copy(
                            isLoading = false,
                            isLoadingMore = false,
                        )
                    }
                }
                sendEffect(SearchMyPromotionEffect.ShowError(throwable.toErrorCode()))
            }
        }
    }

    override fun onError(throwable: Throwable) {
        setState {
            copy(
                isLoading = false,
                isLoadingMore = false,
            )
        }
        sendEffect(SearchMyPromotionEffect.ShowError(throwable.toErrorCode()))
    }

    private companion object {
        private const val TAB_ALL = "all"
        private const val DEBOUNCE_MS = 400L
        private const val MIN_KEYWORD_LENGTH = 1
    }
}
