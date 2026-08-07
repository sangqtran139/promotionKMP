package com.ttcn.promotionsdk.data.repository

import com.ttcn.promotionsdk.data.dto.eligible.toEligibleCampaignsRequest
import com.ttcn.promotionsdk.data.dto.eligible.toEligibleOffersResult
import com.ttcn.promotionsdk.data.dto.redemption.toCreateRedemptionResult
import com.ttcn.promotionsdk.data.dto.redemption.toRedemptionSessionRequest
import com.ttcn.promotionsdk.data.dto.stackablediscount.toStackableDiscountsRequest
import com.ttcn.promotionsdk.data.dto.stackablediscount.toValidateDiscountsResult
import com.ttcn.promotionsdk.data.dto.voucher.toSearchCustomerVouchersResult
import com.ttcn.promotionsdk.data.dto.voucher.toVoucherDetail
import com.ttcn.promotionsdk.data.remote.PromotionRemoteDataSource
import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffersResult
import com.ttcn.promotionsdk.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.domain.model.redemption.CreateRedemptionResult
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsResult
import com.ttcn.promotionsdk.domain.model.voucher.SearchCustomerVouchersResult
import com.ttcn.promotionsdk.domain.model.voucher.VoucherDetail
import com.ttcn.promotionsdk.domain.repository.PromotionRepository

internal class PromotionRepositoryImpl(
    private val remoteDataSource: PromotionRemoteDataSource,
) : PromotionRepository {

    override suspend fun searchCustomerVouchers(
        keyword: String?,
        serviceCode: String?,
        tab: String?,
        page: Int?,
        size: Int?,
    ): SearchCustomerVouchersResult? {
        return remoteDataSource.searchCustomerVouchers(
            keyword = keyword,
            serviceCode = serviceCode,
            tab = tab,
            page = page,
            size = size,
        )?.toSearchCustomerVouchersResult()
    }

    override suspend fun getCustomerVoucherDetail(
        voucherId: String,
        service: String?,
    ): VoucherDetail? {
        return remoteDataSource.getCustomerVoucherDetail(
            voucherId = voucherId,
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

    override suspend fun findEligibleCampaigns(
        request: FindEligibleCampaignsRequest,
    ): EligibleOffersResult? {
        return remoteDataSource.findEligibleCampaigns(request.toEligibleCampaignsRequest())
            ?.toEligibleOffersResult()
    }
}
