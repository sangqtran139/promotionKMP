package com.ttcn.promotionsdk.core.domain.model

/**
 * Kết quả của headless API ([com.ttcn.promotionsdk.core.domain.usecase.PromotionUseCases]).
 *
 * Thay cho kiểu cũ `T?` + ném exception: host xử lý lỗi **tường minh, type-safe** —
 * không cần `try/catch` và không phải đoán tập lỗi.
 *
 * ```kotlin
 * when (val r = PromotionSDK.useCases.searchVouchers(request)) {
 *     is PromotionResult.Success -> render(r.data)
 *     is PromotionResult.Failure -> showError(r.errorCode)
 * }
 * ```
 */
sealed interface PromotionResult<out T> {

    data class Success<out T>(val data: T) : PromotionResult<T>

    data class Failure(
        val errorCode: String,
        val message: String? = null,
        val httpStatus: Int? = null,
    ) : PromotionResult<Nothing>
}
