package com.ttcn.promotionsdk.core.data.remote

import com.ttcn.promotionsdk.core.data.dto.featureflag.FeatureFlagItemResponse
import com.ttcn.promotionsdk.core.data.dto.featureflag.FeatureFlagRequest
import com.ttcn.promotionsdk.core.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.core.domain.exception.FeatureFlagException
import com.ttcn.promotionsdk.core.domain.exception.NetworkException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.io.IOException

internal class FeatureFlagRemoteDataSource(
    private val apiService: FeatureFlagApiService,
) {
    suspend fun getFeatureFlags(sessionId: String, userId: String): List<FeatureFlagItemResponse>? =
        apiCall {
            apiService.getFeatureFlags(
                FeatureFlagRequest(sessionId = sessionId, userId = userId),
            ).requireData()
        }

    /**
     * Khác [PromotionRemoteDataSource]: mọi lỗi HTTP đều gộp về [FeatureFlagException] không mang
     * error code — cờ tính năng không hiển thị lỗi cho người dùng, chỉ rơi về cache/mặc định.
     * Lỗi transport vẫn tách timeout / mất mạng như bản Android.
     */
    private suspend fun <T> apiCall(block: suspend () -> T): T =
        try {
            block()
        } catch (e: FeatureFlagException) {
            throw e
        } catch (e: ResponseException) {
            throw FeatureFlagException()
        } catch (e: HttpRequestTimeoutException) {
            throw NetworkException(ErrorCodes.TIMEOUT, e.message, e)
        } catch (e: ConnectTimeoutException) {
            throw NetworkException(ErrorCodes.TIMEOUT, e.message, e)
        } catch (e: SocketTimeoutException) {
            throw NetworkException(ErrorCodes.TIMEOUT, e.message, e)
        } catch (e: IOException) {
            throw NetworkException(ErrorCodes.NETWORK_ERROR, e.message, e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // `SerializationException` / `NoTransformationFoundException` không nằm trong `@Throws`
            // của use case → Kotlin/Native sẽ abort() thay vì báo lỗi cho Swift. Xem ghi chú ở
            // `PromotionRemoteDataSource.apiCall`.
            throw FeatureFlagException()
        }

    private fun <T> ApiResponseTemplate<T>.requireData(): T? {
        val isHttpSuccess = status == null || status in 200..299
        if (success == false || !isHttpSuccess) {
            throw FeatureFlagException()
        }
        return data
    }
}
