package com.ttcn.promotionsdk.presentation.choosepromotion

import com.ttcn.promotionsdk.core.di.PromotionContainer
import com.ttcn.promotionsdk.core.domain.exception.toErrorCode
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleSection
import com.ttcn.promotionsdk.core.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.core.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.core.util.daysUntil
import com.ttcn.promotionsdk.presentation.PromotionCancellable
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionTab
import com.ttcn.promotionsdk.presentation.mypromotion.toMyPromotionTab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * **Tầng UI-logic dùng chung** cho màn "Chọn ưu đãi" (checkout) — chạy trên cả Android & iOS.
 *
 * Gom phần **rõ ràng dùng chung**: load đầu / preload từ widget / phân trang **2 nhóm độc lập**
 * (`forSectionPage`) / search server-side / xử lý lỗi. Order-context (orderId/orderValue) đọc từ
 * [PromotionContainer.requestContextProvider] — nguồn duy nhất cho cả 2 nền tảng.
 *
 * KHÔNG gom **validate/apply** và **selection**: hai thứ này sống ở tầng khác nhau theo nền tảng
 * (Android: ViewModel; iOS: `PromotionSDKImpl`) và là UI-state native — nhưng **rule quyết định**
 * (`ValidateDiscountsResult.isValidFor/discountFor`) đã dùng chung ở domain.
 *
 * TODO(order-items): request tạm `items = emptyList()` (khớp trạng thái hiện tại của Android). Khi
 * provider có `getOrderItems()`, đọc vào đây để lấy campaign theo SKU (đồng bộ 2 nền tảng).
 */
class ChoosePromotionStore(
    private val findEligibleCampaignsUseCase: FindEligibleCampaignsUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    /** iOS/Swift: khởi tạo không cần truyền scope (xem `MyPromotionStore`). */
    constructor(findEligibleCampaignsUseCase: FindEligibleCampaignsUseCase) :
        this(findEligibleCampaignsUseCase, CoroutineScope(SupervisorJob() + Dispatchers.Default))

    private val _state = MutableStateFlow(ChoosePromotionState())
    val state: StateFlow<ChoosePromotionState> = _state.asStateFlow()

    fun currentState(): ChoosePromotionState = _state.value

    fun watchState(onEach: (ChoosePromotionState) -> Unit): PromotionCancellable {
        val job = scope.launch { state.collect { onEach(it) } }
        return PromotionCancellable { job.cancel() }
    }

    fun clear() {
        scope.cancel()
    }

    private var debounceJob: Job? = null

    fun dispatch(intent: ChoosePromotionIntent) {
        when (intent) {
            ChoosePromotionIntent.LoadInitial -> loadOffers(isRefresh = false)
            is ChoosePromotionIntent.Preload -> preload(intent.myOffers, intent.otherOffers, intent.myIsLastPage, intent.otherIsLastPage)
            ChoosePromotionIntent.Refresh -> loadOffers(isRefresh = true)
            is ChoosePromotionIntent.QueryChanged -> onQueryChanged(intent.keyword)
            ChoosePromotionIntent.Search -> { debounceJob?.cancel(); loadOffers(isRefresh = false) }
            ChoosePromotionIntent.ClearKeyword -> {
                debounceJob?.cancel()
                _state.update { it.copy(keyword = "") }
                loadOffers(isRefresh = false)
            }
            ChoosePromotionIntent.LoadMoreMyVouchers -> loadMore(EligibleSection.MY_OFFERS)
            ChoosePromotionIntent.LoadMoreOtherVouchers -> loadMore(EligibleSection.OTHER_OFFERS)
            ChoosePromotionIntent.ConsumeError -> _state.update { it.copy(errorCode = null) }
        }
    }

    /** Gõ mỗi ký tự → debounce rồi reload server-side (server lọc 2 nhóm); xoá trắng → reload ngay. */
    private fun onQueryChanged(keyword: String) {
        debounceJob?.cancel()
        _state.update { it.copy(keyword = keyword) }
        if (keyword.trim().isEmpty()) {
            loadOffers(isRefresh = false)
        } else {
            debounceJob = scope.launch {
                kotlinx.coroutines.delay(DEBOUNCE_MS)
                loadOffers(isRefresh = false)
            }
        }
    }

    private fun preload(my: List<EligibleOffer>, other: List<EligibleOffer>, myIsLastPage: Boolean, otherIsLastPage: Boolean) {
        if (my.isEmpty() && other.isEmpty()) {
            loadOffers(isRefresh = false)
            return
        }
        val warn = _state.value.expireWarningDate
        _state.update {
            it.copy(
                hasLoadedInitial = true,
                isLoading = false,
                myOffers = my.map { o -> o.toChooseOffer(warn) },
                otherOffers = other.map { o -> o.toChooseOffer(warn) },
                myIsLastPage = myIsLastPage,
                otherIsLastPage = otherIsLastPage,
                isEmpty = my.isEmpty() && other.isEmpty(),
            )
        }
    }

    private fun loadOffers(isRefresh: Boolean) {
        scope.launch {
            _state.update {
                it.copy(isLoading = !isRefresh, isRefreshing = isRefresh, isLoadingMore = false, isLoadingMoreOther = false)
            }
            runCatching { findEligibleCampaignsUseCase(buildRequest(section = null)) }
                .onSuccess { result ->
                    val warn = result?.expireWarningDate
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            tabs = result?.tabs.orEmpty().map { t -> t.toMyPromotionTab() }.sortedBy { t -> t.order },
                            selectedTabCode = result?.activeTab,
                            myPage = 0,
                            otherPage = 0,
                            myIsLastPage = result?.myIsLastPage ?: true,
                            otherIsLastPage = result?.otherIsLastPage ?: true,
                            expireWarningDate = warn,
                            myOffers = result?.myOffers.orEmpty().map { o -> o.toChooseOffer(warn) },
                            otherOffers = result?.otherOffers.orEmpty().map { o -> o.toChooseOffer(warn) },
                            isEmpty = result?.myOffers.orEmpty().isEmpty() && result?.otherOffers.orEmpty().isEmpty(),
                            hasLoadedInitial = true,
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false, isRefreshing = false,
                            myOffers = emptyList(), otherOffers = emptyList(),
                            isEmpty = true, hasLoadedInitial = true, errorCode = throwable.toErrorCode(),
                        )
                    }
                }
        }
    }

    /** Hai nhóm phân trang **độc lập**: chỉ nhóm được yêu cầu mới có dữ liệu trong response. */
    private fun loadMore(section: EligibleSection) {
        val s = _state.value
        if (s.isLoading) return
        val isMine = section == EligibleSection.MY_OFFERS
        if (isMine && (s.myIsLastPage || s.isLoadingMore)) return
        if (!isMine && (s.otherIsLastPage || s.isLoadingMoreOther)) return

        scope.launch {
            _state.update { if (isMine) it.copy(isLoadingMore = true) else it.copy(isLoadingMoreOther = true) }
            val nextPage = if (isMine) s.myPage + 1 else s.otherPage + 1
            runCatching { findEligibleCampaignsUseCase(buildRequest(section, nextPage)) }
                .onSuccess { result ->
                    val warn = _state.value.expireWarningDate
                    _state.update {
                        if (isMine) {
                            it.copy(
                                isLoadingMore = false, myPage = nextPage,
                                myIsLastPage = result?.myIsLastPage ?: true,
                                myOffers = it.myOffers + result?.myOffers.orEmpty().map { o -> o.toChooseOffer(warn) },
                            )
                        } else {
                            it.copy(
                                isLoadingMoreOther = false, otherPage = nextPage,
                                otherIsLastPage = result?.otherIsLastPage ?: true,
                                otherOffers = it.otherOffers + result?.otherOffers.orEmpty().map { o -> o.toChooseOffer(warn) },
                            )
                        }
                    }
                }
                .onFailure { throwable ->
                    _state.update { it.copy(isLoadingMore = false, isLoadingMoreOther = false, errorCode = throwable.toErrorCode()) }
                }
        }
    }

    /** [section] null → cả hai nhóm từ trang 0. Rule phân trang độc lập ở domain (`forSectionPage`). */
    private fun buildRequest(section: EligibleSection?, nextPage: Int = 0): FindEligibleCampaignsRequest {
        val s = _state.value
        val ctx = PromotionContainer.requestContextProvider
        return FindEligibleCampaignsRequest(
            orderId = ctx.getOrderId().orEmpty(),
            orderValue = ctx.getOrderValue().orEmpty(),
            items = emptyList(), // TODO(order-items): xem doc lớp.
            tabCode = null,
            keyword = s.keyword.takeIf { it.isNotBlank() },
            mySize = s.mySize,
            otherSize = s.otherSize,
        ).forSectionPage(
            section = section,
            nextPage = nextPage,
            currentMyPage = s.myPage,
            currentOtherPage = s.otherPage,
        )
    }
}

