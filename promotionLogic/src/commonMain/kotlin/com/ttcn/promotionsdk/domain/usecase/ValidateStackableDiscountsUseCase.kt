package com.ttcn.promotionsdk.domain.usecase

import com.ttcn.promotionsdk.di.internal.get
import com.ttcn.promotionsdk.domain.exception.NetworkException
import com.ttcn.promotionsdk.domain.exception.PromotionException
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.domain.model.stackablediscount.ValidateDiscountsResult
import com.ttcn.promotionsdk.domain.repository.PromotionRepository
import kotlin.coroutines.cancellation.CancellationException

public class ValidateStackableDiscountsUseCase private constructor(
    private val repositoryProvider: () -> PromotionRepository,
) {

    internal constructor(repository: PromotionRepository) : this({ repository })

    private val repository: PromotionRepository get() = repositoryProvider()

    public constructor() : this({ get<PromotionRepository>() })

    /**
     * Ném [PromotionException] (lỗi nghiệp vụ/HTTP) hoặc [NetworkException] (timeout, mất mạng).
     *
     * `@Throws` là bắt buộc cho iOS: Kotlin/Native chỉ chuyển exception được khai báo thành `NSError`;
     * exception không khai báo sẽ `abort()` tiến trình thay vì báo lỗi cho Swift.
     */
    @Throws(PromotionException::class, NetworkException::class, CancellationException::class)
    public suspend operator fun invoke(
        request: ValidateDiscountsRequest,
    ): ValidateDiscountsResult? = promotionCall {
        repository.validateStackableDiscounts(request)
    }
}
