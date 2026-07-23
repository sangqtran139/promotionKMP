package com.ttcn.promotionsdk.core

import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
import com.ttcn.promotionsdk.core.di.PromotionContainer
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
import com.ttcn.promotionsdk.core.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionIntent
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Những nhánh cuối của [MyPromotionStore]: chọn tab "all" cho search, các guard load-more còn lại,
 * và fallback khi response thiếu `number`/`last`.
 */
class MyPromotionLastBranchTest {

    private class Repo(var page: (Int, String?) -> SearchCustomerVouchersResult?) : PromotionRepository {
        var calls = 0
        var lastTab: String? = null
        override suspend fun searchCustomerVouchers(keyword: String?, serviceCode: String?, tab: String?, page: Int?, size: Int?): SearchCustomerVouchersResult? {
            calls++; lastTab = tab; return page(page ?: 0, tab)
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

    private fun v(id: String) = VoucherItem(voucherId = id, status = "ACTIVE", title = id)

    private fun store(repo: Repo, sch: kotlinx.coroutines.test.TestCoroutineScheduler) =
        MyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(sch)))

    @Test
    fun search_usesExistingAllTabCodeWhenPresent() = runTest {
        // Tab "all" đã có trong danh sách tab của server ⇒ dùng đúng code đó cho search.
        val repo = Repo { _, _ ->
            SearchCustomerVouchersResult(
                content = listOf(v("1")), number = 0, size = 10, last = true,
                tabs = listOf(VoucherTabItem("all", "Tat ca", 1, 1)),
            )
        }
        val s = store(repo, testScheduler)
        s.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        testScheduler.advanceUntilIdle()

        s.dispatch(MyPromotionIntent.Search("tim"))
        testScheduler.advanceUntilIdle()
        assertEquals("all", repo.lastTab)
    }

    @Test
    fun responseWithoutPageInfo_usesRequestedPageAndAssumesLastPage() = runTest {
        // Thiếu `number`/`last` ⇒ lùi về trang đã yêu cầu và coi như hết trang (không lặp vô hạn).
        val repo = Repo { _, _ -> SearchCustomerVouchersResult(content = listOf(v("1"))) }
        val s = store(repo, testScheduler)
        s.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        testScheduler.advanceUntilIdle()

        val st = s.currentState()
        assertEquals(0, st.page)
        assertTrue(st.isLastPage)
    }

    @Test
    fun loadMore_ignoredWhileRefreshing() = runTest {
        val repo = Repo { p, _ ->
            SearchCustomerVouchersResult(content = listOf(v("v$p")), number = p, size = 10, last = false)
        }
        val s = store(repo, testScheduler)
        s.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        testScheduler.advanceUntilIdle()

        // Bắt đầu refresh rồi chen load-more ngay ⇒ bị bỏ (guard isRefreshing).
        s.dispatch(MyPromotionIntent.Refresh)
        s.dispatch(MyPromotionIntent.LoadMore)
        testScheduler.advanceUntilIdle()
        assertFalse(s.currentState().isLoadingMore)
    }

    @Test
    fun loadMore_ignoredWhenAlreadyLastPage() = runTest {
        val repo = Repo { _, _ -> SearchCustomerVouchersResult(content = listOf(v("1")), number = 0, size = 10, last = true) }
        val s = store(repo, testScheduler)
        s.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        testScheduler.advanceUntilIdle()
        val before = repo.calls

        s.dispatch(MyPromotionIntent.LoadMore)
        testScheduler.advanceUntilIdle()
        assertEquals(before, repo.calls)
    }

    @Test
    fun expireWarningFromResponse_isAppliedToEveryVoucher() = runTest {
        val repo = Repo { _, _ ->
            SearchCustomerVouchersResult(
                content = listOf(
                    VoucherItem(voucherId = "a", status = "ACTIVE", title = "a", expirationDate = "2099-12-31"),
                    VoucherItem(voucherId = "b", status = "ACTIVE", title = "b", expirationDate = "2099-12-31"),
                ),
                number = 0, size = 10, last = true, expireWarningDate = 9_999_999,
            )
        }
        val s = store(repo, testScheduler)
        s.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        testScheduler.advanceUntilIdle()

        assertTrue(s.currentState().vouchers.all { it.expiringInDays != null })
    }

    @Test
    fun selectTab_thenLoadMore_usesSelectedTab() = runTest {
        val repo = Repo { p, _ ->
            SearchCustomerVouchersResult(content = listOf(v("v$p")), number = p, size = 10, last = p >= 1)
        }
        val s = store(repo, testScheduler)
        s.dispatch(MyPromotionIntent.SelectTab("t7"))
        testScheduler.advanceUntilIdle()
        s.dispatch(MyPromotionIntent.LoadMore)
        testScheduler.advanceUntilIdle()

        assertEquals("t7", repo.lastTab)
        assertEquals(listOf("v0", "v1"), s.currentState().vouchers.map { it.source.voucherId })
    }
}
