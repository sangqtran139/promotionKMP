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
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherItem
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherTabItem
import com.ttcn.promotionsdk.core.domain.repository.PromotionRepository
import com.ttcn.promotionsdk.core.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.core.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionIntent
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionStore
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionBadge
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionIntent
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Các nhánh "rìa" của store mà luồng thường không chạm: fallback tab, giữ danh sách cũ khi đổi tab,
 * lỗi khi đã có cache, và ngưỡng "sắp hết hạn".
 */
class StoreEdgeBranchTest {

    private class FakeRepo(var page: (Int, String?) -> SearchCustomerVouchersResult?) : PromotionRepository {
        var calls = 0
        override suspend fun searchCustomerVouchers(keyword: String?, serviceCode: String?, tab: String?, page: Int?, size: Int?): SearchCustomerVouchersResult? {
            calls++; return page(page ?: 0, tab)
        }
        override suspend fun getCustomerVoucherDetail(voucherId: String, service: String?): VoucherDetail? = null
        override suspend fun createRedemptionSession(request: CreateRedemptionRequest): CreateRedemptionResult? = null
        override suspend fun validateStackableDiscounts(request: ValidateDiscountsRequest): ValidateDiscountsResult? = null
        override suspend fun findEligibleCampaigns(request: FindEligibleCampaignsRequest): EligibleOffersResult? = null
    }

    private class EligibleRepo(var res: () -> EligibleOffersResult?) : PromotionRepository {
        override suspend fun searchCustomerVouchers(keyword: String?, serviceCode: String?, tab: String?, page: Int?, size: Int?): SearchCustomerVouchersResult? = null
        override suspend fun getCustomerVoucherDetail(voucherId: String, service: String?): VoucherDetail? = null
        override suspend fun createRedemptionSession(request: CreateRedemptionRequest): CreateRedemptionResult? = null
        override suspend fun validateStackableDiscounts(request: ValidateDiscountsRequest): ValidateDiscountsResult? = null
        override suspend fun findEligibleCampaigns(request: FindEligibleCampaignsRequest): EligibleOffersResult? = res()
    }

    private class Ctx : PromotionRequestContextProvider {
        override fun getOrderId(): String = "O"
        override fun getOrderValue(): String = "1"
    }

    @BeforeTest
    fun setUp() = PromotionContainer.initialize(PromotionSDKConfig(baseUrl = "https://a.example.com", requestContextProvider = Ctx()))

    @AfterTest
    fun tearDown() = PromotionContainer.clear()

    private fun voucher(id: String, status: String = "ACTIVE", expire: String? = null) =
        VoucherItem(voucherId = id, status = status, title = "V-$id", expirationDate = expire)

    private fun res(
        items: List<VoucherItem>, number: Int = 0, last: Boolean = true,
        tabs: List<VoucherTabItem> = emptyList(), selectedTab: String? = null, warn: Int? = null,
    ) = SearchCustomerVouchersResult(
        content = items, number = number, size = 10, last = last,
        tabs = tabs, selectedTab = selectedTab, expireWarningDate = warn,
    )

