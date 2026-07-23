package com.ttcn.promotionsdk.core

import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
import com.ttcn.promotionsdk.core.di.PromotionContainer
import com.ttcn.promotionsdk.core.domain.exception.PromotionException
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffersResult
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOrderItem
import com.ttcn.promotionsdk.core.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionResult
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.DiscountItemResult
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsResult
import com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersResult
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherDetail
import com.ttcn.promotionsdk.core.domain.repository.PromotionRepository
import com.ttcn.promotionsdk.core.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.core.domain.usecase.ValidateStackableDiscountsUseCase
import com.ttcn.promotionsdk.presentation.endow.EndowAppliedDiscount
import com.ttcn.promotionsdk.presentation.endow.EndowIntent
import com.ttcn.promotionsdk.presentation.endow.EndowState
import com.ttcn.promotionsdk.presentation.endow.EndowStore
import com.ttcn.promotionsdk.presentation.endow.EndowWidgetState
import com.ttcn.promotionsdk.presentation.endow.widgetState
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
 * Khoá hành vi [EndowStore] — tầng UI-logic dùng chung của widget "Ưu đãi" ở màn thanh toán.
 * Store đọc order-context từ [PromotionContainer.requestContextProvider] → cần init container.
 */
class EndowStoreTest {

    private class FakeRepo(
        var eligible: () -> EligibleOffersResult? = { null },
        var validate: () -> ValidateDiscountsResult? = { null },
    ) : PromotionRepository {
        /** Request cuối gửi xuống — dùng để khoá phần dựng request từ context. */
        var lastEligibleRequest: FindEligibleCampaignsRequest? = null
        var lastValidateRequest: ValidateDiscountsRequest? = null
        var eligibleCallCount = 0

        override suspend fun searchCustomerVouchers(keyword: String?, serviceCode: String?, tab: String?, page: Int?, size: Int?): SearchCustomerVouchersResult? = null
        override suspend fun getCustomerVoucherDetail(voucherId: String, service: String?): VoucherDetail? = null
        override suspend fun createRedemptionSession(request: CreateRedemptionRequest): CreateRedemptionResult? = null
        override suspend fun validateStackableDiscounts(request: ValidateDiscountsRequest): ValidateDiscountsResult? {
            lastValidateRequest = request
            return validate()
        }
        override suspend fun findEligibleCampaigns(request: FindEligibleCampaignsRequest): EligibleOffersResult? {
            lastEligibleRequest = request
            eligibleCallCount++
            return eligible()
        }
    }

    private class Ctx : PromotionRequestContextProvider {
        override fun getOrderId(): String = "ORD-1"
        override fun getOrderValue(): String = "500000"
        override fun getOrderItems(): List<EligibleOrderItem> =
            listOf(EligibleOrderItem(skuId = "SKU-1", quantity = 2, unitPrice = "250000"))
    }

    @BeforeTest
    fun setUp() = PromotionContainer.initialize(PromotionSDKConfig(baseUrl = "https://a.example.com", requestContextProvider = Ctx()))

    @AfterTest
    fun tearDown() = PromotionContainer.clear()

    private fun store(repo: FakeRepo) = EndowStore(
        findEligibleCampaignsUseCase = FindEligibleCampaignsUseCase(repo),
        validateStackableDiscountsUseCase = ValidateStackableDiscountsUseCase(repo),
        scope = CoroutineScope(UnconfinedTestDispatcher()),
    )

    private fun offer(id: String, usable: Boolean = true, type: String = "CAMPAIGN") =
        EligibleOffer(id = id, objectType = type, usable = usable)

    // ─── LoadInitial ──────────────────────────────────────────────────────────

    @Test
    fun loadInitial_success_setsOffersAndTotalFromTotalElements() = runTest {
        val repo = FakeRepo(eligible = {
            EligibleOffersResult(
                myOffers = listOf(offer("a")),
                otherOffers = listOf(offer("b")),
                myTotalElements = 7,
                otherTotalElements = 5,
            )
        })
        val s = store(repo)
        s.dispatch(EndowIntent.LoadInitial)

        val st = s.currentState()
        assertTrue(st.hasLoadedInitial)
        assertFalse(st.isLoading)
        assertEquals(listOf("a"), st.myOffers.map { it.id })
        assertEquals(listOf("b"), st.otherOffers.map { it.id })
        // Đếm theo totalElements của server, KHÔNG theo số phần tử đã phân trang.
        assertEquals(12, st.totalVoucherCount)
        assertNull(st.errorCode)
    }

    @Test
    fun loadInitial_buildsRequestFromContextProvider() = runTest {
        val repo = FakeRepo(eligible = { EligibleOffersResult() })
        store(repo).dispatch(EndowIntent.LoadInitial)

        val req = repo.lastEligibleRequest!!
        assertEquals("ORD-1", req.orderId)
        assertEquals("500000", req.orderValue)
        assertEquals(listOf("SKU-1"), req.items.map { it.skuId })
    }

