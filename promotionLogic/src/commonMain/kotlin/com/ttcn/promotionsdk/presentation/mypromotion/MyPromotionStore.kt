package com.ttcn.promotionsdk.presentation.mypromotion

import com.ttcn.promotionsdk.core.domain.exception.toErrorCode
import com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersRequest
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherItem
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherDisplayState
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherStatus
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherTabItem
import com.ttcn.promotionsdk.core.domain.model.voucher.displayState
import com.ttcn.promotionsdk.core.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.core.util.daysUntil
import com.ttcn.promotionsdk.presentation.PromotionCancellable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * **Tầng UI-logic dùng chung** cho màn "Ưu đãi của tôi" — chạy trên cả Android & iOS.
 *
 * Toàn bộ nghiệp-vụ-trình-bày (tab, phân trang, cache theo tab, latest-wins, orchestration search,
 * gộp danh sách, xử lý lỗi) viết **một lần** ở đây. Hai nền tảng chỉ còn:
 *  1. quan sát [state] ([StateFlow]) — Android collect trực tiếp, iOS bridge qua SKIE,
 *  2. đổi [MyPromotionState] → chuỗi hiển thị bằng tài nguyên native (R.string / Swift),
 *  3. điều hướng (mở chi tiết, bottom sheet dịch vụ) — không nằm ở store.
 *
 * **Không chứa chuỗi hiển thị** (giữ đúng rule của lõi): state chỉ mang dữ liệu có cấu trúc
 * (enum trạng thái, số, ngày thô, [expireWarningDate]); việc định dạng "Còn X ngày" / "HSD: …"
 * do tầng native lo (date-math phụ thuộc locale).
 *
 * Port từ `MyPromotionViewModel` (Android) — nguồn logic gốc; sau khi hai nền tảng dùng store này,
 * ViewModel hai bên trở thành lớp bọc mỏng.
 */
