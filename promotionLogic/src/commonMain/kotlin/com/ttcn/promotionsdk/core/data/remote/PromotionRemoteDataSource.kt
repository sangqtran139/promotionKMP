package com.ttcn.promotionsdk.core.data.remote

import com.ttcn.promotionsdk.core.data.dto.eligible.EligibleCampaignsRequest
import com.ttcn.promotionsdk.core.data.dto.eligible.EligibleCampaignsResponse
import com.ttcn.promotionsdk.core.data.dto.redemption.RedemptionSessionRequest
import com.ttcn.promotionsdk.core.data.dto.redemption.RedemptionSessionResponse
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableDiscountsRequest
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableDiscountsResponse
import com.ttcn.promotionsdk.core.data.dto.voucher.CustomerVoucherDetail
import com.ttcn.promotionsdk.core.data.dto.voucher.SearchCustomerVouchersResponse
import com.ttcn.promotionsdk.core.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.core.domain.exception.NetworkException
import com.ttcn.promotionsdk.core.domain.exception.PromotionException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.json.Json

internal class PromotionRemoteDataSource(
    private val apiService: PromotionApiService,
) {
    suspend fun searchCustomerVouchers(
        customerId: String,
        keyword: String?,
        serviceCode: String?,
        tab: String?,
        page: Int?,
        size: Int?,
    ): SearchCustomerVouchersResponse? = apiCall {
        apiService.searchCustomerVouchers(
            customerId = customerId,
            keyword = keyword,
            serviceCode = serviceCode,
            tab = tab,
            page = page,
            size = size,
        ).requireData()
    }

    suspend fun getCustomerVoucherDetail(
        voucherId: String,
        customerId: String,
        service: String?,
    ): CustomerVoucherDetail? = apiCall {
        apiService.getCustomerVoucherDetail(
            voucherId = voucherId,
            customerId = customerId,
            service = service,
        ).requireData()
    }

    suspend fun createRedemptionSession(
        request: RedemptionSessionRequest,
    ): RedemptionSessionResponse? = apiCall {
        apiService.createRedemptionSession(request).requireData()
    }

    suspend fun validateStackableDiscounts(
        request: StackableDiscountsRequest,
    ): StackableDiscountsResponse? = apiCall {
        apiService.validateStackableDiscounts(request).requireData()
    }

    suspend fun findEligibleCampaigns(
        request: EligibleCampaignsRequest,
    ): EligibleCampaignsResponse? = apiCall {
        apiService.findEligibleCampaigns(request).requireData()
    }

    /**
     * Bọc lệnh gọi API, chuẩn hoá lỗi transport sang exception domain:
     * - [PromotionException] (lỗi nghiệp vụ từ [requireData]) → giữ nguyên.
     * - [ResponseException] (4xx/5xx, thay cho `HttpException` của Retrofit) → parse error body,
     *   lấy lại `code`/`message` của server.
     * - timeout → [NetworkException] ([ErrorCodes.TIMEOUT]); [IOException] khác (mất mạng, không
     *   phân giải host…) → [NetworkException] ([ErrorCodes.NETWORK_ERROR]).
     *
     * Ba loại timeout của Ktor đều là con của [IOException] nên phải bắt trước.
     *
     * Nhánh `Throwable` cuối **bắt buộc**, không phải phòng xa. `.body()` còn ném
     * `SerializationException` (server trả JSON lệch schema) và `NoTransformationFoundException`
     * (Content-Type không phải JSON). Chúng không nằm trong `@Throws` của use case, nên trên
     * Kotlin/Native sẽ `abort()` tiến trình thay vì trả `NSError` cho Swift — app iOS chết ngay.
     * Trên Android chúng chỉ rơi vào `catch (Throwable)` của `PromotionUseCases.headlessCall`.
     */
    private suspend fun <T> apiCall(block: suspend () -> T): T =
        try {
            block()
        } catch (e: PromotionException) {
            throw e
        } catch (e: ResponseException) {
            throw e.toPromotionException()
        } catch (e: HttpRequestTimeoutException) {
            throw NetworkException(ErrorCodes.TIMEOUT, e.message, e)
        } catch (e: ConnectTimeoutException) {
            throw NetworkException(ErrorCodes.TIMEOUT, e.message, e)
        } catch (e: SocketTimeoutException) {
            throw NetworkException(ErrorCodes.TIMEOUT, e.message, e)
        } catch (e: IOException) {
            throw NetworkException(ErrorCodes.NETWORK_ERROR, e.message, e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw PromotionException(ErrorCodes.GENERAL, e.message, null)
        }

    private suspend fun ResponseException.toPromotionException(): PromotionException {
        val parsed = runCatching { response.bodyAsText() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { errorJson.decodeFromString<ApiErrorBody>(it) }.getOrNull() }
        return PromotionException(
            errorCode = parsed?.code ?: ErrorCodes.GENERAL,
            message = parsed?.message ?: message,
            httpStatus = response.status.value,
        )
    }

    private fun <T> ApiResponseTemplate<T>.requireData(): T? {
        val isHttpSuccess = status == null || status in 200..299
        if (success == false || !isHttpSuccess) {
            throw PromotionException(
                errorCode = code,
                message = message,
                httpStatus = status,
            )
        }

        if (data == null && success != true) {
            throw PromotionException(
                errorCode = code ?: ERROR_EMPTY_DATA,
                message = message ?: "Empty response data",
                httpStatus = status,
            )
        }
        return data
    }

    private companion object {
        private const val ERROR_EMPTY_DATA = "EMPTY_DATA"
        private val errorJson = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}
