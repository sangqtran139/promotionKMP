package com.ttcn.promotionsdk.presentation.choosepromotion

import com.ttcn.promotionsdk.di.PromotionContainer
import com.ttcn.promotionsdk.domain.exception.toErrorCode
import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.domain.model.eligible.EligibleSection
import com.ttcn.promotionsdk.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.presentation.base.PRMStore
import com.ttcn.promotionsdk.presentation.common.ExpiryWarning
import com.ttcn.promotionsdk.presentation.common.RejectedOffer
import com.ttcn.promotionsdk.presentation.base.PromotionCancellable
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
import com.ttcn.promotionsdk.config.eligibleOrderItems


/**
 * **Tầng UI-logic dùng chung** cho màn "Chọn ưu đãi" (checkout) — chạy trên cả Android & iOS.
 *
 * Gom phần **rõ ràng dùng chung**: load đầu / phân trang **2 nhóm độc lập** (`forSectionPage`) /
 * search server-side / xử lý lỗi / disable tại chỗ ưu đãi bị validate từ chối. Order-context (orderId/orderValue) đọc từ
 * [PromotionContainer.requestContextProvider] — nguồn duy nhất cho cả 2 nền tảng.
 *
 * KHÔNG gom **validate/apply** và **selection**: hai thứ này sống ở tầng khác nhau theo nền tảng
 * (Android: ViewModel; iOS: `PromotionSDKImpl`) và là UI-state native — nhưng **rule quyết định**
 * (`ValidateDiscountsResult.isValidFor/discountFor`) đã dùng chung ở domain.
 *
 * Order items đọc từ [PromotionContainer.requestContextProvider] (`getOrderItems()`) — dùng chung 2
 * nền tảng để lấy campaign theo SKU (host chưa cấp → rỗng, chỉ campaign cấp đơn).
 */
