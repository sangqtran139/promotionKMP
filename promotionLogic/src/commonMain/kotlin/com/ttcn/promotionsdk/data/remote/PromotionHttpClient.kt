package com.ttcn.promotionsdk.data.remote

import com.ttcn.promotionsdk.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.common.randomUuidString
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.appendIfNameAbsent
import kotlinx.serialization.json.Json

/**
 * Thay cho `RetrofitClient` của bản Android. Engine được Ktor chọn theo artifact có trên classpath
 * (OkHttp ở androidMain, Darwin ở iosMain), nên không cần `expect`/`actual` ở đây.
 */
internal object PromotionHttpClient {

    private const val TIMEOUT_MILLIS = 30_000L

    /**
     * Ba cờ dưới đây tái hiện hành vi của Gson ở bản Retrofit cũ. Bỏ bất kỳ cờ nào cũng gây lỗi
     * **im lặng lúc build, nổ lúc chạy**:
     *
     * - `encodeDefaults` — Gson ghi cả giá trị mặc định. Thiếu nó, request `createRedemption`
     *   mất `sessionOptions`.
     * - `explicitNulls = false` — Gson bỏ qua field null.
     * - `isLenient` — **quan trọng nhất**. Server trả số cho các field tiền tệ
     *   (`"originalAmount": 500000`), trong khi DTO khai `String`. Gson tự ép số sang chuỗi;
     *   kotlinx.serialization thì ném `JsonDecodingException`. Có hơn 20 field như vậy
     *   (`totalDiscount`, `finalAmount`, `calculatedDiscount`, `estimatedDiscount`, …).
     *   Bản iOS cũ giải quyết bằng `FlexibleString.swift` — ở đây một cờ là đủ.
     */
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        isLenient = true
    }

    fun create(
        baseUrl: String,
        requestContextProvider: PromotionRequestContextProvider,
        isDebug: Boolean = false,
    ): HttpClient = HttpClient {
        configure(baseUrl, requestContextProvider, isDebug)
    }

    /**
     * Tách khỏi [create] để test dựng được cùng cấu hình trên `MockEngine`.
     */
    fun HttpClientConfig<*>.configure(
        baseUrl: String,
        requestContextProvider: PromotionRequestContextProvider,
        isDebug: Boolean = false,
    ) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(json)
        }

        install(HttpTimeout) {
            connectTimeoutMillis = TIMEOUT_MILLIS
            requestTimeoutMillis = TIMEOUT_MILLIS
            socketTimeoutMillis = TIMEOUT_MILLIS
        }

        install(Logging) {
            level = if (isDebug) LogLevel.BODY else LogLevel.NONE
        }

        // Chỉ bật khi debug: in thêm mỗi request dạng lệnh cURL copy-paste được (chứa cả Bearer token).
        if (isDebug) install(PromotionCurlLogging)

        defaultRequest {
            url(baseUrl.ensureTrailingSlash())

            requestContextProvider.getAccessToken()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { headers.appendIfNameAbsent(HttpHeaders.Authorization, it.toBearerToken()) }

            headers.appendIfNameAbsent(HEADER_REQUEST_ID, randomUuidString())
            headers.appendIfNameAbsent(
                HttpHeaders.AcceptLanguage,
                requestContextProvider.resolveLanguage(),
            )
            headers.appendIfNameAbsent(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }
    }

    private fun String.toBearerToken(): String =
        if (startsWith(BEARER_PREFIX, ignoreCase = true)) this else "$BEARER_PREFIX$this"

    private fun PromotionRequestContextProvider.resolveLanguage(): String =
        getLanguage()?.trim()?.takeIf { it.isNotBlank() } ?: DEFAULT_LANGUAGE

    private fun String.ensureTrailingSlash(): String =
        if (endsWith("/")) this else "$this/"

    private const val HEADER_REQUEST_ID = "X-Request-ID"
    private const val BEARER_PREFIX = "Bearer "
    private const val DEFAULT_LANGUAGE = "vi-VN"
}
