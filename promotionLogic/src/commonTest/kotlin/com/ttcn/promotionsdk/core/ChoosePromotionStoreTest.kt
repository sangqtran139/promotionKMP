package com.ttcn.promotionsdk.core

import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
import com.ttcn.promotionsdk.core.di.PromotionContainer
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffersResult
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleSection
import com.ttcn.promotionsdk.core.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionResult
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsResult
import com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersResult
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherDetail
import com.ttcn.promotionsdk.core.domain.repository.PromotionRepository
import com.ttcn.promotionsdk.core.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionIntent
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Khoá hành vi [ChoosePromotionStore]: load 2 nhóm, phân trang ĐỘC LẬP, preload, lỗi.
 * Store đọc orderId/orderValue từ [PromotionContainer.requestContextProvider] → cần init container.
 */
class ChoosePromotionStoreTest {

    private class FakeRepo(var result: (section: EligibleSection?, page: Int?) -> EligibleOffersResult?) : PromotionRepository {
        var calls = 0
        override suspend fun searchCustomerVouchers(keyword: String?, serviceCode: String?, tab: String?, page: Int?, size: Int?): SearchCustomerVouchersResult? = null
        override suspend fun getCustomerVoucherDetail(voucherId: String, service: String?): VoucherDetail? = null
        override suspend fun createRedemptionSession(request: CreateRedemptionRequest): CreateRedemptionResult? = null
        override suspend fun validateStackableDiscounts(request: ValidateDiscountsRequest): ValidateDiscountsResult? = null
        override suspend fun findEligibleCampaigns(request: FindEligibleCampaignsRequest): EligibleOffersResult? {
            calls++
            return result(request.section, request.myPage)
        }
    }

    private class Ctx : PromotionRequestContextProvider {
        override fun getOrderId(): String = "o-1"
        override fun getOrderValue(): String = "100000"
    }

    @BeforeTest
    fun setUp() = PromotionContainer.initialize(PromotionSDKConfig(baseUrl = "https://a.example.com", requestContextProvider = Ctx()))

    @AfterTest
    fun tearDown() = PromotionContainer.clear()

    private fun offer(id: String, usable: Boolean = true) = EligibleOffer(id = id, campaignName = "C-$id", usable = usable)
    private fun store(repo: FakeRepo) =
        ChoosePromotionStore(FindEligibleCampaignsUseCase(repo), CoroutineScope(UnconfinedTestDispatcher()))

    @Test
    fun loadInitial_populatesBothGroups() = runTest {
        val repo = FakeRepo { _, _ ->
            EligibleOffersResult(
                myOffers = listOf(offer("m1")),
                otherOffers = listOf(offer("o1"), offer("o2")),
                activeTab = "all",
                myIsLastPage = false,
                otherIsLastPage = true,
            )
        }
        val s = store(repo)
        s.dispatch(ChoosePromotionIntent.LoadInitial)

        val st = s.state.value
        assertEquals(listOf("m1"), st.myOffers.map { it.source.id })
        assertEquals(listOf("o1", "o2"), st.otherOffers.map { it.source.id })
        assertEquals("all", st.selectedTabCode)
        assertTrue(st.hasLoadedInitial)
    }

    @Test
    fun preload_usesGivenData_withoutApiCall() = runTest {
        val repo = FakeRepo { _, _ -> EligibleOffersResult() }
        val s = store(repo)
        s.dispatch(ChoosePromotionIntent.Preload(
            myOffers = listOf(offer("m1")),
            otherOffers = listOf(offer("o1")),
            myIsLastPage = true,
            otherIsLastPage = true,
        ))
        assertEquals(0, repo.calls)   // có data preload → KHÔNG gọi API
        assertEquals(listOf("m1"), s.state.value.myOffers.map { it.source.id })
    }

    @Test
    fun loadMoreMy_appends_onlyMyGroup() = runTest {
        val repo = FakeRepo { section, page ->
            when {
                section == null -> EligibleOffersResult(myOffers = listOf(offer("m1")), otherOffers = listOf(offer("o1")), myIsLastPage = false)
                section == EligibleSection.MY_OFFERS -> EligibleOffersResult(myOffers = listOf(offer("m2")), myIsLastPage = true)
                else -> EligibleOffersResult()
            }
        }
        val s = store(repo)
        s.dispatch(ChoosePromotionIntent.LoadInitial)
        s.dispatch(ChoosePromotionIntent.LoadMoreMyVouchers)

        val st = s.state.value
        assertEquals(listOf("m1", "m2"), st.myOffers.map { it.source.id })
        assertEquals(listOf("o1"), st.otherOffers.map { it.source.id })   // nhóm khác giữ nguyên
        assertTrue(st.myIsLastPage)
    }

    @Test
    fun error_setsErrorCode() = runTest {
        val repo = FakeRepo { _, _ -> throw RuntimeException("boom") }
        val s = store(repo)
        s.dispatch(ChoosePromotionIntent.LoadInitial)
        assertTrue(s.state.value.errorCode != null)

        s.dispatch(ChoosePromotionIntent.ConsumeError)
        assertEquals(null, s.state.value.errorCode)
    }
}