class ChoosePromotionStore(
    private val findEligibleCampaignsUseCase: FindEligibleCampaignsUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : PRMStore<ChoosePromotionState, ChoosePromotionIntent> {
    /** iOS/Swift: khởi tạo không cần truyền scope (xem `MyPromotionStore`). */
    constructor(findEligibleCampaignsUseCase: FindEligibleCampaignsUseCase) :
        this(findEligibleCampaignsUseCase, CoroutineScope(SupervisorJob() + Dispatchers.Default))

    private val _state = MutableStateFlow(ChoosePromotionState())
    override val state: StateFlow<ChoosePromotionState> = _state.asStateFlow()

    fun currentState(): ChoosePromotionState = _state.value

    fun watchState(onEach: (ChoosePromotionState) -> Unit): PromotionCancellable {
        val job = scope.launch { state.collect { onEach(it) } }
        return PromotionCancellable { job.cancel() }
    }

    fun clear() {
        scope.cancel()
    }

    private var debounceJob: Job? = null

    /** Gác [ChoosePromotionIntent.SeedOnce] — xem KDoc của intent đó. */
    private var hasSeeded = false

    override fun errorOf(state: ChoosePromotionState): String? = state.errorCode

    override val consumeErrorIntent: ChoosePromotionIntent = ChoosePromotionIntent.ConsumeError

    override fun dispatch(intent: ChoosePromotionIntent) {
        when (intent) {
            ChoosePromotionIntent.LoadInitial -> loadOffers(isRefresh = false)
            is ChoosePromotionIntent.Preload -> preload(intent.myOffers, intent.otherOffers, intent.myIsLastPage, intent.otherIsLastPage)
            ChoosePromotionIntent.Refresh -> loadOffers(isRefresh = true)
            is ChoosePromotionIntent.QueryChanged -> onQueryChanged(intent.keyword)
            ChoosePromotionIntent.Search -> { debounceJob?.cancel(); loadOffers(isRefresh = false) }
            ChoosePromotionIntent.ClearKeyword -> {
                debounceJob?.cancel()
                // `isLoading` bật ngay ở đây chứ không đợi `loadOffers` (nó chạy trong coroutine, hở
                // một khung hình là list kết quả tìm kiếm cũ loé lên trước khi shimmer che).
                _state.update { it.copy(keyword = "", isLoading = true) }
                loadOffers(isRefresh = false)
            }
            ChoosePromotionIntent.LoadMoreMyVouchers -> loadMore(EligibleSection.MY_OFFERS)
            ChoosePromotionIntent.LoadMoreOtherVouchers -> loadMore(EligibleSection.OTHER_OFFERS)
            is ChoosePromotionIntent.SetPreSelected -> _state.update { it.copy(selectedIds = intent.ids.distinct()) }
            is ChoosePromotionIntent.SeedOnce -> {
                if (!hasSeeded) {
                    hasSeeded = true
                    // Bật `isLoading` NGAY ở đây, đồng bộ, chứ không đợi `loadOffers` (nó chạy trong
                    // coroutine): hở một khung hình là màn hiện view rỗng "chưa có ưu đãi nào" trước
                    // khi shimmer kịp che. Cùng lý do với nhánh `ClearKeyword` bên trên.
                    _state.update { it.copy(selectedIds = intent.preSelectedIds.distinct(), isLoading = true) }
                    loadOffers(isRefresh = false)
                }
            }
            is ChoosePromotionIntent.ToggleSelection -> onToggleSelection(intent.id)
            ChoosePromotionIntent.SeeMoreMy -> onSeeMoreMy()
            ChoosePromotionIntent.ConsumeError -> _state.update { it.copy(errorCode = null) }
            // Chống spam nút "Áp dụng": lượt validate chạy ở `EndowStore` nên store này không tự
            // biết lúc nào bắt đầu/kết thúc — native báo. Chỉ đổi cờ, không đụng dữ liệu danh sách.
            ChoosePromotionIntent.ApplyStarted -> _state.update { it.copy(isApplying = true) }
            ChoosePromotionIntent.ApplyFinished -> _state.update { it.copy(isApplying = false) }
            is ChoosePromotionIntent.ApplyRejected -> onApplyRejected(intent.items)
            ChoosePromotionIntent.ConsumeApplyMessage -> _state.update { it.copy(applyMessage = null) }
        }
    }

    /**
     * Server từ chối [items] ở lượt "Áp dụng" → **disable tại chỗ + bỏ tick**, không gì khác.
     *
     * Bốn việc trong một lượt cập nhật, cố ý **không tách**: state phát ra giữa chừng là màn vẽ một
     * khung hình có ưu đãi đã disable mà vẫn còn tick.
     *
     * 1. Nhập id của [items] vào [ChoosePromotionState.rejectedIds] (cộng dồn, không thay thế —
     *    lượt trước từ chối ưu đãi khác thì ưu đãi đó vẫn phải đứng ở trạng thái disable).
     * 2. Map lại cả hai nhóm qua [toChooseOffer] để `isUsable` = false. **Chỉ vậy** — KHÔNG gắn nhãn
     *    trạng thái nào lên card, cũng không dải "Chưa đủ điều kiện áp dụng". Dải đó nói về điều kiện
     *    của **đơn hàng** (`findEligible` trả `usable = false` kèm `unmatchedRules`); ưu đãi bị
     *    validate từ chối là chuyện khác, và lý do đã hiện ở popup ([ChoosePromotionState.applyMessage]).
     * 3. **Bỏ tick** các ưu đãi vừa bị từ chối: giữ tick trên một card đã mờ là tự mâu thuẫn, và
     *    `canApply()` cũng sẽ chặn nút mãi vì thấy ưu đãi đang tick không dùng được.
     * 4. Mở khoá nút ([ChoosePromotionState.isApplying] = false) + đặt câu báo một-lần.
     *
     * Câu hiện lên lấy **nguyên văn của server** ([RejectedOffer.message], do
     * `ValidateDiscountsResult.reasonFor` bóc từ `data` của response) — của ưu đãi đầu tiên có lời
     * giải thích. Ở chế độ chọn đơn (mặc định) danh sách chỉ có một phần tử nên không có gì phải
     * chọn; chế độ chọn nhiều sẽ cần cách gom nhiều câu — để đó tới khi thật sự bật multi-select.
     */
    private fun onApplyRejected(items: List<RejectedOffer>) {
        if (items.isEmpty()) {
            _state.update { it.copy(isApplying = false) }
            return
        }
        _state.update { st ->
            // Chỉ giữ **id**, không giữ câu lý do: câu đó dùng đúng một lần cho popup. Giữ lại theo
            // từng item là mời gọi đem nó ra hiển thị lên card — đúng thứ vừa bỏ.
            val allRejected = (st.rejectedIds + items.map { it.objectId }).distinct()
            val lookup = allRejected.toSet()
            st.copy(
                rejectedIds = allRejected,
                myOffers = st.myOffers.map { it.source.toChooseOffer(st.expireWarningDate, lookup) },
                otherOffers = st.otherOffers.map { it.source.toChooseOffer(st.expireWarningDate, lookup) },
                selectedIds = st.selectedIds.filterNot { it in lookup },
                isApplying = false,
                applyMessage = items.firstOrNull { it.message.isNotBlank() }?.message.orEmpty(),
            )
        }
    }

    /**
     * Chọn/bỏ chọn 1 ưu đãi (rule **dùng chung** 2 nền tảng): đang chọn → bỏ; chưa chọn →
     * [ChoosePromotionState.isMultiSelection] bật thì thêm, tắt thì thay cả danh sách bằng đúng id này.
     */
    private fun onToggleSelection(id: String) {
        _state.update {
            val cur = it.selectedIds
            val next = when {
                id in cur -> cur - id
                it.isMultiSelection -> cur + id
                else -> listOf(id)
            }
            it.copy(selectedIds = next)
        }
    }

    /**
     * State-machine "Xem thêm / Thu gọn" nhóm "Ưu đãi của tôi" (**dùng chung**): còn item đã tải bị
     * ẩn → mở hết; đã mở mà còn trang → tải trang kế; đã mở & hết trang → thu gọn.
     */
    private fun onSeeMoreMy() {
        val s = _state.value
        when {
            !s.myExpanded && s.myOffers.size > COLLAPSED_MY_COUNT -> _state.update { it.copy(myExpanded = true) }
            s.myExpanded && !s.myIsLastPage -> loadMore(EligibleSection.MY_OFFERS)
            else -> _state.update { it.copy(myExpanded = false) }
        }
    }

    /** Gõ mỗi ký tự → debounce rồi reload server-side (server lọc 2 nhóm); xoá trắng → reload ngay. */
    private fun onQueryChanged(keyword: String) {
        debounceJob?.cancel()
        // Bật loading NGAY khi gõ (trước debounce) → shimmer che ngay, tránh nhấp nháy list CŨ trong
        // ~400ms chờ. Cùng cách với `SearchMyPromotionStore.onQueryChanged`.
        //
        // Khác màn Tìm kiếm ở chỗ: bên đó gõ trắng thì `resetSearchResults()` xử lý cục bộ nên
        // `isLoading = trimmed.isNotEmpty()`; bên này gõ trắng vẫn phải gọi lại API (lấy lại danh
        // sách đầy đủ) nên bật `true` cho **cả hai** nhánh, không để hở khung hình nào.
        _state.update { it.copy(keyword = keyword, isLoading = true) }
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
        // Preload = dữ liệu widget đưa sang, KHÔNG có response nên không có ngưỡng "sắp hết hạn" đi
        // kèm. `_state.value.expireWarningDate` lúc này vẫn là default null (màn này chưa gọi mạng
        // lần nào) → lùi về bản nhớ gần nhất, do `EndowStore.loadInitial` ghi lại khi nạp widget.
        // Ghi luôn vào state để `loadMore` sau đó map cùng một ngưỡng.
        val warn = _state.value.expireWarningDate ?: ExpiryWarning.lastKnownDays
        _state.update {
            it.copy(
                hasLoadedInitial = true,
                isLoading = false,
                expireWarningDate = warn,
                myOffers = my.map { o -> o.toChooseOffer(warn, it.rejectedIds.toSet()) },
                otherOffers = other.map { o -> o.toChooseOffer(warn, it.rejectedIds.toSet()) },
                myIsLastPage = myIsLastPage,
                otherIsLastPage = otherIsLastPage,
                isEmpty = my.isEmpty() && other.isEmpty(),
            )
        }
    }

    private fun loadOffers(isRefresh: Boolean) {
        scope.launch {
            _state.update {
                it.copy(
                    isLoading = !isRefresh, isRefreshing = isRefresh,
                    isLoadingMore = false, isLoadingMoreOther = false,
                    loadFailed = false,   // lượt mới → xoá dấu hỏng của lượt trước
                )
            }
            runCatching { findEligibleCampaignsUseCase(buildRequest(section = null)) }
                .onSuccess { result ->
                    val warn = result?.expireWarningDate
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            loadFailed = false,
                            tabs = result?.tabs.orEmpty().map { t -> t.toMyPromotionTab() }.sortedBy { t -> t.order },
                            selectedTabCode = result?.activeTab,
                            myPage = 0,
                            otherPage = 0,
                            myIsLastPage = result?.myIsLastPage ?: true,
                            otherIsLastPage = result?.otherIsLastPage ?: true,
                            expireWarningDate = warn,
                            // Danh sách mới từ server vẫn phải đi qua `rejectedIds`: server đánh
                            // `usable = true` cho ưu đãi nó vừa từ chối lúc validate (hai API khác
                            // nhau), nên không áp lại override thì kéo-để-tải-lại là ưu đãi hỏng sáng
                            // lên chọn được.
                            myOffers = result?.myOffers.orEmpty().map { o -> o.toChooseOffer(warn, it.rejectedIds.toSet()) },
                            otherOffers = result?.otherOffers.orEmpty().map { o -> o.toChooseOffer(warn, it.rejectedIds.toSet()) },
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
                            // `loadFailed` bền để giữ view rỗng; `errorCode` một-lần để bắn popup.
                            isEmpty = true, hasLoadedInitial = true, loadFailed = true,
                            errorCode = throwable.toErrorCode(),
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
                                myOffers = it.myOffers + result?.myOffers.orEmpty().map { o -> o.toChooseOffer(warn, it.rejectedIds.toSet()) },
                            )
                        } else {
                            it.copy(
                                isLoadingMoreOther = false, otherPage = nextPage,
                                otherIsLastPage = result?.otherIsLastPage ?: true,
                                otherOffers = it.otherOffers + result?.otherOffers.orEmpty().map { o -> o.toChooseOffer(warn, it.rejectedIds.toSet()) },
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
            // `eligibleOrderItems()` chứ không phải `getOrderItems()`: nó đổ `serviceCode`
            // (từ `updateOrderInfo`) vào `productId` của từng item — đường duy nhất mà spec
            // findEligible v19 nhận chiều dịch vụ. Widget dùng cùng hàm này.
            items = ctx.eligibleOrderItems(),
            // `metaData` (từ `updateOrderInfo`) — chưa có field riêng ở findEligible nên đổ vào
            // orderInfo.metadata (free map) giống customerType/segment/tier.
            orderMetadata = ctx.getMetaData()?.let { mapOf("metaData" to it) },
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

/** Chờ gõ xong mới gọi API tìm kiếm — hằng nội bộ của điều phối, không thuộc contract. */
private const val DEBOUNCE_MS = 400L
