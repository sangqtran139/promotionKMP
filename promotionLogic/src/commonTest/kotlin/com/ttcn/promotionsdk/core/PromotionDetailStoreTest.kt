package com.ttcn.promotionsdk.core

import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
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
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherStatus
import com.ttcn.promotionsdk.core.domain.repository.PromotionRepository
import com.ttcn.promotionsdk.core.domain.usecase.GetCustomerVoucherDetailUseCase
import com.ttcn.promotionsdk.presentation.promotiondetail.PromotionDetailIntent
import com.ttcn.promotionsdk.presentation.promotiondetail.PromotionDetailStore
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
 * Khoá hành vi [PromotionDetailStore]: fetch chi tiết + quyết định nút "Dùng ngay" từ trạng thái.
 * Store đọc `service` từ [PromotionContainer.requestContextProvider] → cần init container.
 */
class PromotionDetailStoreTest {

    private class FakeRepo(var detail: () -> VoucherDetail?) : PromotionRepository {
        override suspend fun searchCustomerVouchers(keyword: String?, serviceCode: String?, tab: String?, page: Int?, size: Int?): SearchCustomerVouchersResult? = null
        override suspend fun getCustomerVoucherDetail(voucherId: String, service: String?): VoucherDetail? = detail()
        override suspend fun createRedemptionSession(request: CreateRedemptionRequest): CreateRedemptionResult? = null
        override suspend fun validateStackableDiscounts(request: ValidateDiscountsRequest): ValidateDiscountsResult? = null
        override suspend fun findEligibleCampaigns(request: FindEligibleCampaignsRequest): EligibleOffersResult? = null
    }

    private class Ctx : PromotionRequestContextProvider {
        override fun getService(): String = "svc"
    }

    @BeforeTest
    fun setUp() = PromotionContainer.initialize(PromotionSDKConfig(baseUrl = "https://a.example.com", requestContextProvider = Ctx()))

    @AfterTest
    fun tearDown() = PromotionContainer.clear()

    private fun store(repo: FakeRepo) =
        PromotionDetailStore(GetCustomerVoucherDetailUseCase(repo), CoroutineScope(UnconfinedTestDispatcher()))

    @Test
    fun loadDetail_usable_showsActionEnabled() = runTest {
        val s = store(FakeRepo { VoucherDetail(voucherId = "v-1", title = "T", status = "ACTIVE") })
        s.dispatch(PromotionDetailIntent.LoadDetail("v-1"))

        val st = s.state.value
        assertEquals("v-1", st.detail?.voucherId)
        assertEquals(VoucherStatus.ACTIVE, st.status)
        assertTrue(st.actionVisible)
        assertTrue(st.actionEnabled)
        assertFalse(st.isLoading)
    }

    @Test
    fun loadDetail_notUsable_hidesAction_usesServerLabel() = runTest {
        val s = store(FakeRepo { VoucherDetail(voucherId = "v-2", status = "EXPIRED", displayStatusLabel = "Hết hạn") })
        s.dispatch(PromotionDetailIntent.LoadDetail("v-2"))

        val st = s.state.value
        assertFalse(st.actionVisible)
        assertFalse(st.actionEnabled)
        assertEquals("Hết hạn", st.actionLabel)
    }

    @Test
    fun loadDetail_null_setsError() = runTest {
        val s = store(FakeRepo { null })
        s.dispatch(PromotionDetailIntent.LoadDetail("v-3"))
        assertEquals("error_detail_unavailable", s.state.value.errorCode)
    }

    @Test
    fun loadDetail_failure_setsError() = runTest {
        val s = store(FakeRepo { throw RuntimeException("boom") })
        s.dispatch(PromotionDetailIntent.LoadDetail("v-4"))
        assertTrue(s.state.value.errorCode != null)

        s.dispatch(PromotionDetailIntent.ConsumeError)
        assertEquals(null, s.state.value.errorCode)
    }
}
