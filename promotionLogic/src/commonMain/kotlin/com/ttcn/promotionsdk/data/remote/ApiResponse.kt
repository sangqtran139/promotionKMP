package com.ttcn.promotionsdk.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Envelope chung cho mọi response của Promotion API (voucher, redemption, stackable-discount).
 * [PromotionApiService] trả về `ApiResponseTemplate<T>`, [PromotionRemoteDataSource] bóc tách qua `requireData()`.
 */
@Serializable
data class ApiResponseTemplate<T>(
    @SerialName("status") val status: Int? = null,
    @SerialName("code") val code: String? = null,
    @SerialName("success") val success: Boolean? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("timestamp") val timestamp: String? = null,
    @SerialName("metadata") val metadata: ResponseMetadata? = null,
    @SerialName("data") val data: T? = null,
)

@Serializable
data class ResponseMetadata(
    @SerialName("requestId") val requestId: String? = null,
    @SerialName("partial") val partial: Boolean? = null,
)

/**
 * Hình dạng tối thiểu của body khi server trả HTTP lỗi (4xx/5xx) — dùng để trích `code`/`message`
 * từ body của `ResponseException` (lúc này envelope không deserialize được vào `ApiResponseTemplate<T>`).
 */
@Serializable
internal data class ApiErrorBody(
    @SerialName("code") val code: String? = null,
    @SerialName("message") val message: String? = null,
)
