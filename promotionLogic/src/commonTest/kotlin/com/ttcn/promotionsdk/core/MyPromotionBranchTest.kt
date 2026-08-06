package com.ttcn.promotionsdk.core

import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
import com.ttcn.promotionsdk.core.di.PromotionContainer
import com.ttcn.promotionsdk.core.domain.exception.PromotionException
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffersResult
import com.ttcn.promotionsdk.core.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionResult
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsResult
import com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersResult
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherDetail
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherItem
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherTabItem
import com.ttcn.promotionsdk.core.domain.repository.PromotionRepository
import com.ttcn.promotionsdk.core.domain.model.featureflag.PromotionFeatureFlag
import com.ttcn.promotionsdk.core.domain.usecase.PromotionFeatureGate
import com.ttcn.promotionsdk.core.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionAction
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionBadge
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionIntent
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionStore
import com.ttcn.promotionsdk.presentation.searchmypromotion.SearchMyPromotionIntent
import com.ttcn.promotionsdk.presentation.searchmypromotion.SearchMyPromotionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestCoroutineScheduler
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
 * Phủ nhánh còn lại của [MyPromotionStore] (cache theo tab, latest-wins, nhánh lỗi),
 * [SearchMyPromotionStore] (ClearKeyword/Retry/guard load-more) và các rule mức file.
 */
class MyPromotionBranchTest {

    private class FakeRepo(
        var page: (Int, String?, String?) -> SearchCustomerVouchersResult?,
    ) : PromotionRepository {
        var calls = 0
        var lastTab: String? = null
        override suspend fun searchCustomerVouchers(keyword: String?, serviceCode: String?, tab: String?, page: Int?, size: Int?): SearchCustomerVouchersResult? {
            calls++; lastTab = tab; return page(page ?: 0, tab, keyword)
        }
        override suspend fun getCustomerVoucherDetail(voucherId: String, service: String?): VoucherDetail? = null
        override suspend fun createRedemptionSession(request: CreateRedemptionRequest): CreateRedemptionResult? = null
        override suspend fun validateStackableDiscounts(request: ValidateDiscountsRequest): ValidateDiscountsResult? = null
        override suspend fun findEligibleCampaigns(request: FindEligibleCampaignsRequest): EligibleOffersResult? = null
    }

    @BeforeTest
    fun setUp() = PromotionContainer.initialize(PromotionSDKConfig(baseUrl = "https://a.example.com"))

    @AfterTest
    fun tearDown() = PromotionContainer.clear()

    private fun voucher(id: String, status: String = "ACTIVE", expire: String? = null) =
        VoucherItem(voucherId = id, status = status, title = "V-$id", expirationDate = expire)

    private fun result(
        items: List<VoucherItem>, number: Int = 0, last: Boolean = true,
        tabs: List<VoucherTabItem> = emptyList(), selectedTab: String? = null, warn: Int? = null,
    ) = SearchCustomerVouchersResult(
        content = items, number = number, size = 10, last = last,
        tabs = tabs, selectedTab = selectedTab, expireWarningDate = warn,
    )

