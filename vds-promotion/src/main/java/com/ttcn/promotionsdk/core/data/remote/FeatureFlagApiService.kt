package com.ttcn.promotionsdk.core.data.remote

import com.ttcn.promotionsdk.core.data.dto.featureflag.FeatureFlagItemResponse
import com.ttcn.promotionsdk.core.data.dto.featureflag.FeatureFlagRequest
import retrofit2.http.Body
import retrofit2.http.POST

internal interface FeatureFlagApiService {

    @POST("promotion/promotion-vtm-bff/api/v1/vtm/feature-flag/list")
    suspend fun getFeatureFlags(
        @Body body: FeatureFlagRequest,
    ): ApiResponseTemplate<List<FeatureFlagItemResponse>>
}