    @Test
    fun loadInitial_isIdempotent_secondDispatchDoesNotRefetch() = runTest {
        val repo = FakeRepo(eligible = { EligibleOffersResult(myTotalElements = 1) })
        val s = store(repo)
        s.dispatch(EndowIntent.LoadInitial)
        s.dispatch(EndowIntent.LoadInitial)
        assertEquals(1, repo.eligibleCallCount)
    }

    @Test
    fun loadInitial_noTotalElements_fallsBackToListSize() = runTest {
        val repo = FakeRepo(eligible = {
            EligibleOffersResult(myOffers = listOf(offer("a"), offer("b")), otherOffers = listOf(offer("c")))
        })
        val s = store(repo)
        s.dispatch(EndowIntent.LoadInitial)
        assertEquals(3, s.currentState().totalVoucherCount)
    }

    @Test
    fun loadInitial_failure_setsErrorCodeAndStillMarksLoaded() = runTest {
        val repo = FakeRepo(eligible = { throw PromotionException("BOOM", "loi") })
        val s = store(repo)
        s.dispatch(EndowIntent.LoadInitial)

        val st = s.currentState()
        assertFalse(st.isLoading)
        assertTrue(st.hasLoadedInitial)
        assertEquals("BOOM", st.errorCode)
    }

    // ─── ValidateAndApply ─────────────────────────────────────────────────────

    @Test
    fun validateAndApply_allValid_appliesAndKeepsAvailable() = runTest {
        val repo = FakeRepo(validate = {
            ValidateDiscountsResult(
                overallValid = true, totalDiscountAmount = "1000", finalAmount = "9000",
                items = listOf(DiscountItemResult("a", "CAMPAIGN", true, "1000", "ELIGIBLE")),
            )
        })
        val s = store(repo)
        s.dispatch(EndowIntent.ValidateAndApply(listOf(offer("a"))))

        val st = s.currentState()
        assertFalse(st.isValidating)
        assertEquals(1, st.appliedDiscounts.size)
        assertEquals("1000", st.appliedDiscounts[0].calculatedDiscount)
        assertTrue(st.appliedDiscounts[0].valid)
        assertFalse(st.discountUnavailable)
        assertEquals(EndowWidgetState.APPLIED, st.widgetState)
    }

    @Test
    fun validateAndApply_anyInvalid_marksUnavailable() = runTest {
        val repo = FakeRepo(validate = {
            ValidateDiscountsResult(
                overallValid = false, totalDiscountAmount = "0", finalAmount = "10000",
                items = listOf(
                    DiscountItemResult("a", "CAMPAIGN", true, "1000", "ELIGIBLE"),
                    DiscountItemResult("b", "CAMPAIGN", false, "0", "NOT_ELIGIBLE"),
                ),
            )
        })
        val s = store(repo)
        s.dispatch(EndowIntent.ValidateAndApply(listOf(offer("a"), offer("b"))))

        val st = s.currentState()
        assertTrue(st.discountUnavailable)
        assertEquals(EndowWidgetState.UNAVAILABLE, st.widgetState)
    }

    @Test
    fun validateAndApply_emptyOffers_clearsAppliedWithoutCallingApi() = runTest {
        val repo = FakeRepo(validate = { throw IllegalStateException("khong duoc goi") })
        val s = store(repo)
        s.dispatch(EndowIntent.SetApplied(listOf(applied("a")), unavailable = true))
        s.dispatch(EndowIntent.ValidateAndApply(emptyList()))

        val st = s.currentState()
        assertTrue(st.appliedDiscounts.isEmpty())
        assertFalse(st.discountUnavailable)
        assertNull(repo.lastValidateRequest)
    }

    @Test
    fun validateAndApply_buildsRequestFromOffersAndContext() = runTest {
        val repo = FakeRepo(validate = { ValidateDiscountsResult(true, "0", "0", emptyList()) })
        store(repo).dispatch(EndowIntent.ValidateAndApply(listOf(offer("a", type = "VOUCHER"))))

        val req = repo.lastValidateRequest!!
        assertEquals("ORD-1", req.orderId)
        assertEquals("500000", req.orderValue)
        assertEquals(listOf("a"), req.items.map { it.objectId })
        assertEquals(listOf("VOUCHER"), req.items.map { it.objectType })
    }

    @Test
    fun validateAndApply_failure_setsErrorAndStopsValidating() = runTest {
        val repo = FakeRepo(validate = { throw PromotionException("VAL_ERR", "loi") })
        val s = store(repo)
        s.dispatch(EndowIntent.ValidateAndApply(listOf(offer("a"))))

        val st = s.currentState()
        assertFalse(st.isValidating)
        assertEquals("VAL_ERR", st.errorCode)
        assertTrue(st.appliedDiscounts.isEmpty())
    }

