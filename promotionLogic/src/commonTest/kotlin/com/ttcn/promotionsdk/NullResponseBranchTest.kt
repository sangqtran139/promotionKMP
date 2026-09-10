package com.ttcn.promotionsdk

import com.ttcn.promotionsdk.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.config.PromotionSDKConfig
import com.ttcn.promotionsdk.di.PromotionContainer
import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffersResult
import com.ttcn.promotionsdk.domain.model.eligible.EligibleOrderItem
import com.ttcn.promotionsdk.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.domain.model.redemption.CreateRedemptionResult
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsResult
import com.ttcn.promotionsdk.domain.model.voucher.SearchCustomerVouchersResult
import com.ttcn.promotionsdk.domain.model.voucher.VoucherDetail
import com.ttcn.promotionsdk.domain.repository.PromotionRepository
import com.ttcn.promotionsdk.domain.usecase.CreateRedemptionSessionUseCase
import com.ttcn.promotionsdk.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.domain.usecase.ValidateStackableDiscountsUseCase
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionIntent
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionStore
import com.ttcn.promotionsdk.presentation.offerwidget.OfferWidgetIntent
import com.ttcn.promotionsdk.presentation.offerwidget.OfferWidgetStore
import com.ttcn.promotionsdk.presentation.offerwidget.OfferWidgetDisplayState
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionIntent
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionStore
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

/**
 * Server trả **`data: null`** (2xx nhưng rỗng) — mặt CÒN LẠI của mọi `result?.x ?: default` trong
 * các store. Luồng thường không bao giờ chạy nhánh này, nên không test thì nó chỉ nổ ở production.
 *
 * Kỳ vọng chung: KHÔNG crash, KHÔNG treo loading, và rơi về giá trị mặc định an toàn.
 */
class NullResponseBranchTest {

    /** Repository trả null cho mọi lời gọi — mô phỏng `{"status":200,"success":true,"data":null}`. */
    private class NullRepo : PromotionRepository {
        var calls = 0
        override suspend fun searchCustomerVouchers(keyword: String?, serviceCode: String?, tab: String?, page: Int?, size: Int?): SearchCustomerVouchersResult? { calls++; return null }
        override suspend fun getCustomerVoucherDetail(voucherId: String, service: String?): VoucherDetail? = null
        override suspend fun createRedemptionSession(request: CreateRedemptionRequest): CreateRedemptionResult? = null
        override suspend fun validateStackableDiscounts(request: ValidateDiscountsRequest): ValidateDiscountsResult? { calls++; return null }
        override suspend fun findEligibleCampaigns(request: FindEligibleCampaignsRequest): EligibleOffersResult? { calls++; return null }
    }

    private class Ctx : PromotionRequestContextProvider {
        override fun getOrderId(): String = "O"
        override fun getOrderValue(): String = "1"
    }

    @BeforeTest
    fun setUp() = PromotionContainer.initialize(PromotionSDKConfig(baseUrl = "https://a.example.com", requestContextProvider = Ctx()))

    @AfterTest
    fun tearDown() = PromotionContainer.clear()

    // ─── MyPromotionStore ─────────────────────────────────────────────────────

    @Test
    fun myPromotion_nullResponse_fallsBackToDefaults() = runTest {
        val repo = NullRepo()
        val s = MyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        s.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        testScheduler.advanceUntilIdle()

        val st = s.currentState()
        assertTrue(st.hasLoadedInitial)
        assertFalse(st.isLoading)
        assertTrue(st.vouchers.isEmpty())
        assertTrue(st.isEmpty)
        assertTrue(st.isLastPage)      // `last` null → coi như hết trang
        assertTrue(st.tabs.isEmpty())
        assertNull(st.errorCode)       // null KHÔNG phải lỗi
    }

    @Test
    fun myPromotion_nullResponse_onSelectTab_keepsTabSelected() = runTest {
        val repo = NullRepo()
        val s = MyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        s.dispatch(MyPromotionIntent.SelectTab("t1"))
        testScheduler.advanceUntilIdle()

        val st = s.currentState()
        assertEquals("t1", st.selectedTabCode)   // không có activeTab từ server → giữ tab đã yêu cầu
        assertFalse(st.isRefreshingTab)
        assertFalse(st.isLoading)
    }

    @Test
    fun myPromotion_nullResponse_onRefresh_clearsRefreshingFlag() = runTest {
        val repo = NullRepo()
        val s = MyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        s.dispatch(MyPromotionIntent.Refresh)
        testScheduler.advanceUntilIdle()
        assertFalse(s.currentState().isRefreshing)
    }

    // ─── SearchMyPromotionStore ───────────────────────────────────────────────

    @Test
    fun search_nullResponse_showsEmptyNotError() = runTest {
        val repo = NullRepo()
        val s = SearchMyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        s.dispatch(SearchMyPromotionIntent.QueryChanged("abc"))
        testScheduler.advanceUntilIdle()

        val st = s.currentState()
        assertFalse(st.isLoading)
        assertTrue(st.vouchers.isEmpty())
        assertTrue(st.isEmpty)
        assertTrue(st.isLastPage)
        assertEquals(0, st.page)
        assertNull(st.errorCode)
    }

