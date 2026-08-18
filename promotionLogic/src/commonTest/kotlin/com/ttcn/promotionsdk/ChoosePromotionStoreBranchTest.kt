package com.ttcn.promotionsdk

import com.ttcn.promotionsdk.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.config.PromotionSDKConfig
import com.ttcn.promotionsdk.di.PromotionContainer
import com.ttcn.promotionsdk.domain.exception.PromotionException
import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffersResult
import com.ttcn.promotionsdk.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.domain.model.redemption.CreateRedemptionResult
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsResult
import com.ttcn.promotionsdk.domain.model.voucher.SearchCustomerVouchersResult
import com.ttcn.promotionsdk.domain.model.voucher.VoucherDetail
import com.ttcn.promotionsdk.domain.repository.PromotionRepository
import com.ttcn.promotionsdk.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.presentation.choosepromotion.COLLAPSED_MY_COUNT
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionIntent
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionState
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionStore
import com.ttcn.promotionsdk.presentation.choosepromotion.ChooseSeeMoreState
import com.ttcn.promotionsdk.presentation.choosepromotion.allOffers
import com.ttcn.promotionsdk.presentation.choosepromotion.canApply
import com.ttcn.promotionsdk.presentation.choosepromotion.highlightKeyword
import com.ttcn.promotionsdk.presentation.choosepromotion.mySeeMoreState
import com.ttcn.promotionsdk.presentation.choosepromotion.selectedOffers
import com.ttcn.promotionsdk.presentation.choosepromotion.shouldLoadMoreOther
import com.ttcn.promotionsdk.presentation.choosepromotion.showsIneligibleWarning
import com.ttcn.promotionsdk.presentation.choosepromotion.showsNoResult
import com.ttcn.promotionsdk.presentation.choosepromotion.showsSelectedCount
import com.ttcn.promotionsdk.presentation.choosepromotion.visibleMyOffers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Phủ các **nhánh** của [ChoosePromotionStore] mà bộ test cũ chưa chạm: selection single/multi,
 * state-machine "Xem thêm", guard phân trang 2 nhóm độc lập, debounce search, và nhánh lỗi.
 */
class ChoosePromotionStoreBranchTest {

    private class FakeRepo(var result: (FindEligibleCampaignsRequest) -> EligibleOffersResult?) : PromotionRepository {
        var calls = 0
        var last: FindEligibleCampaignsRequest? = null
        override suspend fun searchCustomerVouchers(keyword: String?, serviceCode: String?, tab: String?, page: Int?, size: Int?): SearchCustomerVouchersResult? = null
        override suspend fun getCustomerVoucherDetail(voucherId: String, service: String?): VoucherDetail? = null
        override suspend fun createRedemptionSession(request: CreateRedemptionRequest): CreateRedemptionResult? = null
        override suspend fun validateStackableDiscounts(request: ValidateDiscountsRequest): ValidateDiscountsResult? = null
        override suspend fun findEligibleCampaigns(request: FindEligibleCampaignsRequest): EligibleOffersResult? {
            calls++; last = request; return result(request)
        }
    }

    private class Ctx : PromotionRequestContextProvider {
        override fun getOrderId(): String = "ORD-1"
        override fun getOrderValue(): String = "1000"
    }

    @BeforeTest
    fun setUp() = PromotionContainer.initialize(PromotionSDKConfig(baseUrl = "https://a.example.com", requestContextProvider = Ctx()))

    @AfterTest
    fun tearDown() = PromotionContainer.clear()

    private fun offer(id: String, usable: Boolean = true, expire: String? = null) =
        EligibleOffer(id = id, usable = usable, expireDate = expire)

    /** `ChooseOffer` tối thiểu cho các test chỉ quan tâm SỐ LƯỢNG item, không quan tâm nội dung. */
    private fun EligibleOffer.toChooseOfferForTest() =
        com.ttcn.promotionsdk.presentation.choosepromotion.ChooseOffer(
            source = this, isUsable = true, expiringInDays = null, isExpired = false,
        )

