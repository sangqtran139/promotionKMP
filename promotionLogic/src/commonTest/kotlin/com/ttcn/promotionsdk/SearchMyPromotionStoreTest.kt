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
import com.ttcn.promotionsdk.domain.repository.PromotionRepository
import com.ttcn.promotionsdk.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionAction
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionBadge
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionVoucher
import com.ttcn.promotionsdk.presentation.searchmypromotion.SearchMyPromotionIntent
import com.ttcn.promotionsdk.presentation.searchmypromotion.SearchMyPromotionState
import com.ttcn.promotionsdk.presentation.searchmypromotion.SearchMyPromotionStore
import com.ttcn.promotionsdk.presentation.searchmypromotion.showsNoResult
import com.ttcn.promotionsdk.presentation.searchmypromotion.showsResults
import com.ttcn.promotionsdk.presentation.searchmypromotion.voucher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Khoá hành vi [SearchMyPromotionStore] (tầng UI-logic dùng chung). Debounce + search + phân trang.
 */
class SearchMyPromotionStoreTest {

    private class FakeRepo(var result: (page: Int?) -> SearchCustomerVouchersResult?) : PromotionRepository {
        var calls = 0
        override suspend fun searchCustomerVouchers(
            keyword: String?, serviceCode: String?, tab: String?, page: Int?, size: Int?,
        ): SearchCustomerVouchersResult? { calls++; return result(page) }
        override suspend fun getCustomerVoucherDetail(voucherId: String, service: String?): VoucherDetail? = null
        override suspend fun createRedemptionSession(request: CreateRedemptionRequest): CreateRedemptionResult? = null
        override suspend fun validateStackableDiscounts(request: ValidateDiscountsRequest): ValidateDiscountsResult? = null
        override suspend fun findEligibleCampaigns(request: FindEligibleCampaignsRequest): EligibleOffersResult? = null
    }

    private fun voucher(id: String) = VoucherItem(voucherId = id, status = "ACTIVE", title = "V-$id")
    private fun page(content: List<VoucherItem>, number: Int, last: Boolean) =
        SearchCustomerVouchersResult(content = content, number = number, size = 10, last = last)

    @Test
    fun queryChanged_debounces_thenSearches() = runTest {
        val repo = FakeRepo { page(listOf(voucher("1"), voucher("2")), 0, true) }
        val store = SearchMyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        store.dispatch(SearchMyPromotionIntent.QueryChanged("grab"))
        // Bật loading ngay khi gõ; chưa gọi API (đang chờ debounce).
        assertTrue(store.state.value.isLoading)
        assertEquals(0, repo.calls)

        testScheduler.advanceUntilIdle()   // hết debounce → search
        val s = store.state.value
        assertEquals(listOf("1", "2"), s.vouchers.map { it.source.voucherId })
        assertFalse(s.isLoading)
        assertTrue(s.isLastPage)
    }

    @Test
    fun loadMore_appendsNextPage() = runTest {
        val repo = FakeRepo { page ->
            if (page == 0) page(listOf(voucher("1")), 0, false) else page(listOf(voucher("2")), 1, true)
        }
        val store = SearchMyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        store.dispatch(SearchMyPromotionIntent.QueryChanged("grab"))
        testScheduler.advanceUntilIdle()
        assertEquals(1, store.state.value.vouchers.size)

        store.dispatch(SearchMyPromotionIntent.LoadMore)
        testScheduler.advanceUntilIdle()
        assertEquals(listOf("1", "2"), store.state.value.vouchers.map { it.source.voucherId })
    }

    @Test
    fun clearKeyword_resets() = runTest {
        val repo = FakeRepo { page(listOf(voucher("1")), 0, true) }
        val store = SearchMyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        store.dispatch(SearchMyPromotionIntent.QueryChanged("grab"))
        testScheduler.advanceUntilIdle()
        assertTrue(store.state.value.vouchers.isNotEmpty())

        store.dispatch(SearchMyPromotionIntent.ClearKeyword)
        val s = store.state.value
        assertEquals("", s.keyword)
        assertTrue(s.vouchers.isEmpty())
    }

    // ─── Rule dùng chung cho native: showsNoResult / showsResults / voucher(id) ──

    /**
     * "Không tìm thấy" đòi **có từ khoá**. Trước đây iOS chỉ xét `isEmpty` còn Android đòi thêm độ
     * dài từ khoá — hai bên ra cùng kết quả chỉ nhờ store set `isEmpty = false` lúc reset. Test này
     * khoá luật lại để lần sau đổi `isEmpty` không âm thầm làm lệch một nền tảng.
     */
    @Test
    fun showsNoResult_requiresKeywordAndNotLoading() {
        assertFalse(SearchMyPromotionState(isEmpty = true).showsNoResult())
        assertFalse(SearchMyPromotionState(keyword = "  ", isEmpty = true).showsNoResult())
        assertFalse(SearchMyPromotionState(keyword = "grab", isEmpty = true, isLoading = true).showsNoResult())
        assertTrue(SearchMyPromotionState(keyword = "grab", isEmpty = true).showsNoResult())
    }

    /** Dựng thẳng, không qua mapper: mấy test dưới chỉ quan tâm id và số lượng. */
    private fun listItem(id: String) = MyPromotionVoucher(
        source = voucher(id),
        isEnabled = true,
        expiringInDays = null,
        badge = MyPromotionBadge.NONE,
        action = MyPromotionAction.USE,
    )

    /** Có voucher nhưng chưa gõ gì (danh sách còn sót từ lần trước) thì chưa phải "kết quả tìm kiếm". */
    @Test
    fun showsResults_requiresKeywordAndItems() {
        val items = listOf(listItem("v1"))
        assertFalse(SearchMyPromotionState(vouchers = items).showsResults())
        assertFalse(SearchMyPromotionState(keyword = "grab").showsResults())
        assertTrue(SearchMyPromotionState(keyword = "grab", vouchers = items).showsResults())
    }

    @Test
    fun voucher_findsByIdOrNull() {
        val state = SearchMyPromotionState(vouchers = listOf(listItem("v1"), listItem("v2")))
        assertEquals("v2", state.voucher("v2")?.source?.voucherId)
        assertEquals(null, state.voucher("khong-ton-tai"))
    }

    @Test
    fun error_setsErrorCode() = runTest {
        val repo = FakeRepo { throw RuntimeException("boom") }
        val store = SearchMyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        store.dispatch(SearchMyPromotionIntent.QueryChanged("grab"))
        testScheduler.advanceUntilIdle()
        assertTrue(store.state.value.errorCode != null)

        store.dispatch(SearchMyPromotionIntent.ConsumeError)
        assertEquals(null, store.state.value.errorCode)
    }
}
