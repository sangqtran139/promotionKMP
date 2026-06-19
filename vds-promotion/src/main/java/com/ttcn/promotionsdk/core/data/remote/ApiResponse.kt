package com.ttcn.promotionsdk.core.data.remote

import com.google.gson.annotations.SerializedName

/**
 * Envelope chung cho mọi response của Promotion API (voucher, redemption, stackable-discount).
 * [PromotionApiService] trả về `ApiResponseTemplate<T>`, [PromotionRemoteDataSource] bóc tách qua `requireData()`.
 */
data class ApiResponseTemplate<T>(
    @SerializedName("status") val status: Int? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("timestamp") val timestamp: String? = null,
    @SerializedName("metadata") val metadata: ResponseMetadata? = null,
    @SerializedName("data") val data: T? = null,
)

data class ResponseMetadata(
    @SerializedName("requestId") val requestId: String? = null,
    @SerializedName("partial") val partial: Boolean? = null,
)

/**
 * Hình dạng tối thiểu của body khi server trả HTTP lỗi (4xx/5xx) — dùng để trích `code`/`message`
 * từ `HttpException.errorBody` (lúc này envelope không deserialize được vào `ApiResponseTemplate<T>`).
 */
internal data class ApiErrorBody(
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("status") val status: Int? = null,
)