    @Test
    fun search_nullResponse_onLoadMore_keepsPageStable() = runTest {
        // Trang 0 có dữ liệu (còn trang), trang 1 trả null → không được tăng page bừa.
        var first = true
        val repo = object : PromotionRepository by NullRepo() {
            override suspend fun searchCustomerVouchers(keyword: String?, serviceCode: String?, tab: String?, page: Int?, size: Int?): SearchCustomerVouchersResult? {
                return if (first) {
                    first = false
                    SearchCustomerVouchersResult(content = listOf(), number = 0, size = 10, last = false)
                } else null
            }
        }
        val s = SearchMyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        s.dispatch(SearchMyPromotionIntent.QueryChanged("abc"))
        testScheduler.advanceUntilIdle()
        s.dispatch(SearchMyPromotionIntent.LoadMore)
        testScheduler.advanceUntilIdle()

        assertFalse(s.currentState().isLoadingMore)
        assertTrue(s.currentState().isLastPage)
    }

    // ─── ChoosePromotionStore ─────────────────────────────────────────────────

    @Test
    fun choose_nullResponse_marksEmptyAndLastPage() = runTest {
        val repo = NullRepo()
        val s = ChoosePromotionStore(FindEligibleCampaignsUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        s.dispatch(ChoosePromotionIntent.LoadInitial)
        testScheduler.advanceUntilIdle()

        val st = s.currentState()
        assertTrue(st.hasLoadedInitial)
        assertFalse(st.isLoading)
        assertTrue(st.myOffers.isEmpty())
        assertTrue(st.otherOffers.isEmpty())
        assertTrue(st.isEmpty)
        assertTrue(st.myIsLastPage)
        assertTrue(st.otherIsLastPage)
        assertNull(st.selectedTabCode)
        assertNull(st.expireWarningDate)
    }

    @Test
    fun choose_nullResponse_onLoadMore_stopsLoadingFlags() = runTest {
        val repo = NullRepo()
        val s = ChoosePromotionStore(FindEligibleCampaignsUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        // Preload để có trang kế, rồi loadMore gặp null.
        s.dispatch(ChoosePromotionIntent.Preload(emptyList(), listOf(com.ttcn.promotionsdk.domain.model.eligible.EligibleOffer(id = "b")), myIsLastPage = true, otherIsLastPage = false))
        s.dispatch(ChoosePromotionIntent.LoadMoreOtherVouchers)
        testScheduler.advanceUntilIdle()

        val st = s.currentState()
        assertFalse(st.isLoadingMoreOther)
        assertTrue(st.otherIsLastPage)   // null → mặc định hết trang, không lặp vô hạn
    }

    // ─── OfferWidgetStore ───────────────────────────────────────────────────────────

    @Test
    fun offerWidget_nullResponse_countsZeroAndStaysEmpty() = runTest {
        val repo = NullRepo()
        val s = OfferWidgetStore(
            FindEligibleCampaignsUseCase(repo), ValidateStackableDiscountsUseCase(repo),
            CreateRedemptionSessionUseCase(repo),
            CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        )
        s.dispatch(OfferWidgetIntent.LoadInitial)
        testScheduler.advanceUntilIdle()

        val st = s.currentState()
        assertTrue(st.hasLoadedInitial)
        assertFalse(st.isLoading)
        assertEquals(0, st.totalVoucherCount)
        assertEquals(OfferWidgetDisplayState.EMPTY, st.widgetState)
        assertNull(st.errorCode)
    }

    @Test
    fun offerWidget_nullValidateResult_appliesNothingAndStaysAvailable() = runTest {
        val repo = NullRepo()
        val s = OfferWidgetStore(
            FindEligibleCampaignsUseCase(repo), ValidateStackableDiscountsUseCase(repo),
            CreateRedemptionSessionUseCase(repo),
            CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        )
        s.dispatch(OfferWidgetIntent.ValidateAndApply(listOf(com.ttcn.promotionsdk.domain.model.eligible.EligibleOffer(id = "a"))))
        testScheduler.advanceUntilIdle()

        val st = s.currentState()
        assertFalse(st.isValidating)
        assertTrue(st.appliedDiscounts.isEmpty())
        assertFalse(st.discountUnavailable)   // không có kết quả ⇒ không đánh dấu "không khả dụng"
    }

    // ─── Request mapper: có / không có order items ────────────────────────────

    @Test
    fun eligibleRequest_withoutItems_stillValid() = runTest {
        val repo = NullRepo()
        val res = FindEligibleCampaignsUseCase(repo).invoke(
            FindEligibleCampaignsRequest(orderId = "O", orderValue = "1", items = emptyList()),
        )
        assertNull(res)
        assertEquals(1, repo.calls)
    }

    @Test
    fun eligibleRequest_withFullItems_stillValid() = runTest {
        val repo = NullRepo()
        FindEligibleCampaignsUseCase(repo).invoke(
            FindEligibleCampaignsRequest(
                orderId = "O", orderValue = "1",
                items = listOf(EligibleOrderItem(skuSourceId = "S", quantity = 1, unitPrice = "1", orderItemId = "OI", productId = "P", productName = "N", productCategory = "C")),
                keyword = "kw", tabCode = "t",
            ),
        )
        assertEquals(1, repo.calls)
    }
}
