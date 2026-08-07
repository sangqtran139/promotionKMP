package com.ttcn.promotionsdk

import com.ttcn.promotionsdk.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.config.PromotionSDKConfig
import com.ttcn.promotionsdk.di.PromotionContainer
import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffersResult
import com.ttcn.promotionsdk.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.domain.model.redemption.CreateRedemptionResult
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsResult
import com.ttcn.promotionsdk.domain.model.voucher.SearchCustomerVouchersResult
import com.ttcn.promotionsdk.domain.model.voucher.VoucherDetail
import com.ttcn.promotionsdk.domain.model.voucher.VoucherItem
import com.ttcn.promotionsdk.domain.repository.PromotionRepository
import com.ttcn.promotionsdk.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.presentation.choosepromotion.COLLAPSED_MY_COUNT
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionIntent
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionStore
import com.ttcn.promotionsdk.presentation.searchmypromotion.SearchMyPromotionIntent
import com.ttcn.promotionsdk.presentation.searchmypromotion.SearchMyPromotionStore
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

/** Những nhánh cuối cùng còn sót: phân trang nhóm "của tôi", và các lối vào search ít dùng. */
class RemainingBranchTest {

    private class Repo(
        var eligible: (FindEligibleCampaignsRequest) -> EligibleOffersResult? = { null },
        var vouchers: (Int) -> SearchCustomerVouchersResult? = { null },
    ) : PromotionRepository {
        var eligibleCalls = 0
        var voucherCalls = 0
        override suspend fun searchCustomerVouchers(keyword: String?, serviceCode: String?, tab: String?, page: Int?, size: Int?): SearchCustomerVouchersResult? {
            voucherCalls++; return vouchers(page ?: 0)
        }
        override suspend fun getCustomerVoucherDetail(voucherId: String, service: String?): VoucherDetail? = null
        override suspend fun createRedemptionSession(request: CreateRedemptionRequest): CreateRedemptionResult? = null
        override suspend fun validateStackableDiscounts(request: ValidateDiscountsRequest): ValidateDiscountsResult? = null
        override suspend fun findEligibleCampaigns(request: FindEligibleCampaignsRequest): EligibleOffersResult? {
            eligibleCalls++; return eligible(request)
        }
    }

    private class Ctx : PromotionRequestContextProvider {
        override fun getOrderId(): String = "O"
        override fun getOrderValue(): String = "1"
    }

    @BeforeTest
    fun setUp() = PromotionContainer.initialize(PromotionSDKConfig(baseUrl = "https://a.example.com", requestContextProvider = Ctx()))

    @AfterTest
    fun tearDown() = PromotionContainer.clear()

