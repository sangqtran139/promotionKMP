package com.ttcn.promotionsdk.core.data.remote

import com.ttcn.promotionsdk.core.data.dto.voucher.ApiResponseTemplate
import com.ttcn.promotionsdk.core.data.dto.voucher.CustomerVoucherDetail
import com.ttcn.promotionsdk.core.data.dto.voucher.SearchCustomerVouchersData

class PromotionRemoteDataSource(
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
    ): SearchCustomerVouchersData? {
        return apiService.searchCustomerVouchers(
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
    ): CustomerVoucherDetail? {
        return apiService.getCustomerVoucherDetail(
            voucherId = voucherId,
            customerId = customerId,
            service = service,
        ).requireData()
    }

    private fun <T> ApiResponseTemplate<T>.requireData(): T? {
        val isHttpSuccess = status == null || status in 200..299
        if (success == false || !isHttpSuccess) {
            throw PromotionApiException(
                errorCode = code,
                message = message,
                status = status,
            )
        }

        if (data == null && success != true) {
            throw PromotionApiException(
                errorCode = code ?: ERROR_EMPTY_DATA,
                message = message ?: "Empty response data",
                status = status,
            )
        }
        return data
    }

    private companion object {
        private const val ERROR_EMPTY_DATA = "EMPTY_DATA"
    }
}
