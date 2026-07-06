package com.ttcn.promotionsdk.core.data.dto.featureflag

import com.google.gson.annotations.SerializedName

internal data class FeatureFlagItemResponse(
    @SerializedName("flagName") val flagName: String,
    @SerializedName("enabled") val enabled: Boolean,
)
