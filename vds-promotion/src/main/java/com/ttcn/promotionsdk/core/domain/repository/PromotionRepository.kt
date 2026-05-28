// vds-promotion/src/main/java/com/ttcn/promotionsdk/core/domain/repository/PromotionRepository.kt
package com.ttcn.promotionsdk.core.domain.repository

import com.ttcn.promotionsdk.core.data.dto.voucher.CustomerVoucherDetail
import com.ttcn.promotionsdk.core.data.dto.voucher.SearchCustomerVouchersData

interface PromotionRepository {
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
    ): SearchCustomerVouchersData?

    suspend fun getCustomerVoucherDetail(
        voucherId: String,
        customerId: String,
        service: String?,
    ): CustomerVoucherDetail?
}
