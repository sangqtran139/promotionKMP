package com.ttcn.promotionsdk.core.data.remote

import com.ttcn.promotionsdk.core.data.dto.featureflag.FeatureFlagItemResponse
import com.ttcn.promotionsdk.core.data.dto.featureflag.FeatureFlagRequest
import com.ttcn.promotionsdk.core.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.core.domain.exception.FeatureFlagException
import com.ttcn.promotionsdk.core.domain.exception.NetworkException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

internal class FeatureFlagRemoteDataSource(
    private val apiService: FeatureFlagApiService,
) {
    suspend fun getFeatureFlags(sessionId: String, userId: String): List<FeatureFlagItemResponse>? = apiCall {
        apiService.getFeatureFlags(FeatureFlagRequest(sessionId = sessionId, userId = userId)).requireData()
    }

    private suspend fun <T> apiCall(block: suspend () -> T): T =
        try {
            block()
        } catch (e: FeatureFlagException) {
            throw e
        } catch (e: HttpException) {
            throw FeatureFlagException()
        } catch (e: SocketTimeoutException) {
            throw NetworkException(ErrorCodes.TIMEOUT, e.message, e)
        } catch (e: IOException) {
            throw NetworkException(ErrorCodes.NETWORK_ERROR, e.message, e)
        }

    private fun <T> ApiResponseTemplate<T>.requireData(): T? {
        val isHttpSuccess = status == null || status in 200..299
        if (success == false || !isHttpSuccess) {
            throw FeatureFlagException()
        }
        return data
    }
}
