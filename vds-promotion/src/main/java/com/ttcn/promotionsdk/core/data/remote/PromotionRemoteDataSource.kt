package com.ttcn.promotionsdk.core.data.remote

import com.google.gson.Gson
import com.ttcn.promotionsdk.core.data.dto.redemption.RedemptionSessionRequest
import com.ttcn.promotionsdk.core.data.dto.redemption.RedemptionSessionResponse
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableDiscountsRequest
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableDiscountsResponse
import com.ttcn.promotionsdk.core.data.dto.voucher.CustomerVoucherDetail
import com.ttcn.promotionsdk.core.data.dto.voucher.SearchCustomerVouchersResponse
import com.ttcn.promotionsdk.core.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.core.domain.exception.NetworkException
import com.ttcn.promotionsdk.core.domain.exception.PromotionException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

internal class PromotionRemoteDataSource(
    private val apiService: PromotionApiService,
) {
    suspend fun searchCustomerVouchers(
        customerId: String,
        keyword: String?,
        serviceCode: String?,
        tab: String?,
        sectionCode: String?,
        myVouchersPage: Int?,
        myVouchersSize: Int?,
        otherVouchersPage: Int?,
        otherVouchersSize: Int?,
    ): SearchCustomerVouchersResponse? = apiCall {
        apiService.searchCustomerVouchers(
            customerId = customerId,
            keyword = keyword,
            serviceCode = serviceCode,
            tab = tab,
            sectionCode = sectionCode,
            myVouchersPage = myVouchersPage,
            myVouchersSize = myVouchersSize,
            otherVouchersPage = otherVouchersPage,
            otherVouchersSize = otherVouchersSize,
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

    /**
     * Bọc lệnh gọi API, chuẩn hoá lỗi transport sang exception domain:
     * - [PromotionException] (lỗi nghiệp vụ từ [requireData]) → giữ nguyên.
     * - [HttpException] (4xx/5xx) → parse error body, lấy lại `code`/`message` của server.
     * - timeout → [NetworkException] ([ErrorCodes.TIMEOUT]); IOException khác (mất mạng, không phân
     *   giải host…) → [NetworkException] ([ErrorCodes.NETWORK_ERROR]).
     */
    private suspend fun <T> apiCall(block: suspend () -> T): T =
        try {
            block()
        } catch (e: PromotionException) {
            throw e
        } catch (e: HttpException) {
            throw e.toPromotionException()
        } catch (e: SocketTimeoutException) {
            throw NetworkException(ErrorCodes.TIMEOUT, e.message, e)
        } catch (e: IOException) {
            throw NetworkException(ErrorCodes.NETWORK_ERROR, e.message, e)
        }

    private fun HttpException.toPromotionException(): PromotionException {
        val parsed = runCatching { response()?.errorBody()?.string() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { errorGson.fromJson(it, ApiErrorBody::class.java) }.getOrNull() }
        return PromotionException(
            errorCode = parsed?.code ?: ErrorCodes.GENERAL,
            message = parsed?.message ?: message(),
            httpStatus = code(),
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
        private val errorGson = Gson()
    }
}
