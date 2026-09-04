package vn.viettelpay.networkkit

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.util.appendIfNameAbsent

/**
 * Dựng [HttpClient] theo [NetworkClientConfig] — không chỉ định engine, Ktor tự chọn theo artifact có
 * trên classpath (`ktor-client-okhttp` ở androidMain, `ktor-client-darwin` ở iosMain), giống hệt cách
 * `PromotionHttpClient` của `:promotionLogic` đang làm. Vì thế không cần `expect`/`actual` ở đây.
 *
 * Mỗi lời gọi [create] trả về một `HttpClient` độc lập — không có singleton nào giữ config dùng
 * chung giữa các lời gọi (khác `object VDONetworkSDK` của network-kit-android).
 */
public object NetworkKitHttpClient {

    public fun create(config: NetworkClientConfig): HttpClient = HttpClient {
        configure(config)
    }

    /**
     * Tách khỏi [create] để test dựng được cùng cấu hình trên `MockEngine`.
     */
    internal fun HttpClientConfig<*>.configure(config: NetworkClientConfig) {
        install(HttpTimeout) {
            connectTimeoutMillis = config.timeoutMillis
            requestTimeoutMillis = config.timeoutMillis
            socketTimeoutMillis = config.timeoutMillis
        }

        defaultRequest {
            url(config.baseUrl.ensureTrailingSlash())

            // appendIfNameAbsent: chạy mỗi request (nên dynamicHeaders luôn tính giá trị mới), nhưng
            // không ghi đè header caller đã tự đặt ở lời gọi cụ thể — cùng quy tắc PromotionHttpClient
            // đang dùng, xem NetworkingGuide.md §2.
            config.headers.forEach { (name, value) ->
                headers.appendIfNameAbsent(name, value)
            }
            config.dynamicHeaders.forEach { dynamicHeader ->
                headers.appendIfNameAbsent(dynamicHeader.name, dynamicHeader.currentValue())
            }

            config.tokenProvider?.provideToken()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { headers.appendIfNameAbsent(HttpHeaders.Authorization, it.toBearerToken()) }
        }
    }

    // Ktor nối path tương đối vào URL nền theo luật URL chuẩn: thiếu dấu "/" cuối thì segment cuối
    // của baseUrl bị THAY THẾ thay vì được nối tiếp (vd "https://a.com/base" + "sub" -> ".../sub",
    // mất "base"). Cùng bẫy `PromotionHttpClient` đã gặp.
    private fun String.ensureTrailingSlash(): String = if (endsWith("/")) this else "$this/"

    private fun String.toBearerToken(): String =
        if (startsWith(BEARER_PREFIX, ignoreCase = true)) this else "$BEARER_PREFIX$this"

    private const val BEARER_PREFIX = "Bearer "
}
