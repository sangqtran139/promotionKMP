package com.ttcn.promotionsdk.core.domain.repository

import com.ttcn.promotionsdk.core.data.dto.redemption.RedemptionSessionRequest
import com.ttcn.promotionsdk.core.data.dto.redemption.RedemptionSessionResponse
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableDiscountsRequest
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableDiscountsResponse
import com.ttcn.promotionsdk.core.domain.model.VoucherDetail
import com.ttcn.promotionsdk.core.domain.model.VoucherSearchResult

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
    ): VoucherSearchResult?

    suspend fun getCustomerVoucherDetail(
        voucherId: String,
        customerId: String,
        service: String?,
    ): VoucherDetail?

    suspend fun createRedemptionSession(request: RedemptionSessionRequest): RedemptionSessionResponse?

    suspend fun validateStackableDiscounts(request: StackableDiscountsRequest): StackableDiscountsResponse?
}
