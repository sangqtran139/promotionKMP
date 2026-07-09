package com.ttcn.promotionsdk.core.domain.usecase

import com.ttcn.promotionsdk.core.di.internal.get
import com.ttcn.promotionsdk.core.domain.exception.NetworkException
import com.ttcn.promotionsdk.core.domain.exception.PromotionException
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffersResult
import com.ttcn.promotionsdk.core.domain.model.eligible.FindEligibleCampaignsRequest
import com.ttcn.promotionsdk.core.domain.repository.PromotionRepository
import kotlin.coroutines.cancellation.CancellationException

class FindEligibleCampaignsUseCase private constructor(
    private val repositoryProvider: () -> PromotionRepository,
) {

    internal constructor(repository: PromotionRepository) : this({ repository })

    private val repository: PromotionRepository get() = repositoryProvider()

    constructor() : this({ get<PromotionRepository>() })

    /**
     * Ném [PromotionException] (lỗi nghiệp vụ/HTTP) hoặc [NetworkException] (timeout, mất mạng).
     *
     * `@Throws` là bắt buộc cho iOS: Kotlin/Native chỉ chuyển exception được khai báo thành `NSError`;
     * exception không khai báo sẽ `abort()` tiến trình thay vì báo lỗi cho Swift.
     */
    @Throws(PromotionException::class, NetworkException::class, CancellationException::class)
    suspend operator fun invoke(
        request: FindEligibleCampaignsRequest,
    ): EligibleOffersResult? = promotionCall {
        repository.findEligibleCampaigns(request)
    }
}
