package com.ttcn.promotionsdk.presentation.mypromotion

import com.ttcn.promotionsdk.domain.exception.toErrorCode
import com.ttcn.promotionsdk.domain.model.voucher.SearchCustomerVouchersRequest
import com.ttcn.promotionsdk.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.common.currentEpochMillis
import com.ttcn.promotionsdk.presentation.base.PRMStore
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
public class MyPromotionStore(
    private val searchCustomerVouchersUseCase: SearchCustomerVouchersUseCase,
    /**
     * Scope chạy use case. Android truyền `viewModelScope` (theo lifecycle, tự huỷ — KHÔNG gọi [clear]).
     * iOS/khác không truyền → store tự sở hữu scope, và gọi [clear] khi rời màn.
     */
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : PRMStore<MyPromotionState, MyPromotionIntent> {

    /**
     * iOS/Swift: khởi tạo không cần truyền `scope` (default param của Kotlin KHÔNG bridge sang Swift,
     * và tạo `CoroutineScope` từ Swift rất bất tiện). Store tự sở hữu scope; gọi [clear] khi rời màn.
     */
    public constructor(searchCustomerVouchersUseCase: SearchCustomerVouchersUseCase) :
        this(searchCustomerVouchersUseCase, CoroutineScope(SupervisorJob() + Dispatchers.Default))

    private val _state = MutableStateFlow(MyPromotionState())

    /** Android collect trực tiếp + đọc `.value` đồng bộ. */
    override val state: StateFlow<MyPromotionState> = _state.asStateFlow()

    /** State hiện tại (đồng bộ) cho iOS đọc nhanh khi cần (vd seed trước khi observe). */
    public fun currentState(): MyPromotionState = _state.value

    /**
     * Quan sát state từ iOS/Swift bằng callback (SKIE version này không bridge Flow của class
     * ObjC-exposed sang `for await`, nên dùng cầu callback thuần — bridge ObjC lambda, không thư viện).
     * [onEach] phát trên dispatcher của store; Swift tự hop về main. Trả [PromotionCancellable] để huỷ.
     */
    public fun watchState(onEach: (MyPromotionState) -> Unit): PromotionCancellable {
        val job = scope.launch { state.collect { onEach(it) } }
        return PromotionCancellable { job.cancel() }
    }

    /** Huỷ scope do store tự sở hữu (iOS gọi khi deinit). KHÔNG gọi nếu đã inject scope ngoài (Android). */
    public fun clear() {
        scope.cancel()
    }

    /**
     * Cache list theo tab (RAM, sống cùng store).
     *
     * Còn tươi (< [TAB_CACHE_TTL_MS]) → đổi tab dùng thẳng, **không gọi API**. Ôi → vẫn hiện ngay
     * rồi refresh ngầm. Cache được lấp sẵn bởi [prefetchOtherTabs] ngay sau lần load đầu.
     */
    private data class TabCache(
        val vouchers: List<MyPromotionVoucher>,
        val page: Int,
        val isLastPage: Boolean,
        /** Mốc ghi cache (epoch millis). `0` = ép ôi (sau khi kéo làm mới). */
        val fetchedAt: Long,
    )

    private fun TabCache.isFresh(now: Long = currentEpochMillis()): Boolean =
        now - fetchedAt < TAB_CACHE_TTL_MS

    /**
     * Chỉ có nghĩa khi server trả `tabs[]`. Host không cấu hình tab → `selectedTabCode` là `null`
     * suốt vòng đời màn, map này rỗng và **không ai đọc tới** (không có tab để bấm qua lại).
     */
    private val tabCaches = mutableMapOf<String, TabCache>()

    /** Tab đang prefetch dở — chống bắn trùng khi user bấm qua lại lúc request chưa về. */
    private val prefetchingTabs = mutableSetOf<String>()

    private var latestTabRequestId = 0L

    override fun errorOf(state: MyPromotionState): String? = state.errorCode

    override val consumeErrorIntent: MyPromotionIntent = MyPromotionIntent.ConsumeError

    override fun dispatch(intent: MyPromotionIntent) {
        when (intent) {
            MyPromotionIntent.LoadInitialIfNeeded -> {
                if (!_state.value.hasLoadedInitial) {
                    loadVouchers(tabCode = null, reset = true, keyword = "", showFullLoading = true)
                }
            }

            // Kéo làm mới = làm mới CẢ MÀN, không riêng tab đang đứng: đánh dấu ôi mọi cache để tab
            // khác cũng được nạp lại. Đánh dấu ôi (không xoá) nên bấm sang tab đó vẫn hiện ngay dữ
            // liệu cũ rồi tự cập nhật, thay vì màn trắng + spinner.
            MyPromotionIntent.Refresh -> {
                markAllCachesStale()
                loadVouchers(
                    tabCode = _state.value.selectedTabCode,
                    reset = true,
                    keyword = _state.value.keyword,
                    showFullLoading = false,
                    isPullRefresh = true,
                )
            }

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
            val isFresh = cache.isFresh()
            _state.update {
                it.copy(
                    selectedTabCode = tabCode,
                    vouchers = cache.vouchers.toList(),
                    page = cache.page,
                    isLastPage = cache.isLastPage,
                    isEmpty = cache.vouchers.isEmpty(),
                    isLoading = false,
                    isRefreshing = false,
                    isRefreshingTab = !isFresh,
                    isLoadingMore = false,
                )
            }
            // Cache còn tươi → DÙNG LUÔN, không gọi API. Đây là điểm chính: tab qua tab lại trong
            // vòng TTL không tốn request nào (cache đã được prefetch lấp sẵn từ lần load đầu).
            if (isFresh) return
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
                // Client đã chỉ định tab (user vừa bấm) → tab ĐÓ thắng, không để response ghi đè.
                // `selectedTab`/`defaultTab` trong response là ý server về tab **đáp xuống**, chỉ có
                // nghĩa khi client chưa có ý kiến (lần load đầu). Server echo lệch — trả
                // `selectedTab = "all"` cho request `tab=used` — mà nghe theo thì tab sáng nhảy về
                // "Tất cả" ngay dưới ngón tay user, trong khi list hiện ra lại là của "Đã dùng".
                // Vì vậy chỉ hỏi `resolveActiveTab` khi KHÔNG có tab được yêu cầu; rule domain
                // (selectedTab → defaultTab → tab đầu theo order) giữ nguyên cho đúng vai trò đó.
                val selected = requestTabCode
                    ?: response?.resolveActiveTab()
                    ?: tabs.firstOrNull()?.code
                val incoming = response?.content.orEmpty()
                    .map { it.toMyPromotionVoucher(response?.expireWarningDate) }
                val resolvedPage = response?.number ?: nextPage
                val resolvedIsLastPage = response?.last ?: true

                val merged = if (reset) incoming.toList() else (_state.value.vouchers + incoming).toList()

                val cacheTabCode = requestTabCode ?: selected
                if (!cacheTabCode.isNullOrBlank()) {
                    tabCaches[cacheTabCode] = TabCache(
                        merged.toList(), resolvedPage, resolvedIsLastPage, currentEpochMillis(),
                    )
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

                // Đây là lúc SỚM NHẤT biết được danh sách tab (server trả động trong `tabs[]`) →
                // lấp cache cho các tab còn lại để user bấm sang là có sẵn.
                if (reset) {
                    prefetchOtherTabs(
                        tabs = tabs,
                        activeTabCode = selected,
                        keyword = keyword,
                        size = response?.size ?: current.size,
                    )
                }
            }.onFailure { throwable ->
                if (!shouldApplyResponse(requestId, requestTabCode, reset)) return@onFailure

                val errorCode = throwable.toErrorCode()
                _state.update {
                    when {
                        // Danh sách đang hiển thị KHÔNG phải của tab vừa yêu cầu: `onTabSelected`
                        // cố tình giữ tạm list tab cũ trong lúc load cho đỡ nháy
                        // (`keepCurrentListWhileLoading`). Gọi hỏng thì phải xoá, nếu không user
                        // thấy nguyên list của tab bên cạnh nằm dưới tab vừa bấm.
                        //
                        // Điều kiện là **chính cái cờ đã gây ra list lệch tab**, chứ không phải
                        // "tab này có cache không" như trước: hai chuyện đó không đồng nghĩa. Server
                        // không trả `tabs[]` thì [tabCaches] rỗng suốt → mọi lần `reset` hỏng đều rơi
                        // vào nhánh xoá, tức kéo-làm-mới ngay trên tab đang đứng mà rớt mạng là
                        // danh sách bị xoá trắng.
                        reset && keepCurrentListWhileLoading -> it.copy(
                            isLoading = false, isRefreshing = false, isRefreshingTab = false,
                            isLoadingMore = false,
                            vouchers = emptyList(),
                            page = 0,
                            isLastPage = true,
                            isEmpty = true,
                            hasLoadedInitial = true, errorCode = errorCode,
                        )

                        // Làm mới / load lại chính tab đang đứng: đã có gì thì GIỮ NGUYÊN, chỉ báo lỗi.
                        reset -> it.copy(
                            isLoading = false, isRefreshing = false, isRefreshingTab = false,
                            isLoadingMore = false, isEmpty = it.vouchers.isEmpty(),
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
     * Nạp trước trang đầu của **các tab chưa xem** rồi cất vào [tabCaches] — để đổi tab là dùng
     * ngay, không phải chờ mạng.
     *
     * **KHÔNG đi qua [loadVouchers]** một cách có chủ đích: hàm đó lọc response qua
     * [shouldApplyResponse], mà điều kiện "tab đã đổi" sẽ vứt sạch response của tab đang không được
     * chọn — đúng định nghĩa của prefetch. Đường riêng này vì thế:
     *  - không đụng `latestTabRequestId`, không set cờ loading nào → vô hình với UI;
     *  - **chỉ ghi [tabCaches]**, tuyệt đối không chạm `_state.vouchers`;
     *  - lỗi thì **nuốt im** (không ghi cache, không `errorCode`): bắn toast cho tab user chưa bấm
     *    vào là vô lý; tab đó lúc bấm sẽ load bình thường như chưa có gì.
     */
    private fun prefetchOtherTabs(
        tabs: List<MyPromotionTab>,
        activeTabCode: String?,
        keyword: String,
        size: Int,
    ) {
        // Đang tìm kiếm thì kết quả gắn với keyword ở tab "all" — prefetch tab khác vô nghĩa.
        if (keyword.isNotBlank()) return

        val now = currentEpochMillis()
        for (tab in tabs) {
            val code = tab.code
            if (code.isBlank() || code == activeTabCode) continue
            if (tabCaches[code]?.isFresh(now) == true) continue
            if (!prefetchingTabs.add(code)) continue     // request cho tab này đang bay
            launchPrefetch(code, size)
        }
    }

    private fun launchPrefetch(tabCode: String, size: Int) {
        scope.launch {
            runCatching {
                searchCustomerVouchersUseCase(
                    SearchCustomerVouchersRequest(
                        keyword = null,
                        serviceCode = null,
                        tab = tabCode,
                        page = 0,
                        size = size,
                    )
                )
            }.onSuccess { response ->
                val vouchers = response?.content.orEmpty()
                    .map { it.toMyPromotionVoucher(response?.expireWarningDate) }
                tabCaches[tabCode] = TabCache(
                    vouchers = vouchers,
                    page = response?.number ?: 0,
                    isLastPage = response?.last ?: true,
                    fetchedAt = currentEpochMillis(),
                )
            }
            prefetchingTabs -= tabCode
        }
    }

    /** Ép mọi cache thành ôi (giữ dữ liệu để hiện ngay, nhưng lần bấm tới sẽ nạp lại). */
    private fun markAllCachesStale() {
        for (code in tabCaches.keys.toList()) {
            tabCaches[code]?.let { tabCaches[code] = it.copy(fetchedAt = 0L) }
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

        /**
         * Cache tab còn "tươi" trong bao lâu. Đủ dài để tab qua tab lại không tốn request, đủ ngắn
         * để voucher vừa dùng ở luồng khác không hiện sai quá lâu.
         */
        private const val TAB_CACHE_TTL_MS = 60_000L
    }
}
