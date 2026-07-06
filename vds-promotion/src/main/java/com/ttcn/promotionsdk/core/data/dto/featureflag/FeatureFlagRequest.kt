package com.ttcn.promotionsdk.core.data.dto.featureflag

import com.google.gson.annotations.SerializedName

internal data class FeatureFlagRequest(
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("properties") val properties: Map<String, String> = emptyMap(),
)
