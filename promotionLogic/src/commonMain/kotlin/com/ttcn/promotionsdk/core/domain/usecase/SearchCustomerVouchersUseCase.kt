package com.ttcn.promotionsdk.core.domain.usecase

import com.ttcn.promotionsdk.core.di.internal.get
import com.ttcn.promotionsdk.core.domain.exception.NetworkException
import com.ttcn.promotionsdk.core.domain.exception.PromotionException
import com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersRequest
import com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersResult
import com.ttcn.promotionsdk.core.domain.repository.PromotionRepository
import kotlin.coroutines.cancellation.CancellationException

class SearchCustomerVouchersUseCase private constructor(
    private val repositoryProvider: () -> PromotionRepository,
) {

    internal constructor(repository: PromotionRepository) : this({ repository })

    private val repository: PromotionRepository get() = repositoryProvider()

    /**
     * UI dựng thẳng use case: `SearchCustomerVouchersUseCase()`.
     * Repository được lấy từ đồ thị đã dựng bởi `PromotionContainer.initialize(...)`.
     * Gọi trước khi init sẽ ném `IllegalStateException`.
     */
    constructor() : this({ get<PromotionRepository>() })

    /**
     * Ném [PromotionException] (lỗi nghiệp vụ/HTTP) hoặc [NetworkException] (timeout, mất mạng).
     *
     * `@Throws` là bắt buộc cho iOS: Kotlin/Native chỉ chuyển exception được khai báo thành `NSError`;
     * exception không khai báo sẽ `abort()` tiến trình thay vì báo lỗi cho Swift.
     */
    @Throws(PromotionException::class, NetworkException::class, CancellationException::class)
    suspend operator fun invoke(
        request: SearchCustomerVouchersRequest,
    ): SearchCustomerVouchersResult? = promotionCall {
        repository.searchCustomerVouchers(
            customerId = request.customerId,
            keyword = request.keyword,
            serviceCode = request.serviceCode,
            tab = request.tab,
            page = request.page,
            size = request.size,
        )
    }
}
