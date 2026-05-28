// vds-promotion/src/main/java/com/ttcn/promotionsdk/core/data/repository/PromotionRepositoryImpl.kt
package com.ttcn.promotionsdk.core.data.repository

import com.ttcn.promotionsdk.core.data.dto.voucher.CustomerVoucherDetail
import com.ttcn.promotionsdk.core.data.dto.voucher.SearchCustomerVouchersData
import com.ttcn.promotionsdk.core.data.remote.PromotionRemoteDataSource
import com.ttcn.promotionsdk.core.domain.repository.PromotionRepository

class PromotionRepositoryImpl(
    private val remoteDataSource: PromotionRemoteDataSource,
) : PromotionRepository {

    override suspend fun searchCustomerVouchers(
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
        return remoteDataSource.searchCustomerVouchers(
            customerId = customerId,
            keyword = keyword,
            serviceCode = serviceCode,
            tab = tab,
            sectionCode = sectionCode,
            myVouchersPage = myVouchersPage,
            myVouchersSize = myVouchersSize,
            otherVouchersPage = otherVouchersPage,
            otherVouchersSize = otherVouchersSize,
        )
    }

    override suspend fun getCustomerVoucherDetail(
        voucherId: String,
        customerId: String,
        service: String?,
    ): CustomerVoucherDetail? {
        return remoteDataSource.getCustomerVoucherDetail(
            voucherId = voucherId,
            customerId = customerId,
            service = service,
        )
    }
}
