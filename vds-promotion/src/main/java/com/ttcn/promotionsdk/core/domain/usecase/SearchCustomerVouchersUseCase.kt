package com.ttcn.promotionsdk.core.domain.usecase

import com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersRequest
import com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersResult
import com.ttcn.promotionsdk.core.domain.repository.PromotionRepository

internal class SearchCustomerVouchersUseCase(
    private val repository: PromotionRepository,
) {
    suspend operator fun invoke(
        request: SearchCustomerVouchersRequest,
    ): SearchCustomerVouchersResult? {
        return repository.searchCustomerVouchers(
            customerId = request.customerId,
            keyword = request.keyword,
            serviceCode = request.serviceCode,
            sectionCode = request.sectionCode,
            tab = request.tab,
            myVouchersPage = request.myVouchersPage,
            myVouchersSize = request.myVouchersSize,
            otherVouchersPage = request.otherVouchersPage,
            otherVouchersSize = request.otherVouchersSize,
        )
    }
}
