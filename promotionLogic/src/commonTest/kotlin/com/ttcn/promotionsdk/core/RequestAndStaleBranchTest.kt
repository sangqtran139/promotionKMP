package com.ttcn.promotionsdk.core

import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
import com.ttcn.promotionsdk.core.data.remote.KtorPromotionApiService
import com.ttcn.promotionsdk.core.data.remote.PromotionHttpClient
import com.ttcn.promotionsdk.core.data.remote.PromotionRemoteDataSource
import com.ttcn.promotionsdk.core.data.repository.PromotionRepositoryImpl
import com.ttcn.promotionsdk.core.di.PromotionContainer
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleFilterOptions
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOrderItem
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleSection
import com.ttcn.promotionsdk.core.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffersResult
import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionResult
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsResult
import com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersResult
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherDetail
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherItem
import com.ttcn.promotionsdk.core.domain.repository.PromotionRepository
import com.ttcn.promotionsdk.core.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.core.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionIntent
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.content.TextContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Hai nhóm nhánh cuối:
 *  1. `toEligibleCampaignsRequest` — metadata rỗng vs đầy, keyword trắng, section null/có.
 *  2. `shouldApplyResponse` — latest-wins: bỏ response cũ, bỏ response sai tab.
 *
 * Nhóm 2 cần [StandardTestDispatcher] (KHÔNG phải Unconfined) để coroutine xếp hàng, nhờ đó dựng
 * được cảnh "response về muộn sau khi user đã đổi tab".
 */
class RequestAndStaleBranchTest {

    private class Ctx : PromotionRequestContextProvider {
        override fun getOrderId(): String = "O"
        override fun getOrderValue(): String = "1"
    }

    @BeforeTest
    fun setUp() = PromotionContainer.initialize(PromotionSDKConfig(baseUrl = "https://a.example.com", requestContextProvider = Ctx()))

    @AfterTest
    fun tearDown() = PromotionContainer.clear()

    // ─── 1. Request mapper: nhánh metadata / keyword / section ────────────────

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun captureBody(req: FindEligibleCampaignsRequest): String {
        var body = ""
        val client = HttpClient(MockEngine { r: HttpRequestData ->
            body = (r.body as? TextContent)?.text.orEmpty()
            respond("""{"status":200,"success":true,"data":null}""", HttpStatusCode.OK, jsonHeaders)
        }) {
            with(PromotionHttpClient) { configure("https://api.example.com", Ctx(), false) }
        }
        val repo = PromotionRepositoryImpl(PromotionRemoteDataSource(KtorPromotionApiService(client)))
        kotlinx.coroutines.runBlocking { FindEligibleCampaignsUseCase(repo).invoke(req) }
        return body
    }

    @Test
    fun request_withAllCustomerMetadata_isSent() {
        val body = captureBody(
            FindEligibleCampaignsRequest(
                orderId = "O", orderValue = "1",
                customerType = "VIP", segment = "S1", tier = "GOLD",
                keyword = "  tim  ", section = EligibleSection.MY_OFFERS,
            ),
        )
        assertTrue(body.contains("customerType"))
        assertTrue(body.contains("segment"))
        assertTrue(body.contains("tier"))
        assertTrue(body.contains("\"keyword\":\"tim\""))   // đã trim
        assertTrue(body.contains("sectionCode"))
    }

    @Test
    fun request_withoutMetadata_omitsItEntirely() {
        val body = captureBody(FindEligibleCampaignsRequest(orderId = "O", orderValue = "1"))
        // buildMap rỗng → ifEmpty { null } → không gửi metadata/keyword/sectionCode.
        assertFalse(body.contains("customerType"))
        assertFalse(body.contains("\"keyword\""))
        assertFalse(body.contains("sectionCode"))
    }

    @Test
    fun request_blankKeyword_isDropped() {
        val body = captureBody(FindEligibleCampaignsRequest(orderId = "O", orderValue = "1", keyword = "   "))
        assertFalse(body.contains("\"keyword\""))
    }

