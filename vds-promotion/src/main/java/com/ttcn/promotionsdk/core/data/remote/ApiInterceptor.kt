package com.ttcn.promotionsdk.core.data.remote

import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID

class ApiInterceptor(
    private val requestContextProvider: PromotionRequestContextProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        val token = resolveToken(requestContextProvider)
        if (!token.isNullOrBlank() && originalRequest.header(HEADER_AUTHORIZATION)
                .isNullOrBlank()
        ) {
            requestBuilder.header(HEADER_AUTHORIZATION, token.toBearerToken())
        }

        if (originalRequest.header(HEADER_REQUEST_ID).isNullOrBlank()) {
            requestBuilder.header(HEADER_REQUEST_ID, UUID.randomUUID().toString())
        }

        if (originalRequest.header(HEADER_ACCEPT_LANGUAGE).isNullOrBlank()) {
            requestBuilder.header(HEADER_ACCEPT_LANGUAGE, resolveLanguage(requestContextProvider))
        }

        if (originalRequest.header(HEADER_ACCEPT).isNullOrBlank()) {
            requestBuilder.header(HEADER_ACCEPT, CONTENT_TYPE_JSON)
        }

        if (originalRequest.body != null && originalRequest.header(HEADER_CONTENT_TYPE)
                .isNullOrBlank()
        ) {
            requestBuilder.header(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
        }

        return chain.proceed(requestBuilder.build())
    }

    private fun String.toBearerToken(): String {
        return if (startsWith(BEARER_PREFIX, ignoreCase = true)) this else "$BEARER_PREFIX$this"
    }

    private fun resolveToken(requestContextProvider: PromotionRequestContextProvider): String? {
        return requestContextProvider.getAccessToken()?.trim().takeIf { !it.isNullOrBlank() }
    }

    private fun resolveLanguage(requestContextProvider: PromotionRequestContextProvider): String {
        return requestContextProvider
            .getLanguage()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_LANGUAGE
    }

    private companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
        const val HEADER_REQUEST_ID = "X-Request-ID"
        const val HEADER_ACCEPT_LANGUAGE = "Accept-Language"
        const val HEADER_ACCEPT = "Accept"
        const val HEADER_CONTENT_TYPE = "Content-Type"

        const val BEARER_PREFIX = "Bearer "
        const val CONTENT_TYPE_JSON = "application/json"
        const val DEFAULT_LANGUAGE = "vi-VN"
    }
}