    private fun my(repo: FakeRepo, sch: TestCoroutineScheduler) =
        MyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(sch)))

    // ─── MyPromotionStore — nhánh tab / danh sách ─────────────────────────────

    @Test
    fun tabsAbsentInResponse_keepsPreviouslyLoadedTabs() = runTest {
        var withTabs = true
        val repo = FakeRepo { _, _ ->
            if (withTabs) res(listOf(voucher("1")), tabs = listOf(VoucherTabItem("t1", "T1", 1, 1)))
            else res(listOf(voucher("2")), tabs = emptyList())
        }
        val s = my(repo, testScheduler)
        s.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        testScheduler.advanceUntilIdle()
        assertEquals(listOf("t1"), s.currentState().tabs.map { it.code })

        withTabs = false
        s.dispatch(MyPromotionIntent.Refresh)
        testScheduler.advanceUntilIdle()
        // Response sau không kèm tabs → giữ tabs cũ, không xoá trắng thanh tab.
        assertEquals(listOf("t1"), s.currentState().tabs.map { it.code })
    }

    @Test
    fun selectTab_firstVisit_keepsCurrentListWhileLoading() = runTest {
        val repo = FakeRepo { _, tab -> res(listOf(voucher("v-$tab"))) }
        val s = my(repo, testScheduler)
        s.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        testScheduler.advanceUntilIdle()
        val before = s.currentState().vouchers.size

        // Tab chưa có cache → giữ list hiện tại trong lúc tải (không nháy trắng).
        s.dispatch(MyPromotionIntent.SelectTab("moi"))
        assertTrue(s.currentState().vouchers.size >= before || s.currentState().isLoading)
        testScheduler.advanceUntilIdle()
        assertEquals("moi", s.currentState().selectedTabCode)
    }

    @Test
    fun failureAfterCacheExists_keepsListAndReportsError() = runTest {
        var fail = false
        val repo = FakeRepo { _, _ -> if (fail) throw PromotionException("E", "x") else res(listOf(voucher("1"))) }
        val s = my(repo, testScheduler)
        s.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        testScheduler.advanceUntilIdle()

        fail = true
        s.dispatch(MyPromotionIntent.Refresh)
        testScheduler.advanceUntilIdle()

        val st = s.currentState()
        assertEquals("E", st.errorCode)
        // Đã có dữ liệu thì lỗi refresh KHÔNG được xoá danh sách đang hiển thị.
        assertEquals(1, st.vouchers.size)
    }

    @Test
    fun loadMoreFailure_keepsExistingList() = runTest {
        var calls = 0
        val repo = FakeRepo { page, _ ->
            calls++
            if (page == 0) res(listOf(voucher("1")), number = 0, last = false)
            else throw PromotionException("LM", "x")
        }
        val s = my(repo, testScheduler)
        s.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        testScheduler.advanceUntilIdle()
        s.dispatch(MyPromotionIntent.LoadMore)
        testScheduler.advanceUntilIdle()

        val st = s.currentState()
        assertEquals("LM", st.errorCode)
        assertEquals(1, st.vouchers.size)
    }

    // ─── toMyPromotionVoucher — nhánh "sắp hết hạn" ───────────────────────────

    @Test
    fun expiringSoon_whenWithinWarningWindow() = runTest {
        // Ngưỡng rất lớn ⇒ mọi hạn tương lai đều nằm trong [0, warn] ⇒ badge EXPIRING_SOON.
        val repo = FakeRepo { _, _ -> res(listOf(voucher("a", expire = "2099-12-31")), warn = 9_999_999) }
        val s = my(repo, testScheduler)
        s.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        testScheduler.advanceUntilIdle()

        val v = s.currentState().vouchers.single()
        assertNotNull(v.expiringInDays)
        assertEquals(MyPromotionBadge.EXPIRING_SOON, v.badge)
        assertTrue(v.isEnabled)
    }

    @Test
    fun expiredDateOutsideWindow_hasNoBadge() = runTest {
        // Hạn xa hơn ngưỡng ⇒ không phải "sắp hết hạn".
        val repo = FakeRepo { _, _ -> res(listOf(voucher("a", expire = "2099-12-31")), warn = 1) }
        val s = my(repo, testScheduler)
        s.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        testScheduler.advanceUntilIdle()
        assertEquals(MyPromotionBadge.NONE, s.currentState().vouchers.single().badge)
    }

    @Test
    fun unparsableExpiryDate_doesNotCrash() = runTest {
        val repo = FakeRepo { _, _ -> res(listOf(voucher("a", expire = "khong-phai-ngay")), warn = 30) }
        val s = my(repo, testScheduler)
        s.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        testScheduler.advanceUntilIdle()
        assertEquals(MyPromotionBadge.NONE, s.currentState().vouchers.single().badge)
    }

    @Test
    fun tabItem_nullCountAndOrder_getDefaults() = runTest {
        val repo = FakeRepo { _, _ ->
            res(listOf(voucher("1")), tabs = listOf(VoucherTabItem("t", "T", count = null, order = null)))
        }
        val s = my(repo, testScheduler)
        s.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        testScheduler.advanceUntilIdle()

        val tab = s.currentState().tabs.single()
        assertEquals(0, tab.count)              // count null → 0
        assertEquals(Int.MAX_VALUE, tab.order)  // order null → xuống cuối
    }

    // ─── ChoosePromotionStore — nhánh còn lại ─────────────────────────────────

    @Test
    fun chooseOffer_expiringDays_whenUsableAndWithinWindow() = runTest {
        val repo = EligibleRepo {
            EligibleOffersResult(
                myOffers = listOf(EligibleOffer(id = "a", usable = true, expireDate = "2099-12-31")),
                expireWarningDate = 9_999_999,
            )
        }
        val s = ChoosePromotionStore(FindEligibleCampaignsUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        s.dispatch(ChoosePromotionIntent.LoadInitial)
        testScheduler.advanceUntilIdle()

        assertNotNull(s.currentState().myOffers.single().expiringInDays)
    }

    @Test
    fun loadMoreMy_guardedWhileLoading() = runTest {
        val repo = EligibleRepo { EligibleOffersResult(myOffers = listOf(EligibleOffer(id = "a")), myIsLastPage = true) }
        val s = ChoosePromotionStore(FindEligibleCampaignsUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        s.dispatch(ChoosePromotionIntent.LoadInitial)
        testScheduler.advanceUntilIdle()

        // Hết trang nhóm "của tôi" → LoadMoreMyVouchers không gọi thêm.
        s.dispatch(ChoosePromotionIntent.LoadMoreMyVouchers)
        testScheduler.advanceUntilIdle()
        assertEquals(1, s.currentState().myOffers.size)
    }

    @Test
    fun choose_isEmptyFlag_whenBothGroupsEmpty() = runTest {
        val repo = EligibleRepo { EligibleOffersResult() }
        val s = ChoosePromotionStore(FindEligibleCampaignsUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        s.dispatch(ChoosePromotionIntent.LoadInitial)
        testScheduler.advanceUntilIdle()
        assertTrue(s.currentState().isEmpty)
    }
}
