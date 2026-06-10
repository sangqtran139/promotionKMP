package com.ttcn.promotionsdk.core.domain.usecase

import com.ttcn.promotionsdk.core.data.dto.redemption.RedemptionSessionRequest
import com.ttcn.promotionsdk.core.data.dto.redemption.RedemptionSessionResponse
import com.ttcn.promotionsdk.core.domain.repository.PromotionRepository

class CreateRedemptionSessionUseCase(
    private val repository: PromotionRepository,
) {
    suspend operator fun invoke(
        request: RedemptionSessionRequest,
    ): RedemptionSessionResponse? {
        return repository.createRedemptionSession(request)
    }
}