class MyPromotionStore(
    private val searchCustomerVouchersUseCase: SearchCustomerVouchersUseCase,
    /**
     * Scope chạy use case. Android truyền `viewModelScope` (theo lifecycle, tự huỷ — KHÔNG gọi [clear]).
     * iOS/khác không truyền → store tự sở hữu scope, và gọi [clear] khi rời màn.
     */
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {

    /**
     * iOS/Swift: khởi tạo không cần truyền `scope` (default param của Kotlin KHÔNG bridge sang Swift,
     * và tạo `CoroutineScope` từ Swift rất bất tiện). Store tự sở hữu scope; gọi [clear] khi rời màn.
     */
    constructor(searchCustomerVouchersUseCase: SearchCustomerVouchersUseCase) :
        this(searchCustomerVouchersUseCase, CoroutineScope(SupervisorJob() + Dispatchers.Default))

    private val _state = MutableStateFlow(MyPromotionState())

    /** Android collect trực tiếp + đọc `.value` đồng bộ. */
    val state: StateFlow<MyPromotionState> = _state.asStateFlow()

    /** State hiện tại (đồng bộ) cho iOS đọc nhanh khi cần (vd seed trước khi observe). */
    fun currentState(): MyPromotionState = _state.value

    /**
     * Quan sát state từ iOS/Swift bằng callback (SKIE version này không bridge Flow của class
     * ObjC-exposed sang `for await`, nên dùng cầu callback thuần — bridge ObjC lambda, không thư viện).
     * [onEach] phát trên dispatcher của store; Swift tự hop về main. Trả [PromotionCancellable] để huỷ.
     */
    fun watchState(onEach: (MyPromotionState) -> Unit): PromotionCancellable {
        val job = scope.launch { state.collect { onEach(it) } }
        return PromotionCancellable { job.cancel() }
    }

    /** Huỷ scope do store tự sở hữu (iOS gọi khi deinit). KHÔNG gọi nếu đã inject scope ngoài (Android). */
    fun clear() {
        scope.cancel()
    }

    /** Cache list theo tab (RAM, sống cùng store): quay lại tab đã xem hiện ngay rồi refresh ngầm. */
    private data class TabCache(
        val vouchers: List<MyPromotionVoucher>,
        val page: Int,
        val isLastPage: Boolean,
    )

    private val tabCaches = mutableMapOf<String, TabCache>()
    private var latestTabRequestId = 0L

    fun dispatch(intent: MyPromotionIntent) {
        when (intent) {
            MyPromotionIntent.LoadInitialIfNeeded -> {
                if (!_state.value.hasLoadedInitial) {
                    loadVouchers(tabCode = null, reset = true, keyword = "", showFullLoading = true)
                }
            }

            MyPromotionIntent.Refresh -> loadVouchers(
                tabCode = _state.value.selectedTabCode,
                reset = true,
                keyword = _state.value.keyword,
                showFullLoading = false,
                isPullRefresh = true,
            )

            is MyPromotionIntent.SelectTab -> onTabSelected(intent.tabCode)

            is MyPromotionIntent.Search -> {
                val trimmed = intent.keyword.trim()
                if (trimmed.isNotEmpty()) {
                    val allTabCode = _state.value.tabs.firstOrNull { it.code == TAB_ALL }?.code ?: TAB_ALL
                    loadVouchers(tabCode = allTabCode, reset = true, keyword = trimmed, showFullLoading = true)
                }
            }

            MyPromotionIntent.LoadMore -> loadVouchers(
                tabCode = _state.value.selectedTabCode,
                reset = false,
                keyword = _state.value.keyword,
            )

            MyPromotionIntent.ConsumeError -> _state.update { it.copy(errorCode = null) }
        }
    }

    private fun onTabSelected(tabCode: String) {
        val cache = tabCaches[tabCode]
        if (cache != null) {
            _state.update {
                it.copy(
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
            loadVouchers(tabCode, reset = true, keyword = _state.value.keyword, isRefreshTab = true)
            return
        }
        _state.update {
            it.copy(
                selectedTabCode = tabCode,
                isEmpty = false,
                isLoadingMore = false,
                isRefreshingTab = false,
                isLoading = true,
            )
        }
        loadVouchers(tabCode, reset = true, keyword = _state.value.keyword, keepCurrentListWhileLoading = true)
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
        val current = _state.value
        if (!reset && (
                current.isLastPage || current.isLoadingMore || current.isLoading ||
                    current.isRefreshing || current.isRefreshingTab || current.vouchers.isEmpty()
                )
        ) return

        val requestTabCode = tabCode ?: current.selectedTabCode
        val nextPage = if (reset) 0 else current.page + 1
        val requestId = if (reset) ++latestTabRequestId else latestTabRequestId

        scope.launch {
            if (reset) {
                _state.update {
                    it.copy(
                        isLoading = showFullLoading || keepCurrentListWhileLoading,
                        isRefreshing = isPullRefresh,
                        isRefreshingTab = isRefreshTab,
                        isLoadingMore = false,
                        keyword = keyword,
                        selectedTabCode = tabCode ?: it.selectedTabCode,
                        vouchers = when {
                            keepCurrentListWhileLoading -> it.vouchers
                            isRefreshTab || isPullRefresh -> it.vouchers
                            showFullLoading -> emptyList()
                            else -> it.vouchers
                        },
                    )
                }
            } else {
                _state.update { it.copy(isLoadingMore = true, keyword = keyword) }
            }

            runCatching {
                searchCustomerVouchersUseCase(
                    SearchCustomerVouchersRequest(
                        keyword = keyword.takeIf { it.isNotBlank() },
                        serviceCode = null,
                        tab = requestTabCode,
                        page = nextPage,
                        size = current.size,
                    )
                )
            }.onSuccess { response ->
                if (!shouldApplyResponse(requestId, requestTabCode, reset)) return@onSuccess

                val incomingTabs = response?.tabs
                    ?.map { it.toMyPromotionTab() }
                    ?.sortedBy { it.order }
                    .orEmpty()
                val tabs = incomingTabs.takeIf { it.isNotEmpty() } ?: _state.value.tabs
                // Quy tắc chọn tab active dùng chung (domain): resolveActiveTab.
                val selected = response?.resolveActiveTab(requestTabCode)
                    ?: requestTabCode
                    ?: tabs.firstOrNull()?.code
                val incoming = response?.content.orEmpty()
                    .map { it.toMyPromotionVoucher(response?.expireWarningDate) }
                val resolvedPage = response?.number ?: nextPage
                val resolvedIsLastPage = response?.last ?: true

                val merged = if (reset) incoming.toList() else (_state.value.vouchers + incoming).toList()

                val cacheTabCode = requestTabCode ?: selected
                if (!cacheTabCode.isNullOrBlank()) {
                    tabCaches[cacheTabCode] = TabCache(merged.toList(), resolvedPage, resolvedIsLastPage)
                }

                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isRefreshingTab = false,
                        isLoadingMore = false,
                        isEmpty = merged.isEmpty(),
                        tabs = tabs,
                        selectedTabCode = selected,
                        vouchers = merged.toList(),
                        page = resolvedPage,
                        size = response?.size ?: current.size,
                        isLastPage = resolvedIsLastPage,
                        hasLoadedInitial = true,
                    )
                }
            }.onFailure { throwable ->
                if (!shouldApplyResponse(requestId, requestTabCode, reset)) return@onFailure

                val errorCode = throwable.toErrorCode()
                val hasCache = requestTabCode?.let { tabCaches[it] } != null
                _state.update {
                    when {
                        reset && hasCache -> it.copy(
                            isLoading = false, isRefreshing = false, isRefreshingTab = false,
                            isLoadingMore = false, hasLoadedInitial = true, errorCode = errorCode,
                        )

                        reset -> it.copy(
                            isLoading = false, isRefreshing = false, isRefreshingTab = false,
                            isLoadingMore = false,
                            isEmpty = it.vouchers.isEmpty(),
                            hasLoadedInitial = true, errorCode = errorCode,
                        )

                        else -> it.copy(
                            isLoading = false, isRefreshing = false, isRefreshingTab = false,
                            isLoadingMore = false, errorCode = errorCode,
                        )
                    }
                }
            }
        }
    }

    /**
     * Latest-wins + chống đè nhầm tab: bỏ response nếu (1) có request mới hơn, (2) tab đã đổi,
     * (3) load-more trong lúc đang refresh tab.
     */
    private fun shouldApplyResponse(requestId: Long, requestTabCode: String?, reset: Boolean): Boolean {
        if (requestId != latestTabRequestId) return false
        if (requestTabCode != null && _state.value.selectedTabCode != requestTabCode) return false
        if (!reset && _state.value.isRefreshingTab) return false
        return true
    }

    private companion object {
        private const val TAB_ALL = "all"
    }
}

// ─── State / Intent / Models (cấu trúc, KHÔNG chuỗi hiển thị) ──────────────────

data class MyPromotionState(
    val hasLoadedInitial: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isRefreshingTab: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isEmpty: Boolean = false,
    val tabs: List<MyPromotionTab> = emptyList(),
    val selectedTabCode: String? = null,
    val keyword: String = "",
    val page: Int = 0,
    val size: Int = 10,
    val isLastPage: Boolean = true,
    val vouchers: List<MyPromotionVoucher> = emptyList(),
    /** Mã lỗi một-lần; native hiển thị rồi `dispatch(ConsumeError)` để xoá. */
    val errorCode: String? = null,
)

/** Nhãn trạng thái đã QUYẾT ĐỊNH ở store — native chỉ tra chuỗi tương ứng, không tự suy. */
enum class MyPromotionBadge {
    /** Không hiện badge (voucher dùng được, chưa sắp hết hạn). */
    NONE,
    /** Sắp hết hạn — native hiển thị "Còn {expiringInDays} ngày". */
    EXPIRING_SOON,
    USED,
    EXPIRED,
    INELIGIBLE,
}

/** Nút thao tác đã QUYẾT ĐỊNH ở store — native map sang chuỗi + hiện/ẩn. */
enum class MyPromotionAction {
    /** Hiện nút "Sử dụng". */
    USE,
    /** Không hiện nút. */
    NONE,
}

sealed interface MyPromotionIntent {
    data object LoadInitialIfNeeded : MyPromotionIntent
    data object Refresh : MyPromotionIntent
    data class SelectTab(val tabCode: String) : MyPromotionIntent
    data class Search(val keyword: String) : MyPromotionIntent
    data object LoadMore : MyPromotionIntent
    data object ConsumeError : MyPromotionIntent
}

data class MyPromotionTab(
    val code: String,
    val label: String,
    val count: Int,
    val order: Int,
    val isDefault: Boolean = false,
)

/**
 * View-model cho 1 voucher: **bọc** domain [VoucherItem] ([source]) + các **quyết định hiển thị đã tính**
 * ([isEnabled]/[expiringInDays]/[badge]/[action]). Không chép lại field của domain (tránh trùng model),
 * không chứa chuỗi hiển thị — native đọc `source.*` cho dữ liệu thô và enum/số cho phần đã quyết định.
 */
data class MyPromotionVoucher(
    /** Dữ liệu thô tái dùng từ domain (merchantName/title/logo/expirationDate/status...). */
    val source: VoucherItem,
    // ── Quyết định hiển thị (store tính, native chỉ dùng) ──
    /** Voucher còn dùng được → mở màn/áp; false → hiển thị mờ, không cho thao tác. */
    val isEnabled: Boolean,
    /** Số ngày còn lại khi sắp hết hạn (khi [badge] == EXPIRING_SOON); null nếu không áp dụng. */
    val expiringInDays: Int?,
    val badge: MyPromotionBadge,
    val action: MyPromotionAction,
)

/** [expireWarningDate]: ngưỡng cảnh báo (ngày) từ server — quyết định badge "sắp hết hạn". */
internal fun VoucherItem.toMyPromotionVoucher(expireWarningDate: Int?): MyPromotionVoucher {
    val display = VoucherStatus.from(status).displayState()
    val enabled = display.isUsable
    // "Còn X ngày" chỉ khi còn dùng được và trong ngưỡng [0, expireWarningDate].
    val days = if (enabled && expireWarningDate != null) {
        daysUntil(expirationDate)?.takeIf { it in 0..expireWarningDate }
    } else null
    val badge = when {
        !enabled -> when (display) {
            VoucherDisplayState.USED -> MyPromotionBadge.USED
            VoucherDisplayState.EXPIRED -> MyPromotionBadge.EXPIRED
            else -> MyPromotionBadge.INELIGIBLE
        }
        days != null -> MyPromotionBadge.EXPIRING_SOON
        else -> MyPromotionBadge.NONE
    }
    return MyPromotionVoucher(
        source = this,
        isEnabled = enabled,
        expiringInDays = days,
        badge = badge,
        action = if (enabled) MyPromotionAction.USE else MyPromotionAction.NONE,
    )
}

internal fun VoucherTabItem.toMyPromotionTab() = MyPromotionTab(
    code = code,
    label = label,
    count = count ?: 0,
    order = order ?: Int.MAX_VALUE,
    isDefault = isDefault,
)
