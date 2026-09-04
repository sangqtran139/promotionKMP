package vn.viettelpay.networkkit

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.url

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
        }
    }

    // Ktor nối path tương đối vào URL nền theo luật URL chuẩn: thiếu dấu "/" cuối thì segment cuối
    // của baseUrl bị THAY THẾ thay vì được nối tiếp (vd "https://a.com/base" + "sub" -> ".../sub",
    // mất "base"). Cùng bẫy `PromotionHttpClient` đã gặp.
    private fun String.ensureTrailingSlash(): String = if (endsWith("/")) this else "$this/"
}