    @Test
    fun validateAndApply_nullResult_appliesNothing() = runTest {
        val repo = FakeRepo(validate = { null })
        val s = store(repo)
        s.dispatch(EndowIntent.ValidateAndApply(listOf(offer("a"))))
        assertTrue(s.currentState().appliedDiscounts.isEmpty())
    }

    // ─── SetApplied / MarkUnavailable / ClearApplied / ConsumeError ────────────

    private fun applied(id: String, valid: Boolean = true) =
        EndowAppliedDiscount(objectId = id, objectType = "CAMPAIGN", valid = valid, calculatedDiscount = "1000", eligibilityStatus = "ELIGIBLE")

    @Test
    fun setApplied_storesDiscountsAndClearsError() = runTest {
        val repo = FakeRepo(eligible = { throw PromotionException("E", "x") })
        val s = store(repo)
        s.dispatch(EndowIntent.LoadInitial)
        assertEquals("E", s.currentState().errorCode)

        s.dispatch(EndowIntent.SetApplied(listOf(applied("a")), unavailable = false))
        val st = s.currentState()
        assertEquals(listOf("a"), st.appliedDiscounts.map { it.objectId })
        assertNull(st.errorCode)
    }

    @Test
    fun markUnavailable_keepsListButFlipsFlag() = runTest {
        val s = store(FakeRepo())
        s.dispatch(EndowIntent.SetApplied(listOf(applied("a")), unavailable = false))
        s.dispatch(EndowIntent.MarkUnavailable)

        val st = s.currentState()
        assertEquals(1, st.appliedDiscounts.size)
        assertTrue(st.discountUnavailable)
    }

    @Test
    fun clearApplied_resetsListAndFlag() = runTest {
        val s = store(FakeRepo())
        s.dispatch(EndowIntent.SetApplied(listOf(applied("a")), unavailable = true))
        s.dispatch(EndowIntent.ClearApplied)

        val st = s.currentState()
        assertTrue(st.appliedDiscounts.isEmpty())
        assertFalse(st.discountUnavailable)
    }

    @Test
    fun consumeError_clearsErrorCode() = runTest {
        val repo = FakeRepo(eligible = { throw PromotionException("E", "x") })
        val s = store(repo)
        s.dispatch(EndowIntent.LoadInitial)
        s.dispatch(EndowIntent.ConsumeError)
        assertNull(s.currentState().errorCode)
    }

    // ─── widgetState — quyết định hiển thị dùng chung 2 nền tảng ───────────────

    @Test
    fun widgetState_empty_whenNothingLoaded() {
        assertEquals(EndowWidgetState.EMPTY, EndowState().widgetState)
    }

    @Test
    fun widgetState_notApplied_whenHasVouchersButNoneApplied() {
        assertEquals(EndowWidgetState.NOT_APPLIED, EndowState(totalVoucherCount = 3).widgetState)
    }

    @Test
    fun widgetState_applied_takesPrecedenceOverCount() {
        val st = EndowState(totalVoucherCount = 3, appliedDiscounts = listOf(applied("a")))
        assertEquals(EndowWidgetState.APPLIED, st.widgetState)
    }

    @Test
    fun widgetState_unavailable_needsBothFlagAndAppliedList() {
        // Cờ bật nhưng chưa áp gì → vẫn theo count, KHÔNG phải UNAVAILABLE.
        assertEquals(
            EndowWidgetState.NOT_APPLIED,
            EndowState(totalVoucherCount = 2, discountUnavailable = true).widgetState,
        )
        assertEquals(
            EndowWidgetState.UNAVAILABLE,
            EndowState(totalVoucherCount = 2, discountUnavailable = true, appliedDiscounts = listOf(applied("a"))).widgetState,
        )
    }

    // ─── watchState / clear ───────────────────────────────────────────────────

    @Test
    fun watchState_emitsCurrentAndSubsequentStates() = runTest {
        val s = store(FakeRepo())
        val seen = mutableListOf<Int>()
        val c = s.watchState { seen += it.appliedDiscounts.size }

        s.dispatch(EndowIntent.SetApplied(listOf(applied("a")), unavailable = false))
        s.dispatch(EndowIntent.ClearApplied)
        c.cancel()

        // Phát state hiện tại lúc đăng ký (0) rồi mỗi lần đổi (1, 0).
        assertEquals(listOf(0, 1, 0), seen)
    }

    @Test
    fun watchState_afterCancel_stopsReceiving() = runTest {
        val s = store(FakeRepo())
        var count = 0
        val c = s.watchState { count++ }
        val atCancel = count
        c.cancel()
        s.dispatch(EndowIntent.SetApplied(listOf(applied("a")), unavailable = false))
        assertEquals(atCancel, count)
    }
}