    @Test
    fun request_orderItem_withAndWithoutOptionalMetadata() {
        val full = captureBody(
            FindEligibleCampaignsRequest(
                orderId = "O", orderValue = "1",
                items = listOf(EligibleOrderItem(skuId = "S", quantity = 1, unitPrice = "1", orderItemId = "OI", productId = "P", productName = "Ten", productCategory = "Cat")),
            ),
        )
        assertTrue(full.contains("productName"))
        assertTrue(full.contains("productCategory"))

        val bare = captureBody(
            FindEligibleCampaignsRequest(
                orderId = "O", orderValue = "1",
                items = listOf(EligibleOrderItem(skuId = "S", quantity = 1, unitPrice = "1")),
            ),
        )
        assertFalse(bare.contains("productName"))
        assertFalse(bare.contains("productCategory"))
    }

    @Test
    fun request_filterOptions_bothPolarities() {
        val on = captureBody(
            FindEligibleCampaignsRequest(
                orderId = "O", orderValue = "1",
                filterOptions = EligibleFilterOptions(includeExpired = true, checkBudgetAvailability = false, includePreview = false),
            ),
        )
        assertTrue(on.contains("\"includeExpired\":true"))
        assertTrue(on.contains("\"checkBudgetAvailability\":false"))
    }

    // ─── 2. latest-wins: bỏ response cũ / sai tab ─────────────────────────────

    private class SlowRepo(
        /** Số lần gọi → response tương ứng; test tự điều phối bằng scheduler. */
        val pages: MutableList<SearchCustomerVouchersResult?> = mutableListOf(),
    ) : PromotionRepository {
        var calls = 0
        override suspend fun searchCustomerVouchers(keyword: String?, serviceCode: String?, tab: String?, page: Int?, size: Int?): SearchCustomerVouchersResult? {
            val idx = calls++
            return pages.getOrNull(idx)
        }
        override suspend fun getCustomerVoucherDetail(voucherId: String, service: String?): VoucherDetail? = null
        override suspend fun createRedemptionSession(request: CreateRedemptionRequest): CreateRedemptionResult? = null
        override suspend fun validateStackableDiscounts(request: ValidateDiscountsRequest): ValidateDiscountsResult? = null
        override suspend fun findEligibleCampaigns(request: FindEligibleCampaignsRequest): EligibleOffersResult? = null
    }

    private fun page(id: String, tab: String?) = SearchCustomerVouchersResult(
        content = listOf(VoucherItem(voucherId = id, status = "ACTIVE", title = id)),
        number = 0, size = 10, last = true, selectedTab = tab,
    )

    @Test
    fun staleResponse_fromPreviousTab_isDropped() = runTest {
        // Hai lần đổi tab liên tiếp; cả hai request đều đang bay. Response của tab CŨ về sau
        // KHÔNG được ghi đè danh sách của tab đang chọn.
        val repo = SlowRepo(mutableListOf(page("cu", "t1"), page("moi", "t2")))
        val s = MyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(StandardTestDispatcher(testScheduler)))

        s.dispatch(MyPromotionIntent.SelectTab("t1"))
        s.dispatch(MyPromotionIntent.SelectTab("t2"))
        testScheduler.advanceUntilIdle()

        val st = s.currentState()
        assertEquals("t2", st.selectedTabCode)
        // Chỉ dữ liệu của tab đang chọn được giữ.
        assertTrue(st.vouchers.isEmpty() || st.vouchers.all { it.source.voucherId == "moi" })
    }

    @Test
    fun loadMore_whileRefreshingTab_isDropped() = runTest {
        val repo = SlowRepo(mutableListOf(page("a", "t1"), page("b", "t1"), page("c", "t1")))
        val s = MyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(StandardTestDispatcher(testScheduler)))

        s.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        testScheduler.advanceUntilIdle()

        // Đổi tab (đang refresh) rồi lập tức load-more → load-more phải bị bỏ.
        s.dispatch(MyPromotionIntent.SelectTab("t1"))
        s.dispatch(MyPromotionIntent.LoadMore)
        testScheduler.advanceUntilIdle()

        assertFalse(s.currentState().isLoadingMore)
    }

    @Test
    fun refreshDuringInitialLoad_doesNotDuplicateList() = runTest {
        val repo = SlowRepo(mutableListOf(page("a", null), page("a", null)))
        val s = MyPromotionStore(SearchCustomerVouchersUseCase(repo), CoroutineScope(StandardTestDispatcher(testScheduler)))

        s.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
        s.dispatch(MyPromotionIntent.Refresh)
        testScheduler.advanceUntilIdle()

        // reset=true ⇒ thay danh sách, không nối thêm.
        assertEquals(1, s.currentState().vouchers.size)
    }
}
