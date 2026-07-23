package com.ttcn.promotionsdk.core

import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
import com.ttcn.promotionsdk.core.di.PromotionContainer
import com.ttcn.promotionsdk.core.domain.exception.PromotionException
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffersResult
import com.ttcn.promotionsdk.core.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionResult
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsResult
import com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersResult
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherDetail
import com.ttcn.promotionsdk.core.domain.repository.PromotionRepository
import com.ttcn.promotionsdk.core.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.presentation.choosepromotion.COLLAPSED_MY_COUNT
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionIntent
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionState
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionStore
import com.ttcn.promotionsdk.presentation.choosepromotion.ChooseSeeMoreState
import com.ttcn.promotionsdk.presentation.choosepromotion.mySeeMoreState
import com.ttcn.promotionsdk.presentation.choosepromotion.visibleMyOffers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
            com.ttcn.promotionsdk.presentation.choosepromotion.ChooseOffer(offer("o$i"), isUsable = true, expiringInDays = null)
        })
    }

    @Test
    fun mySeeMoreState_hidden_whenFewItemsAndLastPage() {
        assertEquals(ChooseSeeMoreState.HIDDEN, stateWith(COLLAPSED_MY_COUNT, expanded = false, lastPage = true).mySeeMoreState())
    }

    @Test
    fun mySeeMoreState_collapse_whenExpandedAndLastPage() {
        assertEquals(ChooseSeeMoreState.COLLAPSE, stateWith(COLLAPSED_MY_COUNT + 1, expanded = true, lastPage = true).mySeeMoreState())
    }

    @Test
    fun mySeeMoreState_expand_whenMorePagesEvenIfFewItems() {
        assertEquals(ChooseSeeMoreState.EXPAND, stateWith(1, expanded = false, lastPage = false).mySeeMoreState())
    }

    @Test
    fun visibleMyOffers_respectsCollapsedCount() {
        val collapsed = stateWith(COLLAPSED_MY_COUNT + 3, expanded = false, lastPage = true)
        assertEquals(COLLAPSED_MY_COUNT, collapsed.visibleMyOffers().size)
        assertEquals(COLLAPSED_MY_COUNT + 3, collapsed.copy(myExpanded = true).visibleMyOffers().size)
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

    @Test
    fun toChooseOffer_noWarningThreshold_leavesDaysNull() = runTest {
        val repo = FakeRepo {
            EligibleOffersResult(myOffers = listOf(offer("a", expire = "2099-01-01")), expireWarningDate = null)
        }
        val s = TestScopeStore(repo, testScheduler)
        s.dispatch(ChoosePromotionIntent.LoadInitial)
        testScheduler.advanceUntilIdle()
        assertNull(s.currentState().myOffers.single().expiringInDays)
    }
}