// ─── State / Intent / Models ──────────────────────────────────────────────────

data class ChoosePromotionState(
    val hasLoadedInitial: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isLoadingMoreOther: Boolean = false,
    val isEmpty: Boolean = false,
    val tabs: List<MyPromotionTab> = emptyList(),
    val selectedTabCode: String? = null,
    val keyword: String = "",
    val myPage: Int = 0,
    val mySize: Int = 10,
    val myIsLastPage: Boolean = false,
    val otherPage: Int = 0,
    val otherSize: Int = 10,
    val otherIsLastPage: Boolean = true,
    val myOffers: List<ChooseOffer> = emptyList(),
    val otherOffers: List<ChooseOffer> = emptyList(),
    val expireWarningDate: Int? = null,
    val errorCode: String? = null,
)

sealed interface ChoosePromotionIntent {
    data object LoadInitial : ChoosePromotionIntent
    data class Preload(
        val myOffers: List<EligibleOffer>,
        val otherOffers: List<EligibleOffer>,
        val myIsLastPage: Boolean,
        val otherIsLastPage: Boolean,
    ) : ChoosePromotionIntent
    data object Refresh : ChoosePromotionIntent
    data class QueryChanged(val keyword: String) : ChoosePromotionIntent
    data object Search : ChoosePromotionIntent
    data object ClearKeyword : ChoosePromotionIntent
    data object LoadMoreMyVouchers : ChoosePromotionIntent
    data object LoadMoreOtherVouchers : ChoosePromotionIntent
    data object ConsumeError : ChoosePromotionIntent
}

private const val DEBOUNCE_MS = 400L

/**
 * View-model 1 ưu đãi eligible: **bọc** domain [EligibleOffer] ([source]) + quyết định hiển thị đã tính.
 * Native format chuỗi ("Giảm X đ" từ `source.estimatedDiscount`, "Còn X ngày" từ [expiringInDays]).
 */
data class ChooseOffer(
    val source: EligibleOffer,
    val isUsable: Boolean,
    val expiringInDays: Int?,
)

internal fun EligibleOffer.toChooseOffer(expireWarningDate: Int?): ChooseOffer {
    val days = if (usable && expireWarningDate != null) {
        daysUntil(expireDate)?.takeIf { it in 0..expireWarningDate }
    } else null
    return ChooseOffer(source = this, isUsable = usable, expiringInDays = days)
}
