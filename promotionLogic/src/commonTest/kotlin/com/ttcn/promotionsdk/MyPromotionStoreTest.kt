package com.ttcn.promotionsdk

import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffersResult
import com.ttcn.promotionsdk.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.domain.model.redemption.CreateRedemptionResult
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsResult
import com.ttcn.promotionsdk.domain.model.voucher.SearchCustomerVouchersResult
import com.ttcn.promotionsdk.domain.model.voucher.VoucherDetail
import com.ttcn.promotionsdk.domain.model.voucher.VoucherItem
import com.ttcn.promotionsdk.domain.model.voucher.VoucherTabItem
import com.ttcn.promotionsdk.domain.repository.PromotionRepository
import com.ttcn.promotionsdk.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionIntent
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Khoá hành vi tầng UI-logic dùng chung [MyPromotionStore] — chạy trên cả Android & iOS.
 *
 * Store gom paging/tab/cache/latest-wins; test này giữ cho logic không lệch khi hai nền tảng
 * cùng bọc nó.
 */
class MyPromotionStoreTest {

    /** Repository giả: trả trang theo (tab, page) do test nạp; đếm số lần gọi. */
    private class FakeRepo(
        var result: (tab: String?, page: Int?) -> SearchCustomerVouchersResult?,
    ) : PromotionRepository {
        var calls = 0
        override suspend fun searchCustomerVouchers(
            keyword: String?, serviceCode: String?, tab: String?, page: Int?, size: Int?,
        ): SearchCustomerVouchersResult? {
            calls++
            return result(tab, page)
        }
        override suspend fun getCustomerVoucherDetail(voucherId: String, service: String?): VoucherDetail? = null
        override suspend fun createRedemptionSession(request: CreateRedemptionRequest): CreateRedemptionResult? = null
        override suspend fun validateStackableDiscounts(request: ValidateDiscountsRequest): ValidateDiscountsResult? = null
        override suspend fun findEligibleCampaigns(request: FindEligibleCampaignsRequest): EligibleOffersResult? = null
    }

    private fun voucher(id: String) = VoucherItem(voucherId = id, status = "ACTIVE", title = "V-$id")

    private fun page(
        content: List<VoucherItem>, number: Int, last: Boolean,
        tabs: List<VoucherTabItem> = emptyList(), selectedTab: String? = null,
    ) = SearchCustomerVouchersResult(
        tabs = tabs, selectedTab = selectedTab, content = content, number = number, size = 10, last = last,
    )

    @Test
    fun loadInitial_populatesVouchers_andResolvesActiveTab() = runTest {
        val repo = FakeRepo { _, _ ->
            page(
                content = listOf(voucher("1"), voucher("2")),
                number = 0, last = true,
                tabs = listOf(VoucherTabItem("all", "Tất cả", order = 0), VoucherTabItem("used", "Đã dùng", order = 1)),
                selectedTab = "used",
            )
        }
        val store = MyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        store.dispatch(MyPromotionIntent.LoadInitialIfNeeded)

        val s = store.state.value
        assertEquals(2, s.vouchers.size)
        assertEquals("used", s.selectedTabCode)   // resolveActiveTab: selectedTab thắng
        assertEquals(2, s.tabs.size)
        assertTrue(s.hasLoadedInitial)
        assertFalse(s.isLoading)
        assertTrue(s.isLastPage)
    }

    @Test
    fun loadMore_appendsNextPage() = runTest {
        val repo = FakeRepo { _, page ->
            when (page) {
                0 -> page(listOf(voucher("1")), number = 0, last = false)
                else -> page(listOf(voucher("2")), number = 1, last = true)
            }
        }
        val store = MyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        store.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        assertEquals(1, store.state.value.vouchers.size)
        assertFalse(store.state.value.isLastPage)

        store.dispatch(MyPromotionIntent.LoadMore)

        val s = store.state.value
        assertEquals(listOf("1", "2"), s.vouchers.map { it.source.voucherId })
        assertTrue(s.isLastPage)
    }

    @Test
    fun loadMore_noOp_whenLastPage() = runTest {
        val repo = FakeRepo { _, _ -> page(listOf(voucher("1")), number = 0, last = true) }
        val store = MyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        store.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        val callsAfterInitial = repo.calls

        store.dispatch(MyPromotionIntent.LoadMore)

        assertEquals(callsAfterInitial, repo.calls)  // isLastPage → không gọi thêm
    }

    @Test
    fun error_setsErrorCode_thenConsumeClears() = runTest {
        val repo = FakeRepo { _, _ -> throw RuntimeException("boom") }
        val store = MyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        store.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        assertTrue(store.state.value.errorCode != null)
        assertTrue(store.state.value.hasLoadedInitial)

        store.dispatch(MyPromotionIntent.ConsumeError)
        assertEquals(null, store.state.value.errorCode)
    }

    @Test
    fun selectTab_secondVisit_servesFromCache() = runTest {
        val repo = FakeRepo { tab, _ ->
            page(listOf(voucher("$tab-1")), number = 0, last = true,
                tabs = listOf(VoucherTabItem("all", "Tất cả", order = 0), VoucherTabItem("used", "Đã dùng", order = 1)))
        }
        val store = MyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        store.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        store.dispatch(MyPromotionIntent.SelectTab("used"))
        store.dispatch(MyPromotionIntent.SelectTab("all"))
        // Quay lại "all": hiện ngay từ cache (state đổi trước khi network xong).
        assertEquals("all", store.state.value.selectedTabCode)
        assertTrue(store.state.value.vouchers.isNotEmpty())
    }

