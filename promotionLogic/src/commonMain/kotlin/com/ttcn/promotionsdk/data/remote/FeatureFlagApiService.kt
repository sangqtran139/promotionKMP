package com.ttcn.promotionsdk.data.remote

import com.ttcn.promotionsdk.data.dto.featureflag.FeatureFlagItemResponse
import com.ttcn.promotionsdk.data.dto.featureflag.FeatureFlagRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

internal interface FeatureFlagApiService {
    suspend fun getFeatureFlags(
        body: FeatureFlagRequest,
    ): ApiResponseTemplate<List<FeatureFlagItemResponse>>
}

internal class KtorFeatureFlagApiService(
    private val client: HttpClient,
) : FeatureFlagApiService {

    override suspend fun getFeatureFlags(
        body: FeatureFlagRequest,
    ): ApiResponseTemplate<List<FeatureFlagItemResponse>> =
        client.post("$BASE_PATH/feature-flag/list") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    private companion object {
        const val BASE_PATH = "promotion/promotion-vtm-bff/api/v1/vtm"
    }
}
