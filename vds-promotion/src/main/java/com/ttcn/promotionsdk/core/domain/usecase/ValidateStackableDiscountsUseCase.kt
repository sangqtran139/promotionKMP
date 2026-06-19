package com.ttcn.promotionsdk.core.domain.usecase

import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsResult
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.core.domain.repository.PromotionRepository

internal class ValidateStackableDiscountsUseCase(
    private val repository: PromotionRepository,
) {
    suspend operator fun invoke(
        request: ValidateDiscountsRequest,
    ): ValidateDiscountsResult? {
        return repository.validateStackableDiscounts(request)
    }
}