package com.ttcn.promotionsdk.core.domain.usecase

import com.ttcn.promotionsdk.core.domain.exception.NetworkException
import com.ttcn.promotionsdk.core.domain.exception.PromotionErrorCodes
import com.ttcn.promotionsdk.core.domain.exception.PromotionException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Rào chắn cuối cùng trước khi exception vượt biên giới Kotlin → Swift.
 *
 * Mọi use case đều khai `@Throws(PromotionException, NetworkException, CancellationException)`.
 * Kotlin/Native **giết tiến trình** nếu một exception khác thoát ra — không có cách nào bắt lại từ
 * Swift. Trên Android cùng exception đó chỉ rơi vào `catch (Throwable)` của
 * [PromotionUseCases.headlessCall] và thành `PromotionResult.Failure`.
 *
 * `apiCall` của data source chỉ phủ tầng network. Hàm này phủ cả phần còn lại: mapper DTO → domain,
 * và `IllegalStateException` khi use case được dựng trước `PromotionContainer.initialize(...)`.
 *
 * Đã có một crash thật vì thiếu nó: `JsonConvertException` từ `.body()` làm app iOS `abort()`.
 */
internal suspend fun <T> promotionCall(block: suspend () -> T): T =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: PromotionException) {
        throw e
    } catch (e: NetworkException) {
        throw e
    } catch (e: Throwable) {
        throw PromotionException(PromotionErrorCodes.GENERAL, e.message, null)
    }
