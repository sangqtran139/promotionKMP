package com.ttcn.promotionsdk.core.domain.usecase

import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableDiscountsRequest
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableDiscountsResponse
import com.ttcn.promotionsdk.core.domain.repository.PromotionRepository
import java.util.UUID

class ValidateStackableDiscountsUseCase(
    private val repository: PromotionRepository,
) {
    suspend operator fun invoke(
        request: StackableDiscountsRequest,
    ): StackableDiscountsResponse? {
        return repository.validateStackableDiscounts(request)
    }
}