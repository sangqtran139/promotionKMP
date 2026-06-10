package com.ttcn.promotionsdk.core.domain.usecase

import com.ttcn.promotionsdk.core.data.dto.voucher.CustomerVoucherDetail
import com.ttcn.promotionsdk.core.domain.repository.PromotionRepository

class GetCustomerVoucherDetailUseCase(
    private val repository: PromotionRepository,
) {
    suspend operator fun invoke(
        voucherId: String,
        customerId: String,
        service: String?,
    ): CustomerVoucherDetail? {
        return repository.getCustomerVoucherDetail(
            voucherId = voucherId,
            customerId = customerId,
            service = service,
        )
    }
}