    private fun myStore(repo: FakeRepo, sch: TestCoroutineScheduler) =
        MyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(sch)))

    private fun searchStore(repo: FakeRepo, sch: TestCoroutineScheduler) =
        SearchMyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(sch)))

    // ─── MyPromotionStore ─────────────────────────────────────────────────────

    @Test
    fun loadInitialIfNeeded_secondCallIsNoOp() = runTest {
        val repo = FakeRepo { _, _, _ -> result(listOf(voucher("1"))) }
        val s = myStore(repo, testScheduler)
        s.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        testScheduler.advanceUntilIdle()
        val after = repo.calls
        s.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        testScheduler.advanceUntilIdle()
        assertEquals(after, repo.calls)
    }

    @Test
    fun selectTab_secondVisitServesFreshCache_withoutCallingApi() = runTest {
        val repo = FakeRepo { _, tab, _ -> result(listOf(voucher("v-$tab")), tabs = emptyList()) }
        val s = myStore(repo, testScheduler)
        s.dispatch(MyPromotionIntent.SelectTab("t1"))
        testScheduler.advanceUntilIdle()
        s.dispatch(MyPromotionIntent.SelectTab("t2"))
        testScheduler.advanceUntilIdle()

        // Quay lại t1: cache còn tươi (< TTL) → dùng thẳng, KHÔNG gọi lại API.
        val before = repo.calls
        s.dispatch(MyPromotionIntent.SelectTab("t1"))
        testScheduler.advanceUntilIdle()
        assertEquals(before, repo.calls)
        assertEquals("t1", s.currentState().selectedTabCode)
        assertFalse(s.currentState().isRefreshingTab)
    }

    @Test
    fun search_blankKeyword_isIgnored() = runTest {
        val repo = FakeRepo { _, _, _ -> result(emptyList()) }
        val s = myStore(repo, testScheduler)
        s.dispatch(MyPromotionIntent.Search("   "))
        testScheduler.advanceUntilIdle()
        assertEquals(0, repo.calls)
    }

    @Test
    fun search_nonBlank_queriesAllTab() = runTest {
        val repo = FakeRepo { _, _, _ -> result(listOf(voucher("1"))) }
        val s = myStore(repo, testScheduler)
        s.dispatch(MyPromotionIntent.Search("grab"))
        testScheduler.advanceUntilIdle()
        assertEquals("all", repo.lastTab)
        assertEquals("grab", s.currentState().keyword)
    }

    @Test
    fun loadMore_ignoredWhenListEmpty() = runTest {
        val repo = FakeRepo { _, _, _ -> result(emptyList()) }
        val s = myStore(repo, testScheduler)
        s.dispatch(MyPromotionIntent.LoadMore)
        testScheduler.advanceUntilIdle()
        assertEquals(0, repo.calls)
    }

    @Test
    fun loadMore_appendsAndStopsAtLastPage() = runTest {
        val repo = FakeRepo { page, _, _ ->
            if (page == 0) result(listOf(voucher("1")), number = 0, last = false)
            else result(listOf(voucher("2")), number = 1, last = true)
        }
        val s = myStore(repo, testScheduler)
        s.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        testScheduler.advanceUntilIdle()
        s.dispatch(MyPromotionIntent.LoadMore)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("1", "2"), s.currentState().vouchers.map { it.source.voucherId })
        assertTrue(s.currentState().isLastPage)

        val before = repo.calls
        s.dispatch(MyPromotionIntent.LoadMore)     // hết trang → bỏ qua
        testScheduler.advanceUntilIdle()
        assertEquals(before, repo.calls)
    }

    @Test
    fun refresh_failureKeepsLoadedFlagAndSetsError() = runTest {
        var fail = false
        val repo = FakeRepo { _, _, _ -> if (fail) throw PromotionException("R_ERR", "x") else result(listOf(voucher("1"))) }
        val s = myStore(repo, testScheduler)
        s.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        testScheduler.advanceUntilIdle()

        fail = true
        s.dispatch(MyPromotionIntent.Refresh)
        testScheduler.advanceUntilIdle()

        val st = s.currentState()
        assertEquals("R_ERR", st.errorCode)
        assertTrue(st.hasLoadedInitial)
        assertFalse(st.isRefreshing)

        s.dispatch(MyPromotionIntent.ConsumeError)
        assertNull(s.currentState().errorCode)
    }

    @Test
    fun resolveActiveTab_fallsBackToRequestedThenFirstTab() = runTest {
        val repo = FakeRepo { _, _, _ ->
            result(listOf(voucher("1")), tabs = listOf(VoucherTabItem(code = "t9", label = "T9", count = 1, order = 1)))
        }
        val s = myStore(repo, testScheduler)
        s.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        testScheduler.advanceUntilIdle()
        assertEquals("t9", s.currentState().selectedTabCode)
    }

    // ─── toMyPromotionVoucher — badge/action (rule dùng chung) ────────────────

    @Test
    fun badge_used_expired_ineligible_none() = runTest {
        val repo = FakeRepo { _, _, _ ->
            result(
                listOf(
                    voucher("used", status = "USED"),
                    voucher("exp", status = "EXPIRED"),
                    voucher("inel", status = "REVOKED"),
                    voucher("ok", status = "ACTIVE"),
                ),
            )
        }
        val s = myStore(repo, testScheduler)
        s.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        testScheduler.advanceUntilIdle()

        val by = s.currentState().vouchers.associateBy { it.source.voucherId }
        assertEquals(MyPromotionBadge.USED, by["used"]!!.badge)
        assertEquals(MyPromotionBadge.EXPIRED, by["exp"]!!.badge)
        assertEquals(MyPromotionBadge.INELIGIBLE, by["inel"]!!.badge)
        assertEquals(MyPromotionBadge.NONE, by["ok"]!!.badge)

        // action: dùng được → USE, còn lại NONE.
        assertEquals(MyPromotionAction.USE, by["ok"]!!.action)
        assertEquals(MyPromotionAction.NONE, by["used"]!!.action)
        assertFalse(by["exp"]!!.isEnabled)
    }

    @Test
    fun badge_none_whenNoWarningThreshold() = runTest {
        val repo = FakeRepo { _, _, _ -> result(listOf(voucher("a", expire = "2099-12-31")), warn = null) }
        val s = myStore(repo, testScheduler)
        s.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        testScheduler.advanceUntilIdle()

        val v = s.currentState().vouchers.single()
        assertNull(v.expiringInDays)
        assertEquals(MyPromotionBadge.NONE, v.badge)
    }

    // ─── SearchMyPromotionStore ──────────────────────────────────────────────

    @Test
    fun clearKeyword_wipesResults() = runTest {
        val repo = FakeRepo { _, _, _ -> result(listOf(voucher("1"))) }
        val s = searchStore(repo, testScheduler)
        s.dispatch(SearchMyPromotionIntent.QueryChanged("abc"))
        testScheduler.advanceUntilIdle()
        assertEquals(1, s.currentState().vouchers.size)

        s.dispatch(SearchMyPromotionIntent.ClearKeyword)
        val st = s.currentState()
        assertEquals("", st.keyword)
        assertTrue(st.vouchers.isEmpty())
        assertFalse(st.isLoading)
        assertTrue(st.isLastPage)
    }

    @Test
    fun queryChanged_blank_resetsWithoutCallingApi() = runTest {
        val repo = FakeRepo { _, _, _ -> result(listOf(voucher("1"))) }
        val s = searchStore(repo, testScheduler)
        s.dispatch(SearchMyPromotionIntent.QueryChanged("   "))
        testScheduler.advanceUntilIdle()
        assertEquals(0, repo.calls)
        assertFalse(s.currentState().isLoading)
    }

    @Test
    fun retry_withKeyword_searchesAgain_withoutKeyword_isNoOp() = runTest {
        val repo = FakeRepo { _, _, _ -> result(listOf(voucher("1"))) }
        val s = searchStore(repo, testScheduler)

        s.dispatch(SearchMyPromotionIntent.Retry)      // chưa có keyword → bỏ qua
        testScheduler.advanceUntilIdle()
        assertEquals(0, repo.calls)

        s.dispatch(SearchMyPromotionIntent.QueryChanged("abc"))
        testScheduler.advanceUntilIdle()
        val after = repo.calls
        s.dispatch(SearchMyPromotionIntent.Retry)
        testScheduler.advanceUntilIdle()
        assertTrue(repo.calls > after)
    }

    @Test
    fun search_immediate_skipsDebounce() = runTest {
        val repo = FakeRepo { _, _, _ -> result(listOf(voucher("1"))) }
        val s = searchStore(repo, testScheduler)
        s.dispatch(SearchMyPromotionIntent.QueryChanged("abc"))
        s.dispatch(SearchMyPromotionIntent.Search)
        testScheduler.advanceUntilIdle()
        assertEquals(1, repo.calls)
    }

    @Test
    fun loadMore_guards_emptyKeywordAndLastPage() = runTest {
        val repo = FakeRepo { _, _, _ -> result(listOf(voucher("1")), last = true) }
        val s = searchStore(repo, testScheduler)

        s.dispatch(SearchMyPromotionIntent.LoadMore)   // chưa gõ gì
        testScheduler.advanceUntilIdle()
        assertEquals(0, repo.calls)

        s.dispatch(SearchMyPromotionIntent.QueryChanged("abc"))
        testScheduler.advanceUntilIdle()
        val after = repo.calls
        s.dispatch(SearchMyPromotionIntent.LoadMore)   // đã hết trang
        testScheduler.advanceUntilIdle()
        assertEquals(after, repo.calls)
    }

    @Test
    fun search_failure_onReset_emptiesAndFlagsEmpty() = runTest {
        val repo = FakeRepo { _, _, _ -> throw PromotionException("S_ERR", "x") }
        val s = searchStore(repo, testScheduler)
        s.dispatch(SearchMyPromotionIntent.QueryChanged("abc"))
        testScheduler.advanceUntilIdle()

        val st = s.currentState()
        assertEquals("S_ERR", st.errorCode)
        assertTrue(st.vouchers.isEmpty())
        assertTrue(st.isEmpty)

        s.dispatch(SearchMyPromotionIntent.ConsumeError)
        assertNull(s.currentState().errorCode)
    }

    // ─── PromotionFeatureGate — fail-open ─────────────────────────────────────

    @Test
    fun featureGate_neverThrows_andEachGateMatchesItsFlag() {
        // Bất biến đúng trên CẢ hai nền tảng: gate không bao giờ ném (mọi lỗi tra cờ đều bị nuốt),
        // và mỗi hàm canX() chỉ là bí danh của isEnabled(<cờ tương ứng>).
        //
        // CỐ Ý không assert "luôn true": fail-open chỉ xảy ra khi lookup NÉM. JVM (unit test, không
        // có SharedPreferences) thì ném → true; Native thì NSUserDefaults chạy được, cache rỗng →
        // trả false. Khác biệt này là của tầng storage, không phải của gate.
        val flags = listOf(
            PromotionFeatureFlag.ENABLE_ALL to PromotionFeatureGate.isSdkEnabled(),
            PromotionFeatureFlag.VOUCHER_LIST to PromotionFeatureGate.canOpenVoucherList(),
            PromotionFeatureFlag.VOUCHER_DETAIL to PromotionFeatureGate.canOpenVoucherDetail(),
            PromotionFeatureFlag.VOUCHER_SELECTION to PromotionFeatureGate.canShowVoucherSelection(),
            PromotionFeatureFlag.VOUCHER_APPLY to PromotionFeatureGate.canApplyVoucher(),
            PromotionFeatureFlag.VOUCHER_REDEEM to PromotionFeatureGate.canRedeemVoucher(),
        )
        for ((flag, gate) in flags) assertEquals(PromotionFeatureGate.isEnabled(flag), gate)

        // Cờ không tồn tại vẫn phải trả về được một Boolean, không ném.
        PromotionFeatureGate.isEnabled("KHONG_TON_TAI")
    }
}
