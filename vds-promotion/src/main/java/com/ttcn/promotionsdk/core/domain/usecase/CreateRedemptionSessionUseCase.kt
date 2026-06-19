package com.ttcn.promotionsdk.core.domain.usecase

import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionResult
import com.ttcn.promotionsdk.core.domain.repository.PromotionRepository

internal class CreateRedemptionSessionUseCase(
    private val repository: PromotionRepository,
) {
    suspend operator fun invoke(
        request: CreateRedemptionRequest,
    ): CreateRedemptionResult? {
        return repository.createRedemptionSession(request)
    }
}