    private fun TestScopeStore(repo: FakeRepo, scheduler: kotlinx.coroutines.test.TestCoroutineScheduler) =
        ChoosePromotionStore(FindEligibleCampaignsUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(scheduler)))

    // ─── Preload ──────────────────────────────────────────────────────────────

    @Test
    fun preload_withData_usesItAndDoesNotCallApi() = runTest {
        val repo = FakeRepo { EligibleOffersResult() }
        val s = TestScopeStore(repo, testScheduler)
        s.dispatch(ChoosePromotionIntent.Preload(listOf(offer("a")), listOf(offer("b")), myIsLastPage = false, otherIsLastPage = true))
        testScheduler.advanceUntilIdle()

        assertEquals(0, repo.calls)
        val st = s.currentState()
        assertTrue(st.hasLoadedInitial)
        assertEquals(listOf("a"), st.myOffers.map { it.source.id })
        assertFalse(st.isEmpty)
    }

    @Test
    fun preload_empty_fallsBackToFetch() = runTest {
        val repo = FakeRepo { EligibleOffersResult(myOffers = listOf(offer("x"))) }
        val s = TestScopeStore(repo, testScheduler)
        s.dispatch(ChoosePromotionIntent.Preload(emptyList(), emptyList(), myIsLastPage = false, otherIsLastPage = true))
        testScheduler.advanceUntilIdle()

        assertEquals(1, repo.calls)
        assertEquals(listOf("x"), s.currentState().myOffers.map { it.source.id })
    }

    // ─── Selection: single vs multi ───────────────────────────────────────────

    @Test
    fun toggleSelection_singleMode_replacesSelection() = runTest {
        val s = TestScopeStore(FakeRepo { EligibleOffersResult() }, testScheduler)
        s.dispatch(ChoosePromotionIntent.ToggleSelection("a"))
        s.dispatch(ChoosePromotionIntent.ToggleSelection("b"))
        assertEquals(listOf("b"), s.currentState().selectedIds)
    }

    @Test
    fun toggleSelection_sameId_deselects() = runTest {
        val s = TestScopeStore(FakeRepo { EligibleOffersResult() }, testScheduler)
        s.dispatch(ChoosePromotionIntent.ToggleSelection("a"))
        s.dispatch(ChoosePromotionIntent.ToggleSelection("a"))
        assertTrue(s.currentState().selectedIds.isEmpty())
    }

    @Test
    fun setPreSelected_distinctsIds() = runTest {
        val s = TestScopeStore(FakeRepo { EligibleOffersResult() }, testScheduler)
        s.dispatch(ChoosePromotionIntent.SetPreSelected(listOf("a", "a", "b")))
        assertEquals(listOf("a", "b"), s.currentState().selectedIds)
    }

    // ─── SeeMoreMy — state machine 3 nhánh ────────────────────────────────────

    @Test
    fun seeMoreMy_expandsWhenHiddenItemsLoaded() = runTest {
        val many = (1..COLLAPSED_MY_COUNT + 2).map { offer("o$it") }
        val s = TestScopeStore(FakeRepo { EligibleOffersResult() }, testScheduler)
        s.dispatch(ChoosePromotionIntent.Preload(many, emptyList(), myIsLastPage = true, otherIsLastPage = true))
        s.dispatch(ChoosePromotionIntent.SeeMoreMy)
        assertTrue(s.currentState().myExpanded)
    }

    @Test
    fun seeMoreMy_expandedAndLastPage_collapses() = runTest {
        val many = (1..COLLAPSED_MY_COUNT + 2).map { offer("o$it") }
        val s = TestScopeStore(FakeRepo { EligibleOffersResult() }, testScheduler)
        s.dispatch(ChoosePromotionIntent.Preload(many, emptyList(), myIsLastPage = true, otherIsLastPage = true))
        s.dispatch(ChoosePromotionIntent.SeeMoreMy)   // mở
        s.dispatch(ChoosePromotionIntent.SeeMoreMy)   // đã mở + hết trang → thu gọn
        assertFalse(s.currentState().myExpanded)
    }

    @Test
    fun seeMoreMy_expandedAndMorePages_loadsNextPage() = runTest {
        val many = (1..COLLAPSED_MY_COUNT + 2).map { offer("o$it") }
        val repo = FakeRepo { EligibleOffersResult(myOffers = listOf(offer("new")), myIsLastPage = true) }
        val s = TestScopeStore(repo, testScheduler)
        s.dispatch(ChoosePromotionIntent.Preload(many, emptyList(), myIsLastPage = false, otherIsLastPage = true))
        s.dispatch(ChoosePromotionIntent.SeeMoreMy)   // mở
        s.dispatch(ChoosePromotionIntent.SeeMoreMy)   // đã mở + còn trang → tải tiếp
        testScheduler.advanceUntilIdle()

        assertEquals(1, repo.calls)
        assertTrue(s.currentState().myOffers.any { it.source.id == "new" })
    }

    // ─── Phân trang 2 nhóm độc lập + guard ────────────────────────────────────

    @Test
    fun loadMoreOther_stopsAtLastPage() = runTest {
        val repo = FakeRepo { EligibleOffersResult() }
        val s = TestScopeStore(repo, testScheduler)
        s.dispatch(ChoosePromotionIntent.Preload(emptyList(), listOf(offer("b")), myIsLastPage = true, otherIsLastPage = true))
        val before = repo.calls
        s.dispatch(ChoosePromotionIntent.LoadMoreOtherVouchers)
        testScheduler.advanceUntilIdle()
        assertEquals(before, repo.calls)
    }

    @Test
    fun loadMoreOther_fetchesNextPageAndAppends() = runTest {
        val repo = FakeRepo { EligibleOffersResult(otherOffers = listOf(offer("b2")), otherIsLastPage = true) }
        val s = TestScopeStore(repo, testScheduler)
        s.dispatch(ChoosePromotionIntent.Preload(emptyList(), listOf(offer("b1")), myIsLastPage = true, otherIsLastPage = false))
        s.dispatch(ChoosePromotionIntent.LoadMoreOtherVouchers)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("b1", "b2"), s.currentState().otherOffers.map { it.source.id })
        assertEquals(1, s.currentState().otherPage)
    }

    @Test
    fun loadMore_failure_setsErrorAndClearsLoadingFlags() = runTest {
        val repo = FakeRepo { throw PromotionException("LM_ERR", "x") }
        val s = TestScopeStore(repo, testScheduler)
        s.dispatch(ChoosePromotionIntent.Preload(emptyList(), listOf(offer("b1")), myIsLastPage = true, otherIsLastPage = false))
        s.dispatch(ChoosePromotionIntent.LoadMoreOtherVouchers)
        testScheduler.advanceUntilIdle()

        val st = s.currentState()
        assertEquals("LM_ERR", st.errorCode)
        assertFalse(st.isLoadingMore)
        assertFalse(st.isLoadingMoreOther)
    }

    // ─── Search / keyword ─────────────────────────────────────────────────────

    @Test
    fun queryChanged_nonEmpty_debouncesBeforeFetch() = runTest {
        val repo = FakeRepo { EligibleOffersResult() }
        val s = TestScopeStore(repo, testScheduler)
        s.dispatch(ChoosePromotionIntent.QueryChanged("abc"))
        assertEquals(0, repo.calls)
        testScheduler.advanceUntilIdle()
        assertEquals(1, repo.calls)
        assertEquals("abc", repo.last?.keyword)
    }

    @Test
    fun queryChanged_blank_reloadsImmediatelyWithoutKeyword() = runTest {
        val repo = FakeRepo { EligibleOffersResult() }
        val s = TestScopeStore(repo, testScheduler)
        s.dispatch(ChoosePromotionIntent.QueryChanged("   "))
        testScheduler.advanceUntilIdle()
        assertEquals(1, repo.calls)
        assertNull(repo.last?.keyword)
    }

    @Test
    fun search_runsImmediatelyCancellingDebounce() = runTest {
        val repo = FakeRepo { EligibleOffersResult() }
        val s = TestScopeStore(repo, testScheduler)
        s.dispatch(ChoosePromotionIntent.QueryChanged("abc"))
        s.dispatch(ChoosePromotionIntent.Search)
        testScheduler.advanceUntilIdle()
        // Debounce bị huỷ → chỉ còn đúng một lần gọi của Search.
        assertEquals(1, repo.calls)
    }

    @Test
    fun clearKeyword_resetsKeywordAndReloads() = runTest {
        val repo = FakeRepo { EligibleOffersResult() }
        val s = TestScopeStore(repo, testScheduler)
        s.dispatch(ChoosePromotionIntent.QueryChanged("abc"))
        testScheduler.advanceUntilIdle()
        s.dispatch(ChoosePromotionIntent.ClearKeyword)
        testScheduler.advanceUntilIdle()

        assertEquals("", s.currentState().keyword)
        assertNull(repo.last?.keyword)
    }

    /**
     * Gõ vào ô tìm kiếm phải bật `isLoading` **ngay**, không đợi hết debounce 400ms. Nếu đợi thì
     * trong ~400ms đó native vẫn render list CŨ rồi mới nhảy sang shimmer — nhìn như nhấp nháy.
     * Cùng cách với `SearchMyPromotionStore.onQueryChanged`.
     */
    @Test
    fun queryChanged_setsLoadingImmediately_beforeDebounceFires() = runTest {
        val repo = FakeRepo { EligibleOffersResult(myOffers = listOf(offer("a"))) }
        val s = TestScopeStore(repo, testScheduler)
        s.dispatch(ChoosePromotionIntent.Preload(listOf(offer("old")), emptyList(), myIsLastPage = true, otherIsLastPage = true))
        testScheduler.advanceUntilIdle()

        s.dispatch(ChoosePromotionIntent.QueryChanged("abc"))
        // CHƯA advance: debounce vẫn đang chờ, nhưng loading phải bật rồi.
        assertTrue(s.currentState().isLoading)
        assertEquals(0, repo.calls)   // và chưa gọi API

        testScheduler.advanceUntilIdle()
        assertFalse(s.currentState().isLoading)
    }

    /**
     * Xoá trắng phải gọi lại API ngay, không qua debounce — kết quả tìm kiếm cũ mới được thay.
     *
     * KHÔNG assert `isLoading == true` ngay sau `dispatch` như test bên trên: store chạy trên
     * `UnconfinedTestDispatcher` nên `loadOffers` chạy tuốt tới khi xong (repo giả trả về ngay),
     * `isLoading` đã kịp về `false`. Nhánh "bật loading trước khi gọi API" đã được
     * `queryChanged_setsLoadingImmediately_beforeDebounceFires` phủ — ở đó `delay` của debounce giữ
     * coroutine lại nên quan sát được.
     */
    @Test
    fun clearKeyword_reloadsImmediatelyWithoutDebounce() = runTest {
        val repo = FakeRepo { EligibleOffersResult(myOffers = listOf(offer("a"))) }
        val s = TestScopeStore(repo, testScheduler)
        s.dispatch(ChoosePromotionIntent.Preload(listOf(offer("old")), emptyList(), myIsLastPage = true, otherIsLastPage = true))
        testScheduler.advanceUntilIdle()
        assertEquals(0, repo.calls)

        s.dispatch(ChoosePromotionIntent.ClearKeyword)

        // Chưa advance scheduler: không có debounce nào chắn, API phải được gọi rồi.
        assertEquals(1, repo.calls)
        assertEquals("", s.currentState().keyword)
    }

    // ─── Refresh + lỗi ────────────────────────────────────────────────────────

    @Test
    fun refresh_usesRefreshingFlagNotLoading() = runTest {
        val repo = FakeRepo { EligibleOffersResult(myOffers = listOf(offer("a"))) }
        val s = TestScopeStore(repo, testScheduler)
        s.dispatch(ChoosePromotionIntent.Refresh)
        testScheduler.advanceUntilIdle()
        val st = s.currentState()
        assertFalse(st.isRefreshing)
        assertFalse(st.isLoading)
        assertTrue(st.hasLoadedInitial)
    }

    @Test
    fun loadOffers_failure_emptiesListsAndFlagsEmpty() = runTest {
        val repo = FakeRepo { throw PromotionException("E1", "x") }
        val s = TestScopeStore(repo, testScheduler)
        s.dispatch(ChoosePromotionIntent.LoadInitial)
        testScheduler.advanceUntilIdle()

        val st = s.currentState()
        assertEquals("E1", st.errorCode)
        assertTrue(st.myOffers.isEmpty())
        assertTrue(st.isEmpty)
        assertTrue(st.hasLoadedInitial)

        s.dispatch(ChoosePromotionIntent.ConsumeError)
        assertNull(s.currentState().errorCode)
    }

    // ─── Rule dùng chung: mySeeMoreState / visibleMyOffers / toChooseOffer ─────

    private fun stateWith(n: Int, expanded: Boolean, lastPage: Boolean): ChoosePromotionState {
        var st = ChoosePromotionState(myExpanded = expanded, myIsLastPage = lastPage)
        // Dựng qua Preload để dùng đúng mapper toChooseOffer.
        return st.copy(myOffers = (1..n).map { i ->
            com.ttcn.promotionsdk.presentation.choosepromotion.ChooseOffer(offer("o$i"), isUsable = true, expiringInDays = null, isExpired = false)
        })
    }

    @Test
    fun mySeeMoreState_hidden_whenFewItemsAndLastPage() {
        assertEquals(ChooseSeeMoreState.HIDDEN, stateWith(COLLAPSED_MY_COUNT, expanded = false, lastPage = true).mySeeMoreState())
    }

    /** Nút ẩn kể cả khi chưa phải trang cuối: thu gọn đã thấy hết thì không có gì để mở thêm. */
    @Test
    fun mySeeMoreState_hidden_whenFewItems_evenIfNotLastPage() {
        assertEquals(ChooseSeeMoreState.HIDDEN, stateWith(1, expanded = false, lastPage = false).mySeeMoreState())
        assertEquals(ChooseSeeMoreState.HIDDEN, stateWith(COLLAPSED_MY_COUNT, expanded = false, lastPage = false).mySeeMoreState())
    }

    @Test
    fun mySeeMoreState_collapse_whenExpandedAndLastPage() {
        assertEquals(ChooseSeeMoreState.COLLAPSE, stateWith(COLLAPSED_MY_COUNT + 1, expanded = true, lastPage = true).mySeeMoreState())
    }

    @Test
    fun mySeeMoreState_expand_whenMoreItemsThanCollapsedCount() {
        assertEquals(ChooseSeeMoreState.EXPAND, stateWith(COLLAPSED_MY_COUNT + 1, expanded = false, lastPage = true).mySeeMoreState())
        assertEquals(ChooseSeeMoreState.EXPAND, stateWith(COLLAPSED_MY_COUNT + 1, expanded = true, lastPage = false).mySeeMoreState())
    }

    @Test
    fun visibleMyOffers_respectsCollapsedCount() {
        val collapsed = stateWith(COLLAPSED_MY_COUNT + 3, expanded = false, lastPage = true)
        assertEquals(COLLAPSED_MY_COUNT, collapsed.visibleMyOffers().size)
        assertEquals(COLLAPSED_MY_COUNT + 3, collapsed.copy(myExpanded = true).visibleMyOffers().size)
    }

    // ─── Rule dùng chung: selection / nút Áp dụng / "không tìm thấy" ───────────

    /** State có 2 offer "của tôi" (m1, m2) và 2 offer "khác" (o1, o2). */
    private fun selectionState(vararg selected: String) = ChoosePromotionState(
        myOffers = listOf("m1", "m2").map { offer(it).toChooseOfferForTest() },
        otherOffers = listOf("o1", "o2").map { offer(it).toChooseOfferForTest() },
        selectedIds = selected.toList(),
    )

    /** Gộp CẢ HAI nhóm, giữ thứ tự "của tôi" trước — thứ tự này là hợp đồng với native. */
    @Test
    fun allOffers_mergesBothGroupsInOrder() {
        assertEquals(listOf("m1", "m2", "o1", "o2"), selectionState().allOffers().map { it.id })
    }

    /** Lọc đúng theo `selectedIds`, lấy được cả offer nằm ở nhóm "Ưu đãi khác". */
    @Test
    fun selectedOffers_filtersBothGroups() {
        assertEquals(listOf("m2", "o1"), selectionState("m2", "o1").selectedOffers().map { it.id })
        assertTrue(selectionState().selectedOffers().isEmpty())
    }

    /** Id lạ (offer đã bị lọc khỏi danh sách sau khi search) không sinh phần tử rác. */
    @Test
    fun selectedOffers_ignoresUnknownIds() {
        assertEquals(listOf("m1"), selectionState("m1", "khong-ton-tai").selectedOffers().map { it.id })
    }

    @Test
    fun canApply_onlyWhenSomethingSelected() {
        assertFalse(selectionState().canApply())
        assertTrue(selectionState("m1").canApply())
    }

    /** Thanh "Đã chọn N" đòi CẢ HAI: bật multi-select và có item được chọn. */
    @Test
    fun showsSelectedCount_requiresMultiSelectionAndSelection() {
        assertFalse(selectionState("m1").showsSelectedCount())
        assertFalse(selectionState().copy(isMultiSelection = true).showsSelectedCount())
        assertTrue(selectionState("m1").copy(isMultiSelection = true).showsSelectedCount())
    }

    /** Rỗng mà KHÔNG có từ khoá = "chưa có ưu đãi nào", không phải "tìm không ra". */
    @Test
    fun showsNoResult_requiresKeyword() {
        assertFalse(ChoosePromotionState(isEmpty = true).showsNoResult())
        assertFalse(ChoosePromotionState(keyword = "   ", isEmpty = true).showsNoResult())
        assertTrue(ChoosePromotionState(keyword = "abc", isEmpty = true).showsNoResult())
    }

    /** Đang tải thì chưa kết luận được là không có kết quả. */
    @Test
    fun showsNoResult_falseWhileLoading() {
        assertFalse(ChoosePromotionState(keyword = "abc", isEmpty = true, isLoading = true).showsNoResult())
    }

    @Test
    fun highlightKeyword_isTrimmed() {
        assertEquals("abc", ChoosePromotionState(keyword = "  abc  ").highlightKeyword())
        assertEquals("", ChoosePromotionState(keyword = "   ").highlightKeyword())
    }

    @Test
    fun toChooseOffer_expiringDays_onlyWhenUsableAndWithinWarning() = runTest {
        // Không usable → không tính "còn X ngày" dù có ngưỡng.
        val repo = FakeRepo {
            EligibleOffersResult(
                myOffers = listOf(offer("a", usable = false, expire = "2099-01-01")),
                expireWarningDate = 30,
            )
        }
        val s = TestScopeStore(repo, testScheduler)
        s.dispatch(ChoosePromotionIntent.LoadInitial)
        testScheduler.advanceUntilIdle()

        val o = s.currentState().myOffers.single()
        assertFalse(o.isUsable)
        assertNull(o.expiringInDays)
    }

    /**
     * Ngoài dải cảnh báo → không hiện "còn X ngày".
     *
     * Trước đây test này khai `expireWarningDate = null` để ép ra null. Không dùng được nữa: thiếu
     * ngưỡng trong response thì [ExpiryWarning] lùi về bản nhớ gần nhất, mà object đó **toàn cục**
     * nên giá trị do test khác seed sẽ rò sang. Nay tự seed ngưỡng NHỎ rồi lấy ngày rất xa — kết quả
     * không phụ thuộc thứ tự chạy test.
     */
    @Test
    fun toChooseOffer_expiryOutsideWarningWindow_leavesDaysNull() = runTest {
        val repo = FakeRepo {
            EligibleOffersResult(myOffers = listOf(offer("a", expire = "2099-01-01")), expireWarningDate = 5)
        }
        val s = TestScopeStore(repo, testScheduler)
        s.dispatch(ChoosePromotionIntent.LoadInitial)
        testScheduler.advanceUntilIdle()

        val o = s.currentState().myOffers.single()
        assertTrue(o.isUsable)
        assertNull(o.expiringInDays)
    }

    // ─── Hết hạn & ngưỡng "sắp hết hạn" (2 nhánh mới) ─────────────────────────
    //
    // `ExpiryWarning` là **object toàn cục**, ngưỡng dính lại giữa các test trong cùng tiến trình và
    // không có API reset. Nên các test dưới KHÔNG dựa vào "chưa ai set ngưỡng"; chúng tự seed ngưỡng
    // rồi dùng mốc ngày CỐ ĐỊNH (2000/2099) + ngưỡng rất lớn, để kết quả không đổi theo ngày chạy.

    /** Ngưỡng đủ lớn để mọi ngày trong tương lai đều nằm trong dải "sắp hết hạn" (~26k ngày tới 2099). */
    private val hugeWarning = 40_000

    @Test
    fun toChooseOffer_expiredDate_disablesEvenWhenServerSaysUsable() = runTest {
        // Server nói dùng được (`usable = true`, tức displayMode != DISABLED) nhưng ngày đã qua.
        val repo = FakeRepo {
            EligibleOffersResult(
                myOffers = listOf(offer("a", usable = true, expire = "2000-01-01")),
                expireWarningDate = hugeWarning,
            )
        }
        val s = TestScopeStore(repo, testScheduler)
        s.dispatch(ChoosePromotionIntent.LoadInitial)
        testScheduler.advanceUntilIdle()

        val o = s.currentState().myOffers.single()
        assertFalse(o.isUsable, "hết hạn thì không được cho chọn, dù server chưa đánh DISABLED")
        assertTrue(o.isExpired, "native cần cờ này để hiện nhãn 'Đã hết hạn' thay vì badge rỗng")
        assertNull(o.expiringInDays, "đã hết hạn thì không còn là 'sắp hết hạn'")
    }

    /**
     * Dải "Chưa đủ điều kiện áp dụng" bám **đúng cờ `usable` của server**, không phải `isUsable`.
     *
     * Ưu đãi hết hạn mà server vẫn nói `usable = true` thì `isUsable` = false (client trừ hạn dùng)
     * nhưng **không** có dải: hết hạn thì sửa đơn kiểu gì cũng vô ích, treo "chưa đủ điều kiện" lên
     * là sai nghĩa. Ca đó chỉ mờ card + badge "Đã hết hạn". Hai nền tảng từng cùng bám `isUsable`.
     */
    @Test
    fun showsIneligibleWarning_expiredOffer_hasNoWarningStrip() = runTest {
        val repo = FakeRepo {
            EligibleOffersResult(
                myOffers = listOf(offer("a", usable = true, expire = "2000-01-01")),
                expireWarningDate = hugeWarning,
            )
        }
        val s = TestScopeStore(repo, testScheduler)
        s.dispatch(ChoosePromotionIntent.LoadInitial)
        testScheduler.advanceUntilIdle()

        val o = s.currentState().myOffers.single()
        assertFalse(o.isUsable, "hết hạn vẫn không cho chọn")
        assertFalse(o.showsIneligibleWarning(), "nhưng KHÔNG hiện dải 'Chưa đủ điều kiện áp dụng'")
    }

    /** Server đánh `displayMode = DISABLED` (`usable = false`) — đúng ca của dải. */
    @Test
    fun showsIneligibleWarning_serverSaysUnusable_showsWarningStrip() = runTest {
        val repo = FakeRepo {
            EligibleOffersResult(
                myOffers = listOf(offer("a", usable = false, expire = "2099-01-01")),
                expireWarningDate = hugeWarning,
            )
        }
        val s = TestScopeStore(repo, testScheduler)
        s.dispatch(ChoosePromotionIntent.LoadInitial)
        testScheduler.advanceUntilIdle()

        val o = s.currentState().myOffers.single()
        assertFalse(o.isUsable)
        assertFalse(o.isExpired)
        assertTrue(o.showsIneligibleWarning())
    }

    /** Còn hạn + server cho dùng → không dải, không mờ. */
    @Test
    fun showsIneligibleWarning_usableOffer_hasNoWarningStrip() = runTest {
        val repo = FakeRepo {
            EligibleOffersResult(
                myOffers = listOf(offer("a", usable = true, expire = "2099-01-01")),
                expireWarningDate = hugeWarning,
            )
        }
        val s = TestScopeStore(repo, testScheduler)
        s.dispatch(ChoosePromotionIntent.LoadInitial)
        testScheduler.advanceUntilIdle()

        assertFalse(s.currentState().myOffers.single().showsIneligibleWarning())
    }

    @Test
    fun toChooseOffer_futureDate_staysUsable() = runTest {
        val repo = FakeRepo {
            EligibleOffersResult(
                myOffers = listOf(offer("a", usable = true, expire = "2099-01-01")),
                expireWarningDate = hugeWarning,
            )
        }
        val s = TestScopeStore(repo, testScheduler)
        s.dispatch(ChoosePromotionIntent.LoadInitial)
        testScheduler.advanceUntilIdle()

        val o = s.currentState().myOffers.single()
        assertTrue(o.isUsable)
        assertFalse(o.isExpired)
        assertNotNull(o.expiringInDays, "trong ngưỡng thì phải ra số ngày")
    }

    /**
     * Luồng THƯỜNG của màn này: widget đã gọi `findEligible` rồi đưa danh sách sang bằng `Preload` —
     * không có response nào kèm theo nên `state.expireWarningDate` vẫn là default null.
     *
     * Trước khi sửa, `expiringInDays` ra null cho mọi item và dòng "HSD còn X ngày" không bao giờ
     * hiện. Nay lùi về ngưỡng `EndowStore.loadInitial` đã ghi nhớ.
     */
    @Test
    fun preload_fallsBackToRememberedWarningThreshold() = runTest {
        // Seed ngưỡng đúng như EndowStore làm khi nạp widget (nó gọi ExpiryWarning.remember).
        val seeding = FakeRepo {
            EligibleOffersResult(
                myOffers = listOf(offer("seed", expire = "2099-01-01")),
                expireWarningDate = hugeWarning,
            )
        }
        TestScopeStore(seeding, testScheduler).let {
            it.dispatch(ChoosePromotionIntent.LoadInitial)
            testScheduler.advanceUntilIdle()
        }

        // Store MỚI, chỉ nhận preload — không gọi mạng lần nào.
        val repo = FakeRepo { EligibleOffersResult() }
        val s = TestScopeStore(repo, testScheduler)
        s.dispatch(
            ChoosePromotionIntent.Preload(
                myOffers = listOf(offer("a", expire = "2099-01-01")),
                otherOffers = emptyList(),
                myIsLastPage = true,
                otherIsLastPage = true,
            )
        )
        testScheduler.advanceUntilIdle()

        assertEquals(0, repo.calls, "preload có dữ liệu thì không được gọi API")
        val st = s.currentState()
        assertEquals(hugeWarning, st.expireWarningDate, "ngưỡng phải được ghi vào state cho loadMore dùng lại")
        assertNotNull(st.myOffers.single().expiringInDays, "đây chính là dòng 'HSD còn X ngày' từng mất")
    }

    // ─── SeedOnce & ngưỡng paging (rule vừa hạ xuống store) ───────────────────

    @Test
    fun seedOnce_ignoresSecondCall_soUserSelectionSurvivesViewRecreation() = runTest {
        val repo = FakeRepo { EligibleOffersResult() }
        val s = TestScopeStore(repo, testScheduler)
        val seed = ChoosePromotionIntent.SeedOnce(
            preSelectedIds = listOf("pre"),
            myOffers = listOf(offer("a")),
            otherOffers = listOf(offer("b")),
            myIsLastPage = true,
            otherIsLastPage = true,
        )
        s.dispatch(seed)
        testScheduler.advanceUntilIdle()
        s.dispatch(ChoosePromotionIntent.ToggleSelection("b"))   // user tick thêm

        // View dựng lại → Android bắn seed lần nữa. Trước đây ghi đè về `["pre"]`.
        s.dispatch(seed)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("b"), s.currentState().selectedIds, "seed lần 2 không được đụng selection")
        assertEquals(0, repo.calls)
    }

    @Test
    fun shouldLoadMoreOther_onlyAtLastItemAndWhenMorePagesRemain() {
        val two = ChoosePromotionState(
            otherOffers = listOf(offer("a"), offer("b")).map { it.toChooseOfferForTest() },
            otherIsLastPage = false,
        )
        assertFalse(two.shouldLoadMoreOther(0), "chưa tới item cuối")
        assertTrue(two.shouldLoadMoreOther(1), "item cuối → nạp")

        assertFalse(two.copy(otherIsLastPage = true).shouldLoadMoreOther(1), "hết trang")
        assertFalse(two.copy(isLoadingMoreOther = true).shouldLoadMoreOther(1), "đang nạp rồi")
        assertFalse(two.copy(isLoading = true).shouldLoadMoreOther(1), "đang load lại cả màn")
        assertFalse(
            ChoosePromotionState(otherIsLastPage = false).shouldLoadMoreOther(0),
            "nhóm rỗng thì không có gì để chạm tới",
        )
    }

    /** Danh sách 1 item: iOS vốn nạp được, Android cũ đòi `dy > 0` nên không bao giờ nạp. */
    @Test
    fun shouldLoadMoreOther_firesEvenWhenListTooShortToScroll() {
        val one = ChoosePromotionState(
            otherOffers = listOf(offer("a").toChooseOfferForTest()),
            otherIsLastPage = false,
        )
        assertTrue(one.shouldLoadMoreOther(0))
    }
}
