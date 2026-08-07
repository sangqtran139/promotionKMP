package com.ttcn.promotionsdk

import com.ttcn.promotionsdk.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.data.remote.KtorPromotionApiService
import com.ttcn.promotionsdk.data.remote.PromotionHttpClient
import com.ttcn.promotionsdk.data.remote.PromotionRemoteDataSource
import com.ttcn.promotionsdk.data.repository.PromotionRepositoryImpl
import com.ttcn.promotionsdk.domain.exception.PromotionErrorCodes
import com.ttcn.promotionsdk.domain.exception.PromotionException
import com.ttcn.promotionsdk.domain.model.PromotionResult
import com.ttcn.promotionsdk.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.domain.model.redemption.RedemptionItemRequest
import com.ttcn.promotionsdk.domain.model.voucher.SearchCustomerVouchersRequest
import com.ttcn.promotionsdk.domain.usecase.CreateRedemptionSessionUseCase
import com.ttcn.promotionsdk.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.domain.usecase.GetCustomerVoucherDetailUseCase
import com.ttcn.promotionsdk.domain.usecase.PromotionUseCases
import com.ttcn.promotionsdk.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.domain.usecase.ValidateStackableDiscountsUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * Response hỏng **không được** để exception lạ thoát ra khỏi data source.
 *
 * Lý do sống còn: `SerializationException` và `NoTransformationFoundException` không nằm trong
 * `@Throws` của use case. Trên Kotlin/Native, exception không khai báo làm `abort()` tiến trình —
 * app iOS chết hẳn thay vì hiện thông báo lỗi. Trên Android chúng chỉ thành `PromotionResult.Failure`.
 *
 * Đây là nguyên nhân thật của crash khi demo iOS gọi `createRedemption` với server staging.
 */
private class NoTokenProvider : PromotionRequestContextProvider

private fun useCases(
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
): PromotionUseCases {
    val client = HttpClient(MockEngine(handler)) {
        with(PromotionHttpClient) { configure("https://api.example.com", NoTokenProvider(), false) }
    }
    val repo = PromotionRepositoryImpl(PromotionRemoteDataSource(KtorPromotionApiService(client)))
    return PromotionUseCases(
        SearchCustomerVouchersUseCase(repo),
        GetCustomerVoucherDetailUseCase(repo),
        ValidateStackableDiscountsUseCase(repo),
        CreateRedemptionSessionUseCase(repo),
        FindEligibleCampaignsUseCase(repo),
    )
}

private fun rawUseCase(
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
): CreateRedemptionSessionUseCase {
    val client = HttpClient(MockEngine(handler)) {
        with(PromotionHttpClient) { configure("https://api.example.com", NoTokenProvider(), false) }
    }
    return CreateRedemptionSessionUseCase(
        PromotionRepositoryImpl(PromotionRemoteDataSource(KtorPromotionApiService(client)))
    )
}

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

private val redemptionRequest = CreateRedemptionRequest(
    orderId = "o-1",
    orderValue = "500000",
    items = listOf(RedemptionItemRequest(objectId = "v-1", objectType = "CAMPAIGN")),
)

class MalformedResponseTest {

    @Test
    fun missingRequiredField_becomesFailure_notCrash() = runTest {
        // `sessionId` là field bắt buộc của RedemptionSessionResponse → SerializationException.
        val useCases = useCases {
            respond("""{"success":true,"data":{"createdAt":"now"}}""", HttpStatusCode.OK, jsonHeaders)
        }

        val result = useCases.createRedemption(redemptionRequest)

        assertIs<PromotionResult.Failure>(result)
        assertEquals(PromotionErrorCodes.GENERAL, result.errorCode)
    }

    @Test
    fun nonJsonContentType_becomesFailure_notCrash() = runTest {
        // Gateway trả HTML → NoTransformationFoundException.
        val useCases = useCases {
            respond("<html>502 Bad Gateway</html>", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html"))
        }

        val result = useCases.createRedemption(redemptionRequest)

        assertIs<PromotionResult.Failure>(result)
        assertEquals(PromotionErrorCodes.GENERAL, result.errorCode)
    }

    @Test
    fun garbageBody_becomesFailure_notCrash() = runTest {
        val useCases = useCases {
            respond("not json at all", HttpStatusCode.OK, jsonHeaders)
        }

        val result = useCases.createRedemption(redemptionRequest)

        assertIs<PromotionResult.Failure>(result)
    }

    @Test
    fun searchVouchers_withMalformedItem_becomesFailure_notCrash() = runTest {
        // `voucher` (object) bắt buộc trong VoucherListItem — item phẳng thiếu nó → SerializationException.
        val useCases = useCases {
            respond("""{"success":true,"data":{"content":[{"title":"x"}]}}""", HttpStatusCode.OK, jsonHeaders)
        }

        val result = useCases.searchVouchers(SearchCustomerVouchersRequest())

        assertIs<PromotionResult.Failure>(result)
    }

    /**
     * Use case đơn lẻ (đường mà UI native dùng) chỉ được ném đúng các loại đã khai báo `@Throws`.
     * Nếu test này thấy một exception khác, iOS sẽ abort() lúc runtime.
     */
    @Test
    fun rawUseCase_onMalformedBody_throwsOnlyDeclaredException() = runTest {
        val useCase = rawUseCase {
            respond("""{"success":true,"data":{"createdAt":"now"}}""", HttpStatusCode.OK, jsonHeaders)
        }

        val error = assertFailsWith<PromotionException> { useCase(redemptionRequest) }

        assertEquals(PromotionErrorCodes.GENERAL, error.errorCode)
    }
}
