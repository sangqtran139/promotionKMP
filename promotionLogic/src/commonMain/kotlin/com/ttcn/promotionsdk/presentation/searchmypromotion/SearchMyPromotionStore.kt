package com.ttcn.promotionsdk.presentation.searchmypromotion

import com.ttcn.promotionsdk.domain.exception.toErrorCode
import com.ttcn.promotionsdk.domain.model.voucher.SearchCustomerVouchersRequest
import com.ttcn.promotionsdk.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.presentation.PromotionCancellable
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionVoucher
import com.ttcn.promotionsdk.presentation.mypromotion.toMyPromotionVoucher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * **Tầng UI-logic dùng chung** cho màn "Tìm ưu đãi của tôi" — chạy trên cả Android & iOS.
 *
 * Gom: debounce gõ phím, search server-side, phân trang, xử lý lỗi. Tái dùng [MyPromotionVoucher]
 * (voucher + quyết định hiển thị) của màn "Ưu đãi của tôi" — không tạo model trùng.
 * Cùng khuôn với `MyPromotionStore`: `state` / `dispatch` / `watchState` / `currentState` / `clear`.
 */
class SearchMyPromotionStore(
    private val searchCustomerVouchersUseCase: SearchCustomerVouchersUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    /** iOS/Swift: khởi tạo không cần truyền scope (xem `MyPromotionStore`). */
    constructor(searchCustomerVouchersUseCase: SearchCustomerVouchersUseCase) :
        this(searchCustomerVouchersUseCase, CoroutineScope(SupervisorJob() + Dispatchers.Default))

    private val _state = MutableStateFlow(SearchMyPromotionState())
    val state: StateFlow<SearchMyPromotionState> = _state.asStateFlow()

    fun currentState(): SearchMyPromotionState = _state.value

    fun watchState(onEach: (SearchMyPromotionState) -> Unit): PromotionCancellable {
        val job = scope.launch { state.collect { onEach(it) } }
        return PromotionCancellable { job.cancel() }
    }

    fun clear() {
        scope.cancel()
    }

    private var debounceJob: Job? = null

    fun dispatch(intent: SearchMyPromotionIntent) {
        when (intent) {
            is SearchMyPromotionIntent.QueryChanged -> onQueryChanged(intent.keyword)
            SearchMyPromotionIntent.Search -> performSearch(immediate = true)
            SearchMyPromotionIntent.LoadMore -> loadMore()
            SearchMyPromotionIntent.ClearKeyword -> onClearKeyword()
            SearchMyPromotionIntent.Retry -> retrySearch()
            SearchMyPromotionIntent.ConsumeError -> _state.update { it.copy(errorCode = null) }
        }
    }

    private fun onQueryChanged(keyword: String) {
        debounceJob?.cancel()
        val trimmed = keyword.trim()
        // Bật loading NGAY khi gõ (trước debounce) → shimmer che ngay, tránh nhấp nháy "không kết quả"
        // trong ~400ms chờ. Dùng chung 2 nền tảng.
        _state.update { it.copy(keyword = keyword, isLoading = trimmed.isNotEmpty()) }
        if (trimmed.isEmpty()) resetSearchResults() else scheduleDebouncedSearch(trimmed)
    }

    private fun performSearch(immediate: Boolean) {
        debounceJob?.cancel()
        val trimmed = _state.value.keyword.trim()
        when {
            trimmed.isEmpty() -> resetSearchResults()
            immediate -> search(reset = true, keyword = trimmed)
            else -> scheduleDebouncedSearch(trimmed)
        }
    }

    private fun scheduleDebouncedSearch(keyword: String) {
        debounceJob = scope.launch {
            delay(DEBOUNCE_MS)
            search(reset = true, keyword = keyword)
        }
    }

    private fun onClearKeyword() {
        debounceJob?.cancel()
        _state.update {
            it.copy(
                keyword = "", vouchers = emptyList(), isLoading = false,
                isLoadingMore = false, isEmpty = false, isLastPage = true, page = 0,
            )
        }
    }

    private fun retrySearch() {
        val trimmed = _state.value.keyword.trim()
        if (trimmed.isEmpty()) return
        search(reset = true, keyword = trimmed)
    }

    private fun loadMore() {
        val s = _state.value
        if (s.keyword.trim().isEmpty()) return
        if (s.isLastPage || s.isLoadingMore || s.isLoading) return
        search(reset = false, keyword = s.keyword.trim())
    }

    private fun resetSearchResults() {
        debounceJob?.cancel()
        _state.update {
            it.copy(
                vouchers = emptyList(), isLoading = false, isLoadingMore = false,
                isEmpty = false, isLastPage = true, page = 0,
            )
        }
    }

    private fun search(reset: Boolean, keyword: String) {
        val current = _state.value
        val nextPage = if (reset) 0 else current.page + 1

        scope.launch {
            _state.update {
                it.copy(isLoading = reset, isLoadingMore = !reset, isEmpty = if (reset) false else it.isEmpty)
            }

            runCatching {
                searchCustomerVouchersUseCase(
                    SearchCustomerVouchersRequest(
                        keyword = keyword,
                        serviceCode = null,
                        tab = TAB_ALL,
                        page = nextPage,
                        size = current.pageSize,
                    )
                )
            }.onSuccess { response ->
                val incoming = response?.content.orEmpty()
                    .map { it.toMyPromotionVoucher(response?.expireWarningDate) }
                val merged = if (reset) incoming else current.vouchers + incoming
                _state.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        vouchers = merged,
                        isEmpty = merged.isEmpty(),
                        page = response?.number ?: nextPage,
                        pageSize = response?.size ?: it.pageSize,
                        isLastPage = response?.last ?: true,
                    )
                }
            }.onFailure { throwable ->
                _state.update {
                    if (reset) {
                        it.copy(isLoading = false, isLoadingMore = false, vouchers = emptyList(), isEmpty = true, errorCode = throwable.toErrorCode())
                    } else {
                        it.copy(isLoading = false, isLoadingMore = false, errorCode = throwable.toErrorCode())
                    }
                }
            }
        }
    }

    private companion object {
        private const val TAB_ALL = "all"
        private const val DEBOUNCE_MS = 400L
    }
}

// ─── State / Intent (cấu trúc, không chuỗi hiển thị) ──────────────────────────

data class SearchMyPromotionState(
    val keyword: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isEmpty: Boolean = false,
    val isLastPage: Boolean = true,
    val page: Int = 0,
    val pageSize: Int = 10,
    val vouchers: List<MyPromotionVoucher> = emptyList(),
    val errorCode: String? = null,
)

sealed interface SearchMyPromotionIntent {
    data class QueryChanged(val keyword: String) : SearchMyPromotionIntent
    data object Search : SearchMyPromotionIntent
    data object LoadMore : SearchMyPromotionIntent
    data object ClearKeyword : SearchMyPromotionIntent
    data object Retry : SearchMyPromotionIntent
    data object ConsumeError : SearchMyPromotionIntent
}