    // ─── Prefetch tab + cache TTL ─────────────────────────────────────────────

    private val twoTabs = listOf(
        VoucherTabItem("all", "Tất cả", order = 0),
        VoucherTabItem("used", "Đã dùng", order = 1),
    )

    @Test
    fun loadInitial_prefetchesOtherTabs_soSwitchingCostsNoRequest() = runTest {
        val repo = FakeRepo { tab, _ ->
            page(listOf(voucher("$tab-1")), number = 0, last = true, tabs = twoTabs, selectedTab = "all")
        }
        val store = MyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        store.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        testScheduler.advanceUntilIdle()
        assertEquals(2, repo.calls)   // 1 tab đang mở + 1 prefetch tab còn lại

        store.dispatch(MyPromotionIntent.SelectTab("used"))
        testScheduler.advanceUntilIdle()

        assertEquals(2, repo.calls)   // cache còn tươi → KHÔNG gọi thêm request nào
        assertEquals("used", store.state.value.selectedTabCode)
        assertTrue(store.state.value.vouchers.isNotEmpty())
        assertFalse(store.state.value.isRefreshingTab)
    }

    @Test
    fun prefetchFails_tabStillLoadsOnTap_andShowsNoError() = runTest {
        var prefetchDown = true
        val repo = FakeRepo { tab, _ ->
            if (tab == "used" && prefetchDown) throw RuntimeException("prefetch down")
            page(listOf(voucher("$tab-1")), number = 0, last = true, tabs = twoTabs, selectedTab = "all")
        }
        val store = MyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        store.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        testScheduler.advanceUntilIdle()
        // Prefetch hỏng phải im lặng: tab user chưa bấm vào, không được bắn lỗi.
        assertEquals(null, store.state.value.errorCode)

        prefetchDown = false
        store.dispatch(MyPromotionIntent.SelectTab("used"))
        testScheduler.advanceUntilIdle()

        assertEquals("used", store.state.value.selectedTabCode)
        assertTrue(store.state.value.vouchers.isNotEmpty())   // không cache → load bình thường
    }

    @Test
    fun refresh_marksEveryTabStale_soOtherTabsAreRefetched() = runTest {
        val repo = FakeRepo { tab, _ ->
            page(listOf(voucher("$tab-1")), number = 0, last = true, tabs = twoTabs, selectedTab = "all")
        }
        val store = MyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        store.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        testScheduler.advanceUntilIdle()

        // Kéo làm mới = làm mới CẢ MÀN: tab đang mở + prefetch lại tab kia (cache bị ép ôi).
        store.dispatch(MyPromotionIntent.Refresh)
        testScheduler.advanceUntilIdle()
        assertEquals(4, repo.calls)

        // Tab kia vừa được nạp lại → lại tươi → bấm sang không tốn request.
        store.dispatch(MyPromotionIntent.SelectTab("used"))
        testScheduler.advanceUntilIdle()
        assertEquals(4, repo.calls)
    }

    @Test
    fun selectTab_apiFails_clearsList_insteadOfKeepingPreviousTab() = runTest {
        val tabs = listOf(VoucherTabItem("all", "Tất cả", order = 0), VoucherTabItem("used", "Đã dùng", order = 1))
        val repo = FakeRepo { tab, _ ->
            if (tab == "used") throw RuntimeException("network down")
            page(listOf(voucher("all-1")), number = 0, last = true, tabs = tabs, selectedTab = "all")
        }
        val store = MyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        store.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        assertTrue(store.state.value.vouchers.isNotEmpty())   // tab "all" có dữ liệu

        // Đổi sang tab chưa có cache, API hỏng.
        store.dispatch(MyPromotionIntent.SelectTab("used"))

        val s = store.state.value
        assertEquals("used", s.selectedTabCode)
        assertTrue(s.vouchers.isEmpty())    // KHÔNG mang nguyên list tab "all" sang
        assertTrue(s.isEmpty)               // phải hiện empty state
        assertFalse(s.isLoading)
        assertTrue(s.errorCode != null)     // vẫn báo lỗi (native hiện toast)
    }

    @Test
    fun refreshTab_apiFails_keepsCachedListOfSameTab() = runTest {
        var failNow = false
        val repo = FakeRepo { _, _ ->
            if (failNow) throw RuntimeException("network down")
            page(listOf(voucher("all-1")), number = 0, last = true,
                tabs = listOf(VoucherTabItem("all", "Tất cả", order = 0)), selectedTab = "all")
        }
        val store = MyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        store.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        failNow = true
        store.dispatch(MyPromotionIntent.Refresh)

        // Cùng tab + đã có cache → giữ dữ liệu cũ, chỉ báo lỗi. Khác hẳn ca đổi tab ở trên.
        val s = store.state.value
        assertTrue(s.vouchers.isNotEmpty())
        assertFalse(s.isEmpty)
        assertTrue(s.errorCode != null)
    }
}
