package com.ttcn.promotionsdk

import com.ttcn.promotionsdk.config.PromotionSDKConfig
import com.ttcn.promotionsdk.di.PromotionContainer
import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffersResult
import com.ttcn.promotionsdk.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.domain.model.redemption.CreateRedemptionResult
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsResult
import com.ttcn.promotionsdk.domain.model.voucher.SearchCustomerVouchersResult
import com.ttcn.promotionsdk.domain.model.voucher.VoucherDetail
import com.ttcn.promotionsdk.domain.repository.PromotionRepository
import com.ttcn.promotionsdk.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.domain.usecase.ValidateStackableDiscountsUseCase
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionIntent
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionStore
import com.ttcn.promotionsdk.presentation.endow.EndowIntent
import com.ttcn.promotionsdk.presentation.endow.EndowStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Host **không** truyền `requestContextProvider` (hoặc chưa gọi `updateContext`) ⇒ mọi getter trả
 * `null`. Store phải gửi chuỗi rỗng chứ không được ném NPE — đây là trạng thái thật lúc app vừa
 * khởi động, trước khi vào màn thanh toán.
 */
class NoContextProviderTest {

    private class CapturingRepo : PromotionRepository {
        var last: FindEligibleCampaignsRequest? = null
        var lastValidate: ValidateDiscountsRequest? = null
        override suspend fun searchCustomerVouchers(keyword: String?, serviceCode: String?, tab: String?, page: Int?, size: Int?): SearchCustomerVouchersResult? = null
        override suspend fun getCustomerVoucherDetail(voucherId: String, service: String?): VoucherDetail? = null
        override suspend fun createRedemptionSession(request: CreateRedemptionRequest): CreateRedemptionResult? = null
        override suspend fun validateStackableDiscounts(request: ValidateDiscountsRequest): ValidateDiscountsResult? {
            lastValidate = request; return null
        }
        override suspend fun findEligibleCampaigns(request: FindEligibleCampaignsRequest): EligibleOffersResult? {
            last = request; return EligibleOffersResult()
        }
    }

    // CỐ Ý không truyền requestContextProvider → container dùng provider rỗng mặc định.
    @BeforeTest
    fun setUp() = PromotionContainer.initialize(PromotionSDKConfig(baseUrl = "https://a.example.com"))

    @AfterTest
    fun tearDown() = PromotionContainer.clear()

    @Test
    fun choosePromotion_withoutOrderContext_sendsEmptyStringsNotNull() = runTest {
        val repo = CapturingRepo()
        val s = ChoosePromotionStore(FindEligibleCampaignsUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        s.dispatch(ChoosePromotionIntent.LoadInitial)
        testScheduler.advanceUntilIdle()

        val req = repo.last!!
        assertEquals("", req.orderId)
        assertEquals("", req.orderValue)
        assertTrue(req.items.isEmpty())
    }

    @Test
    fun endow_withoutOrderContext_stillLoads() = runTest {
        val repo = CapturingRepo()
        val s = EndowStore(
            FindEligibleCampaignsUseCase(repo), ValidateStackableDiscountsUseCase(repo),
            CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        )
        s.dispatch(EndowIntent.LoadInitial)
        testScheduler.advanceUntilIdle()

        assertEquals("", repo.last!!.orderId)
        assertEquals("", repo.last!!.orderValue)
        assertTrue(s.currentState().hasLoadedInitial)
    }

    @Test
    fun endowValidate_withoutOrderContext_sendsEmptyStrings() = runTest {
        val repo = CapturingRepo()
        val s = EndowStore(
            FindEligibleCampaignsUseCase(repo), ValidateStackableDiscountsUseCase(repo),
            CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        )
        s.dispatch(EndowIntent.ValidateAndApply(listOf(com.ttcn.promotionsdk.domain.model.eligible.EligibleOffer(id = "a"))))
        testScheduler.advanceUntilIdle()

        assertEquals("", repo.lastValidate!!.orderId)
        assertEquals("", repo.lastValidate!!.orderValue)
    }
}
