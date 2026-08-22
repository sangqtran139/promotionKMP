package com.ttcn.promotionsdk.data.remote

import com.ttcn.promotionsdk.data.dto.eligible.EligibleCampaignsRequest
import com.ttcn.promotionsdk.data.dto.eligible.EligibleCampaignsResponse
import com.ttcn.promotionsdk.data.dto.redemption.RedemptionSessionRequest
import com.ttcn.promotionsdk.data.dto.redemption.RedemptionSessionResponse
import com.ttcn.promotionsdk.data.dto.stackablediscount.StackableDiscountsRequest
import com.ttcn.promotionsdk.data.dto.stackablediscount.StackableDiscountsResponse
import com.ttcn.promotionsdk.data.dto.voucher.CustomerVoucherDetail
import com.ttcn.promotionsdk.data.dto.voucher.SearchCustomerVouchersResponse
import com.ttcn.promotionsdk.common.ioDispatcher
import com.ttcn.promotionsdk.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.domain.exception.NetworkException
import com.ttcn.promotionsdk.domain.exception.PromotionException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.json.Json

internal class PromotionRemoteDataSource(
    private val apiService: PromotionApiService,
    /**
     * `null` = tắt cơ chế thử lại khi 401 (đường dựng tay trong test). Đồ thị DI luôn bơm cổng thật
     * — xem [NetworkModule].
     */
    private val tokenRefreshGate: TokenRefreshGate? = null,
) {
    suspend fun searchCustomerVouchers(
        keyword: String?,
        serviceCode: String?,
        tab: String?,
        page: Int?,
        size: Int?,
    ): SearchCustomerVouchersResponse? = apiCall {
        apiService.searchCustomerVouchers(
            keyword = keyword,
            serviceCode = serviceCode,
            tab = tab,
            page = page,
            size = size,
        ).requireData()
    }

    suspend fun getCustomerVoucherDetail(
        voucherId: String,
        service: String?,
    ): CustomerVoucherDetail? = apiCall {
        apiService.getCustomerVoucherDetail(
            voucherId = voucherId,
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
     *
     * `withContext(ioDispatcher)` bọc ngoài **không** thừa dù engine đã tự chạy nền: nó kéo cả phần
     * pipeline phía client — trong đó `defaultRequest { }` gọi ngược `PromotionRequestContextProvider`
     * của host — ra khỏi thread của nơi gọi. Android truyền `viewModelScope` (main thread) xuống
     * store, nên nếu không có dòng này thì lambda cấp token của host chạy trên main. Xem [ioDispatcher].
     */
    private suspend fun <T> apiCall(block: suspend () -> T): T = withContext(ioDispatcher) {
        try {
            retryingOnUnauthorized(block)
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
    }

    /**
     * Chạy [block]; ăn **401** thì xin host token mới rồi chạy lại **đúng một lần**.
     *
     * Một lần, không phải vòng lặp: refresh xong mà vẫn 401 nghĩa là token mới cũng không được chấp
     * nhận — thử tiếp chỉ đổi một lỗi hiển thị lấy vài giây chờ và một cơn bão request.
     *
     * [TokenRefreshGate.generation] phải chụp **trước** khi gửi, không phải lúc bắt lỗi: chụp lúc
     * bắt lỗi thì hai request cùng hỏng sẽ cùng thấy số mới nhất và cùng bỏ qua refresh.
     *
     * Không có cổng ([tokenRefreshGate] `null`) → chạy thẳng, hành vi y như trước.
     */
    private suspend fun <T> retryingOnUnauthorized(block: suspend () -> T): T {
        val gate = tokenRefreshGate ?: return block()
        val generation = gate.generation
        return try {
            block()
        } catch (e: Throwable) {
            if (!e.isUnauthorized() || !gate.refresh(generation)) throw e
            block()
        }
    }

    /**
     * 401 tới theo **hai** đường: `expectSuccess` của Ktor ném [ResponseException] cho 4xx, còn
     * server trả HTTP 200 kèm `status: 401` trong envelope thì [requireData] ném
     * [PromotionException]. Bỏ sót đường thứ hai là cơ chế thử lại im lặng không chạy ở đúng những
     * endpoint bọc envelope. Cùng cặp điều kiện mà `ErrorCodeExtensions` dùng để ra `TOKEN_EXPIRED`.
     */
    private fun Throwable.isUnauthorized(): Boolean = when (this) {
        is ResponseException -> response.status.value == HTTP_UNAUTHORIZED
        is PromotionException -> httpStatus == HTTP_UNAUTHORIZED
        else -> false
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
        private const val HTTP_UNAUTHORIZED = 401
        private val errorJson = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}
