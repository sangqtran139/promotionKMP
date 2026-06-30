package com.ttcn.promotionsdk.core.data.repository

import com.ttcn.promotionsdk.core.data.dto.redemption.toRedemptionSessionRequest
import com.ttcn.promotionsdk.core.data.dto.redemption.toCreateRedemptionResult
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.toValidateDiscountsResult
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.toStackableDiscountsRequest
import com.ttcn.promotionsdk.core.data.dto.voucher.toVoucherDetail
import com.ttcn.promotionsdk.core.data.dto.voucher.toSearchCustomerVouchersResult
import com.ttcn.promotionsdk.core.data.remote.PromotionRemoteDataSource
import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsResult
import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionResult
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherDetail
import com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersResult
import com.ttcn.promotionsdk.core.domain.repository.PromotionRepository

internal class PromotionRepositoryImpl(
    private val remoteDataSource: PromotionRemoteDataSource,
) : PromotionRepository {

    override suspend fun searchCustomerVouchers(
        customerId: String,
        keyword: String?,
        serviceCode: String?,
        tab: String?,
        page: Int?,
        size: Int?,
    ): SearchCustomerVouchersResult? {
        return remoteDataSource.searchCustomerVouchers(
            customerId = customerId,
            keyword = keyword,
            serviceCode = serviceCode,
            tab = tab,
            page = page,
            size = size,
        )?.toSearchCustomerVouchersResult()
    }

    override suspend fun getCustomerVoucherDetail(
        voucherId: String,
        customerId: String,
        service: String?,
    ): VoucherDetail? {
        return remoteDataSource.getCustomerVoucherDetail(
            voucherId = voucherId,
            customerId = customerId,
            service = service,
        )?.toVoucherDetail()
    }

    override suspend fun createRedemptionSession(
        request: CreateRedemptionRequest,
    ): CreateRedemptionResult? {
        return remoteDataSource.createRedemptionSession(request.toRedemptionSessionRequest())
            ?.toCreateRedemptionResult()
    }

    override suspend fun validateStackableDiscounts(
        request: ValidateDiscountsRequest,
    ): ValidateDiscountsResult? {
        return remoteDataSource.validateStackableDiscounts(request.toStackableDiscountsRequest())
            ?.toValidateDiscountsResult()
    }
}
