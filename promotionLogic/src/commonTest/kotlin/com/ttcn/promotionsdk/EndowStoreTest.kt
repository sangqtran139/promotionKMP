package com.ttcn.promotionsdk

import com.ttcn.promotionsdk.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.config.PromotionSDKConfig
import com.ttcn.promotionsdk.di.PromotionContainer
import com.ttcn.promotionsdk.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.domain.exception.PromotionException
import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffersResult
import com.ttcn.promotionsdk.domain.model.eligible.EligibleOrderItem
import com.ttcn.promotionsdk.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.domain.model.redemption.CreateRedemptionResult
import com.ttcn.promotionsdk.domain.model.stackablediscount.DiscountItemResult
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsResult
import com.ttcn.promotionsdk.domain.model.voucher.SearchCustomerVouchersResult
import com.ttcn.promotionsdk.domain.model.voucher.VoucherDetail
import com.ttcn.promotionsdk.domain.repository.PromotionRepository
import com.ttcn.promotionsdk.domain.model.redemption.RedemptionValidationError
import com.ttcn.promotionsdk.presentation.endow.EndowConfirmResult
import com.ttcn.promotionsdk.domain.usecase.CreateRedemptionSessionUseCase
import com.ttcn.promotionsdk.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.domain.usecase.ValidateStackableDiscountsUseCase
import com.ttcn.promotionsdk.presentation.endow.EndowAppliedDiscount
import com.ttcn.promotionsdk.presentation.endow.EndowApplyOutcome
import com.ttcn.promotionsdk.presentation.endow.EndowIntent
import com.ttcn.promotionsdk.presentation.endow.EndowState
import com.ttcn.promotionsdk.presentation.endow.EndowStore
import com.ttcn.promotionsdk.presentation.endow.EndowWidgetState
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
        var validateCallCount = 0

        override suspend fun searchCustomerVouchers(keyword: String?, serviceCode: String?, tab: String?, page: Int?, size: Int?): SearchCustomerVouchersResult? = null
        override suspend fun getCustomerVoucherDetail(voucherId: String, service: String?): VoucherDetail? = null
        var lastRedemptionRequest: CreateRedemptionRequest? = null
        var redemption: () -> CreateRedemptionResult? = {
            CreateRedemptionResult(sessionId = "S-1", totalDiscount = "0", finalAmount = "0", validationErrors = emptyList())
        }
        override suspend fun createRedemptionSession(request: CreateRedemptionRequest): CreateRedemptionResult? {
            lastRedemptionRequest = request
            return redemption()
        }
        override suspend fun validateStackableDiscounts(request: ValidateDiscountsRequest): ValidateDiscountsResult? {
            lastValidateRequest = request
            validateCallCount++
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
            listOf(EligibleOrderItem(skuSourceId = "SKU-1", quantity = 2, unitPrice = "250000"))
    }

    @BeforeTest
    fun setUp() = PromotionContainer.initialize(PromotionSDKConfig(baseUrl = "https://a.example.com", requestContextProvider = Ctx()))

    @AfterTest
    fun tearDown() = PromotionContainer.clear()

    private fun store(repo: FakeRepo) = EndowStore(
        findEligibleCampaignsUseCase = FindEligibleCampaignsUseCase(repo),
        validateStackableDiscountsUseCase = ValidateStackableDiscountsUseCase(repo),
        createRedemptionSessionUseCase = CreateRedemptionSessionUseCase(repo),
        scope = CoroutineScope(UnconfinedTestDispatcher()),
    )

    private fun redemptionOk(errors: List<RedemptionValidationError> = emptyList()) = CreateRedemptionResult(
        sessionId = "S-1", totalDiscount = "0", finalAmount = "0", validationErrors = errors,
    )

    private fun validateOk(id: String, discount: String, valid: Boolean = true, type: String = "CAMPAIGN") =
        ValidateDiscountsResult(
            overallValid = valid,
            totalDiscountAmount = discount,
            finalAmount = "0",
            items = listOf(
                DiscountItemResult(
                    objectId = id, objectType = type, valid = valid,
                    calculatedDiscount = discount, eligibilityStatus = "OK",
                )
            ),
        )

    private fun offer(id: String, usable: Boolean = true, type: String = "CAMPAIGN") =
        EligibleOffer(id = id, objectType = type, usable = usable)

    // ─── LoadInitial ──────────────────────────────────────────────────────────

    /**
     * Cờ phân trang phải đi vào state: màn "Chọn ưu đãi" đọc lại qua `Preload` để quyết định nút
     * "Xem thêm" và có gọi trang kế không. Trước đây state không giữ nên hai nền tảng truyền cứng.
     */
    @Test
    fun loadInitial_keepsPagingFlagsFromResult() = runTest {
        val repo = FakeRepo(eligible = {
            EligibleOffersResult(
                myOffers = listOf(offer("a")),
                otherOffers = listOf(offer("b")),
                myIsLastPage = false,
                otherIsLastPage = true,
            )
        })
        val s = store(repo)
        s.dispatch(EndowIntent.LoadInitial)

        val st = s.currentState()
        assertFalse(st.myIsLastPage)
        assertTrue(st.otherIsLastPage)
    }

    /** Không có kết quả (API lỗi/null) → coi như hết trang, không mở đường gọi thêm. */
    @Test
    fun loadInitial_nullResult_defaultsPagingFlagsToLastPage() = runTest {
        val s = store(FakeRepo(eligible = { null }))
        s.dispatch(EndowIntent.LoadInitial)

        val st = s.currentState()
        assertTrue(st.myIsLastPage)
        assertTrue(st.otherIsLastPage)
    }

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
        assertEquals(listOf("SKU-1"), req.items.map { it.skuSourceId })
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
        assertEquals(EndowApplyOutcome.Applied, s.validateAndApply(listOf(offer("a"))))

        val st = s.currentState()
        assertFalse(st.isValidating)
        assertEquals(1, st.appliedDiscounts.size)
        assertEquals("1000", st.appliedDiscounts[0].calculatedDiscount)
        assertTrue(st.appliedDiscounts[0].valid)
        assertFalse(st.discountUnavailable)
        assertEquals(EndowWidgetState.APPLIED, st.widgetState)
    }

    /**
     * Có item `valid = false` → **không áp gì cả**: widget giữ nguyên bộ discount cũ, và nơi gọi nhận
     * [EndowApplyOutcome.Rejected] kèm câu của server để màn "Chọn ưu đãi" ở lại + disable item đó.
     *
     * Trước đây nhánh này ghi thẳng `appliedDiscounts` + `discountUnavailable = true` (widget nhảy
     * sang UNAVAILABLE) trong khi màn chọn đóng lại như thành công — user chọn voucher xong thấy nó
     * bị gạch ngang mà không ai nói vì sao.
     */
    @Test
    fun validateAndApply_anyInvalid_rejectsWithReasonAndKeepsPreviousApplied() = runTest {
        val repo = FakeRepo(validate = {
            ValidateDiscountsResult(
                overallValid = false, totalDiscountAmount = "0", finalAmount = "10000",
                items = listOf(
                    DiscountItemResult("a", "CAMPAIGN", true, "1000", "ELIGIBLE"),
                    DiscountItemResult(
                        "b", "CAMPAIGN", false, "0", "NOT_ELIGIBLE",
                        validationMessages = listOf("Voucher khong ap dung cho don nay"),
                    ),
                ),
            )
        })
        val s = store(repo)
        s.dispatch(EndowIntent.SetApplied(listOf(applied("cu")), unavailable = false))

        val outcome = s.validateAndApply(listOf(offer("a"), offer("b")))

        val rejected = outcome as EndowApplyOutcome.Rejected
        assertEquals(listOf("b"), rejected.items.map { it.objectId })
        assertEquals("Voucher khong ap dung cho don nay", rejected.items[0].message)

        val st = s.currentState()
        assertFalse(st.isValidating)
        assertNull(st.errorCode, "bi tu choi khong phai loi ky thuat")
        assertEquals(listOf("cu"), st.appliedDiscounts.map { it.objectId }, "bo dang ap phai giu nguyen")
        assertFalse(st.discountUnavailable)
        assertEquals(EndowWidgetState.APPLIED, st.widgetState)
    }

    /** Server từ chối nhưng không kèm lý do → message rỗng; native tự lùi về câu lỗi chung. */
    @Test
    fun validateAndApply_invalidWithoutMessage_stillRejectsWithEmptyReason() = runTest {
        val repo = FakeRepo(validate = {
            ValidateDiscountsResult(
                overallValid = false, totalDiscountAmount = "0", finalAmount = "10000",
                items = listOf(DiscountItemResult("a", "CAMPAIGN", false, "0", "NOT_ELIGIBLE")),
            )
        })
        val outcome = store(repo).validateAndApply(listOf(offer("a")))

        val rejected = outcome as EndowApplyOutcome.Rejected
        assertEquals("", rejected.items.single().message)
    }

    /** Lý do cấp-đơn (`businessRuleViolations`) cũng phải tới được màn chọn — `reasonFor` đã lùi sẵn. */
    @Test
    fun validateAndApply_invalidWithOrderLevelReason_usesBusinessRuleViolation() = runTest {
        val repo = FakeRepo(validate = {
            ValidateDiscountsResult(
                overallValid = false, totalDiscountAmount = "0", finalAmount = "10000",
                items = listOf(DiscountItemResult("a", "CAMPAIGN", false, "0", "NOT_ELIGIBLE")),
                businessRuleViolations = listOf("Don hang khong du dieu kien"),
            )
        })
        val outcome = store(repo).validateAndApply(listOf(offer("a")))

        assertEquals(
            "Don hang khong du dieu kien",
            (outcome as EndowApplyOutcome.Rejected).items.single().message,
        )
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
        assertEquals(EndowApplyOutcome.Failed("VAL_ERR"), s.validateAndApply(listOf(offer("a"))))

        val st = s.currentState()
        assertFalse(st.isValidating)
        assertEquals("VAL_ERR", st.errorCode)
        assertTrue(st.appliedDiscounts.isEmpty())
    }

    /**
     * Không có kết quả validate → **báo lỗi**, không im lặng coi như áp xong. Nếu chỉ để
     * `appliedDiscounts` rỗng thì widget về "chưa áp gì" còn màn "Chọn ưu đãi" đóng như thành công —
     * user chọn voucher xong thấy widget không đổi mà chẳng có thông báo nào.
     */
    @Test
    fun validateAndApply_nullResult_reportsErrorAndAppliesNothing() = runTest {
        val repo = FakeRepo(validate = { null })
        val s = store(repo)
        assertEquals(
            EndowApplyOutcome.Failed(ErrorCodes.NO_RESULT),
            s.validateAndApply(listOf(offer("a"))),
        )

        val st = s.currentState()
        assertTrue(st.appliedDiscounts.isEmpty())
        assertEquals(ErrorCodes.NO_RESULT, st.errorCode)
        assertFalse(st.isValidating)
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

    // ─── confirmRedemption (luồng checkout — trước đây chỉ có ở Android) ────────

    /** Đơn không áp ưu đãi nào → cho đi tiếp NGAY, không gọi mạng, không phụ thuộc cờ. */
    @Test
    fun confirm_noAppliedDiscount_succeedsWithoutNetwork() = runTest {
        val repo = FakeRepo()
        val s = store(repo)

        assertEquals(EndowConfirmResult.Success, s.confirmRedemption())
        assertNull(repo.lastRedemptionRequest)
    }

    /** Có ưu đãi, redemption sạch → cho đi tiếp; request mang đúng objectId + expectedDiscount. */
    @Test
    fun confirm_cleanRedemption_succeeds_andSendsAppliedItems() = runTest {
        val repo = FakeRepo(validate = { validateOk("a", discount = "1000") })
        val s = store(repo)
        s.dispatch(EndowIntent.ValidateAndApply(listOf(offer("a"))))

        assertEquals(EndowConfirmResult.Success, s.confirmRedemption())

        val req = repo.lastRedemptionRequest!!
        assertEquals("ORD-1", req.orderId)
        assertEquals(listOf("a"), req.items.map { it.objectId })
        assertEquals(listOf("1000"), req.items.map { it.expectedDiscount })
    }

    /**
     * Hết ngân sách báo trong **body**: phải validate lại rồi cập nhật state trước khi báo lỗi —
     * widget hiện giá mới chứ không giữ giá đã sai.
     */
    @Test
    fun confirm_budgetErrorInBody_revalidates_updatesState_andFails() = runTest {
        val repo = FakeRepo(validate = { validateOk("a", discount = "1000") })
        val s = store(repo)
        s.dispatch(EndowIntent.ValidateAndApply(listOf(offer("a"))))

        repo.redemption = { redemptionOk(errors = listOf(RedemptionValidationError(code = "INSUFFICIENT_BUDGET", message = "het"))) }
        repo.validate = { validateOk("a", discount = "0", valid = false) }

        val result = s.confirmRedemption()

        assertEquals(EndowConfirmResult.Failure("INSUFFICIENT_BUDGET"), result)
        val st = s.currentState()
        assertEquals("0", st.appliedDiscounts.single().calculatedDiscount)
        assertTrue(st.discountUnavailable)   // item không còn valid → widget về UNAVAILABLE
    }

    /** Hết ngân sách báo bằng **HTTP 422** thay vì body — cùng nhánh xử lý. */
    @Test
    fun confirm_budgetErrorAsHttp422_revalidates_andFails() = runTest {
        val repo = FakeRepo(validate = { validateOk("a", discount = "1000") })
        val s = store(repo)
        s.dispatch(EndowIntent.ValidateAndApply(listOf(offer("a"))))

        repo.redemption = { throw PromotionException(errorCode = "INSUFFICIENT_BUDGET", message = "het", httpStatus = 422) }
        repo.validate = { validateOk("a", discount = "500") }

        assertEquals(EndowConfirmResult.Failure("INSUFFICIENT_BUDGET"), s.confirmRedemption())
        assertEquals("500", s.currentState().appliedDiscounts.single().calculatedDiscount)
    }

    /** Lỗi khác (không phải hết ngân sách) → báo thẳng mã lỗi, KHÔNG validate lại. */
    @Test
    fun confirm_otherFailure_reportsErrorCode_withoutRevalidate() = runTest {
        val repo = FakeRepo(validate = { validateOk("a", discount = "1000") })
        val s = store(repo)
        s.dispatch(EndowIntent.ValidateAndApply(listOf(offer("a"))))
        val validateCallsBefore = repo.validateCallCount

        repo.redemption = { throw PromotionException(errorCode = "SERVER_ERROR", message = "toang", httpStatus = 500) }

        assertEquals(EndowConfirmResult.Failure("SERVER_ERROR"), s.confirmRedemption())
        assertEquals(validateCallsBefore, repo.validateCallCount)
    }

    /** Revalidate không có kết quả → vẫn báo hết ngân sách, và KHÔNG ghi đè giá đang hiện. */
    @Test
    fun confirm_revalidateReturnsNull_keepsPreviousDiscounts() = runTest {
        val repo = FakeRepo(validate = { validateOk("a", discount = "1000") })
        val s = store(repo)
        s.dispatch(EndowIntent.ValidateAndApply(listOf(offer("a"))))

        repo.redemption = { redemptionOk(errors = listOf(RedemptionValidationError(code = "INSUFFICIENT_BUDGET", message = "het"))) }
        repo.validate = { null }

        assertEquals(EndowConfirmResult.Failure("INSUFFICIENT_BUDGET"), s.confirmRedemption())
        assertEquals("1000", s.currentState().appliedDiscounts.single().calculatedDiscount)
    }
}
