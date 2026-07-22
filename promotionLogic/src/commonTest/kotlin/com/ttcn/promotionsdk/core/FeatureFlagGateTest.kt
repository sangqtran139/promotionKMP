package com.ttcn.promotionsdk.core

import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.data.remote.KtorPromotionApiService
import com.ttcn.promotionsdk.core.data.remote.PromotionHttpClient
import com.ttcn.promotionsdk.core.data.remote.PromotionRemoteDataSource
import com.ttcn.promotionsdk.core.data.repository.PromotionRepositoryImpl
import com.ttcn.promotionsdk.core.domain.exception.PromotionErrorCodes
import com.ttcn.promotionsdk.core.domain.model.PromotionResult
import com.ttcn.promotionsdk.core.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.core.domain.model.featureflag.PromotionFeatureFlag
import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersRequest
import com.ttcn.promotionsdk.core.domain.usecase.CreateRedemptionSessionUseCase
import com.ttcn.promotionsdk.core.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.core.domain.usecase.GetCustomerVoucherDetailUseCase
import com.ttcn.promotionsdk.core.domain.usecase.PromotionUseCases
import com.ttcn.promotionsdk.core.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.core.domain.usecase.ValidateStackableDiscountsUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Kill-switch của facade headless. Cờ TẮT phải chặn **trước khi** gọi mạng, và trả cùng một mã lỗi
 * trên cả hai nền tảng ([PromotionErrorCodes.FEATURE_DISABLED] = `PRM_MOB_021`).
 */
class FeatureFlagGateTest {

    private class Provider : PromotionRequestContextProvider

    /** Ghi lại số request đã bay ra, để chứng minh cờ TẮT là không chạm mạng. */
    private fun useCasesWith(
        requestCount: MutableList<Unit>,
        isFeatureEnabled: (String) -> Boolean,
    ): PromotionUseCases {
        val engine = MockEngine {
            requestCount.add(Unit)
            respond(
                """{"success":true,"data":{}}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            with(PromotionHttpClient) { configure("https://api.example.com", Provider(), isDebug = false) }
        }
        val repository = PromotionRepositoryImpl(PromotionRemoteDataSource(KtorPromotionApiService(client)))
        return PromotionUseCases(
            searchVouchersUseCase = SearchCustomerVouchersUseCase(repository),
            voucherDetailUseCase = GetCustomerVoucherDetailUseCase(repository),
            validateDiscountsUseCase = ValidateStackableDiscountsUseCase(repository),
            createRedemptionUseCase = CreateRedemptionSessionUseCase(repository),
            findEligibleCampaignsUseCase = FindEligibleCampaignsUseCase(repository),
            isFeatureEnabled = isFeatureEnabled,
        )
    }

    private val searchRequest = SearchCustomerVouchersRequest()
    private val eligibleRequest = FindEligibleCampaignsRequest(orderId = "o-1", orderValue = "1000")
    private val validateRequest =
        ValidateDiscountsRequest(orderId = "o-1", orderValue = "1000", items = emptyList())
    private val redemptionRequest =
        CreateRedemptionRequest(orderId = "o-1", orderValue = "1000", items = emptyList())

    @Test
    fun everyMethod_isGatedByItsOwnFlag_andSkipsNetwork() = runTest {
        val requests = mutableListOf<Unit>()
        // Mỗi lượt tắt đúng một cờ; các cờ còn lại bật.
        suspend fun assertBlocked(flag: String, call: suspend (PromotionUseCases) -> PromotionResult<*>) {
            val useCases = useCasesWith(requests) { it != flag }

            val result = call(useCases)

            val failure = assertIs<PromotionResult.Failure>(result, "cờ $flag TẮT phải chặn")
            assertEquals(PromotionErrorCodes.FEATURE_DISABLED, failure.errorCode)
        }

        assertBlocked(PromotionFeatureFlag.VOUCHER_LIST) { it.searchVouchers(searchRequest) }
        assertBlocked(PromotionFeatureFlag.VOUCHER_DETAIL) { it.getVoucherDetail("v-1") }
        assertBlocked(PromotionFeatureFlag.VOUCHER_SELECTION) { it.findEligible(eligibleRequest) }
        assertBlocked(PromotionFeatureFlag.VOUCHER_APPLY) { it.validateDiscounts(validateRequest) }
        assertBlocked(PromotionFeatureFlag.VOUCHER_REDEEM) { it.createRedemption(redemptionRequest) }

        assertTrue(requests.isEmpty(), "cờ TẮT nhưng vẫn gọi mạng ${requests.size} lần")
    }

    @Test
    fun flagOn_letsTheCallThrough() = runTest {
        val requests = mutableListOf<Unit>()
        val useCases = useCasesWith(requests) { true }

        useCases.searchVouchers(searchRequest)

        assertEquals(1, requests.size)
    }

    /**
     * Một cờ TẮT không được chặn nhầm phương thức khác — mỗi hàm gác đúng cờ của nó.
     */
    @Test
    fun disablingOneFlag_doesNotBlockOtherMethods() = runTest {
        val requests = mutableListOf<Unit>()
        val useCases = useCasesWith(requests) { it != PromotionFeatureFlag.VOUCHER_REDEEM }

        val redeem = useCases.createRedemption(redemptionRequest)
        val search = useCases.searchVouchers(searchRequest)

        assertIs<PromotionResult.Failure>(redeem)
        assertIs<PromotionResult.Success<*>>(search)
        assertEquals(1, requests.size)
    }
}
