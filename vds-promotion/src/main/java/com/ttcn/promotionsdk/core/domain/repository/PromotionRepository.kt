package com.ttcn.promotionsdk.core.domain.repository

import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsResult
import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionResult
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherDetail
import com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersResult

internal interface PromotionRepository {
    suspend fun searchCustomerVouchers(
        customerId: String,
        keyword: String?,
        serviceCode: String?,
        tab: String?,
        page: Int?,
        size: Int?,
    ): SearchCustomerVouchersResult?

    suspend fun getCustomerVoucherDetail(
        voucherId: String,
        customerId: String,
        service: String?,
    ): VoucherDetail?

    suspend fun createRedemptionSession(request: CreateRedemptionRequest): CreateRedemptionResult?

    suspend fun validateStackableDiscounts(request: ValidateDiscountsRequest): ValidateDiscountsResult?
}