    private fun choose(repo: Repo) =
        ChoosePromotionStore(FindEligibleCampaignsUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(kotlinx.coroutines.test.TestCoroutineScheduler())))

    // ─── Phân trang nhóm "Ưu đãi của tôi" (trước chỉ test nhóm "khác") ────────

    @Test
    fun loadMoreMy_appendsNextPageIndependently() = runTest {
        val repo = Repo(eligible = { EligibleOffersResult(myOffers = listOf(EligibleOffer(id = "m2")), myIsLastPage = true) })
        val s = ChoosePromotionStore(FindEligibleCampaignsUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        s.dispatch(ChoosePromotionIntent.Preload(listOf(EligibleOffer(id = "m1")), emptyList(), myIsLastPage = false, otherIsLastPage = true))
        s.dispatch(ChoosePromotionIntent.LoadMoreMyVouchers)
        testScheduler.advanceUntilIdle()

        val st = s.currentState()
        assertEquals(listOf("m1", "m2"), st.myOffers.map { it.source.id })
        assertEquals(1, st.myPage)
        assertTrue(st.myIsLastPage)
        assertFalse(st.isLoadingMore)
    }

    @Test
    fun loadMoreMy_ignoredWhileInitialLoading() = runTest {
        // isLoading = true (đang tải trang đầu) ⇒ mọi load-more bị bỏ, không chồng request.
        val repo = Repo(eligible = { EligibleOffersResult(myOffers = listOf(EligibleOffer(id = "a")), myIsLastPage = false) })
        val s = ChoosePromotionStore(FindEligibleCampaignsUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        s.dispatch(ChoosePromotionIntent.LoadInitial)
        s.dispatch(ChoosePromotionIntent.LoadMoreMyVouchers)   // chen vào lúc đang tải
        testScheduler.advanceUntilIdle()
        assertFalse(s.currentState().isLoadingMore)
    }

    @Test
    fun seeMoreMy_noHiddenItemsAndLastPage_collapsesImmediately() = runTest {
        // Ít hơn ngưỡng + hết trang ⇒ rơi thẳng nhánh `else` (thu gọn), không mở, không tải thêm.
        val repo = Repo(eligible = { EligibleOffersResult() })
        val s = ChoosePromotionStore(FindEligibleCampaignsUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        s.dispatch(ChoosePromotionIntent.Preload(listOf(EligibleOffer(id = "a")), emptyList(), myIsLastPage = true, otherIsLastPage = true))
        val before = repo.eligibleCalls
        s.dispatch(ChoosePromotionIntent.SeeMoreMy)
        testScheduler.advanceUntilIdle()

        assertFalse(s.currentState().myExpanded)
        assertEquals(before, repo.eligibleCalls)
    }

    @Test
    fun chooseOffer_unparsableExpiry_leavesDaysNullEvenWhenUsable() = runTest {
        // usable=true, có ngưỡng, nhưng ngày không parse được ⇒ daysUntil null ⇒ không "sắp hết hạn".
        val repo = Repo(eligible = {
            EligibleOffersResult(
                myOffers = listOf(EligibleOffer(id = "a", usable = true, expireDate = "khong-phai-ngay")),
                expireWarningDate = 30,
            )
        })
        val s = ChoosePromotionStore(FindEligibleCampaignsUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        s.dispatch(ChoosePromotionIntent.LoadInitial)
        testScheduler.advanceUntilIdle()
        assertNull(s.currentState().myOffers.single().expiringInDays)
    }

    @Test
    fun chooseOffer_expiryBeyondWindow_leavesDaysNull() = runTest {
        val repo = Repo(eligible = {
            EligibleOffersResult(
                myOffers = listOf(EligibleOffer(id = "a", usable = true, expireDate = "2099-12-31")),
                expireWarningDate = 1,
            )
        })
        val s = ChoosePromotionStore(FindEligibleCampaignsUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        s.dispatch(ChoosePromotionIntent.LoadInitial)
        testScheduler.advanceUntilIdle()
        assertNull(s.currentState().myOffers.single().expiringInDays)
    }

    // ─── SearchMyPromotionStore: các lối vào ít dùng ──────────────────────────

    @Test
    fun search_intentWithBlankKeyword_resetsInsteadOfCallingApi() = runTest {
        val repo = Repo(vouchers = { SearchCustomerVouchersResult(content = listOf(VoucherItem(voucherId = "1", status = "ACTIVE"))) })
        val s = SearchMyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        // Gõ rồi xoá trắng, sau đó bấm nút Tìm ⇒ đi nhánh `trimmed.isEmpty() -> resetSearchResults()`.
        s.dispatch(SearchMyPromotionIntent.QueryChanged("abc"))
        testScheduler.advanceUntilIdle()
        s.dispatch(SearchMyPromotionIntent.QueryChanged(""))
        s.dispatch(SearchMyPromotionIntent.Search)
        testScheduler.advanceUntilIdle()

        val st = s.currentState()
        assertTrue(st.vouchers.isEmpty())
        assertFalse(st.isEmpty)      // xoá trắng KHÔNG phải "không có kết quả"
        assertFalse(st.isLoading)
    }

    @Test
    fun search_loadMoreWhileLoading_isIgnored() = runTest {
        val repo = Repo(vouchers = { SearchCustomerVouchersResult(content = listOf(VoucherItem(voucherId = "1", status = "ACTIVE")), number = 0, size = 10, last = false) })
        val s = SearchMyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        s.dispatch(SearchMyPromotionIntent.QueryChanged("abc"))
        // Chưa hết debounce ⇒ isLoading = true ⇒ load-more phải bị bỏ.
        s.dispatch(SearchMyPromotionIntent.LoadMore)
        assertEquals(0, repo.voucherCalls)
        testScheduler.advanceUntilIdle()
    }

    @Test
    fun search_appendsSecondPage() = runTest {
        val repo = Repo(vouchers = { page ->
            SearchCustomerVouchersResult(
                content = listOf(VoucherItem(voucherId = "v$page", status = "ACTIVE")),
                number = page, size = 10, last = page >= 1,
            )
        })
        val s = SearchMyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        s.dispatch(SearchMyPromotionIntent.QueryChanged("abc"))
        testScheduler.advanceUntilIdle()
        s.dispatch(SearchMyPromotionIntent.LoadMore)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("v0", "v1"), s.currentState().vouchers.map { it.source.voucherId })
        assertTrue(s.currentState().isLastPage)
    }

    @Test
    fun search_loadMoreFailure_keepsExistingResults() = runTest {
        val repo = Repo(vouchers = { page ->
            if (page == 0) SearchCustomerVouchersResult(content = listOf(VoucherItem(voucherId = "v0", status = "ACTIVE")), number = 0, size = 10, last = false)
            else throw com.ttcn.promotionsdk.domain.exception.PromotionException("LM", "x")
        })
        val s = SearchMyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        s.dispatch(SearchMyPromotionIntent.QueryChanged("abc"))
        testScheduler.advanceUntilIdle()
        s.dispatch(SearchMyPromotionIntent.LoadMore)
        testScheduler.advanceUntilIdle()

        val st = s.currentState()
        assertEquals("LM", st.errorCode)
        assertEquals(1, st.vouchers.size)   // lỗi load-more không xoá kết quả đang có
        assertFalse(st.isLoadingMore)
    }

    @Test
    fun search_clearKeywordCancelsPendingDebounce() = runTest {
        val repo = Repo(vouchers = { SearchCustomerVouchersResult(content = listOf(VoucherItem(voucherId = "1", status = "ACTIVE"))) })
        val s = SearchMyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        s.dispatch(SearchMyPromotionIntent.QueryChanged("abc"))
        s.dispatch(SearchMyPromotionIntent.ClearKeyword)   // huỷ debounce đang chờ
        testScheduler.advanceUntilIdle()
        assertEquals(0, repo.voucherCalls)
    }
}
