package com.ttcn.promotionsdk.core.data.repository

import com.ttcn.promotionsdk.core.data.dto.redemption.RedemptionSessionRequest
import com.ttcn.promotionsdk.core.data.dto.redemption.RedemptionSessionResponse
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableDiscountsRequest
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableDiscountsResponse
import com.ttcn.promotionsdk.core.data.dto.toVoucherDetail
import com.ttcn.promotionsdk.core.data.dto.toVoucherSearchResult
import com.ttcn.promotionsdk.core.data.remote.PromotionRemoteDataSource
import com.ttcn.promotionsdk.core.domain.model.VoucherDetail
import com.ttcn.promotionsdk.core.domain.model.VoucherSearchResult
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
    ): VoucherSearchResult? {
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
        )?.toVoucherSearchResult()
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
        request: RedemptionSessionRequest,
    ): RedemptionSessionResponse? {
        return remoteDataSource.createRedemptionSession(request)
    }

    override suspend fun validateStackableDiscounts(
        request: StackableDiscountsRequest,
    ): StackableDiscountsResponse? {
        return remoteDataSource.validateStackableDiscounts(request)
    }
}
