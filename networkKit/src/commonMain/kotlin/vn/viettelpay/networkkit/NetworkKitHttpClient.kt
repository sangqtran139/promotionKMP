package vn.viettelpay.networkkit

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.appendIfNameAbsent
import kotlinx.serialization.json.Json

/**
 * Dựng [HttpClient] theo [NetworkClientConfig] — không chỉ định engine, Ktor tự chọn theo artifact có
 * trên classpath (`ktor-client-okhttp` ở androidMain, `ktor-client-darwin` ở iosMain), giống hệt cách
 * `PromotionHttpClient` của `:promotionLogic` đang làm. Vì thế không cần `expect`/`actual` ở đây.
 *
 * Mỗi lời gọi [create] trả về một `HttpClient` độc lập — không có singleton nào giữ config dùng
 * chung giữa các lời gọi (khác `object VDONetworkSDK` của network-kit-android).
 */
public object NetworkKitHttpClient {

    public fun create(config: NetworkClientConfig): HttpClient {
        val client = HttpClient { configure(config) }
        client.applyInterceptors(config)
        return client
    }

    /**
     * Tách khỏi [create] để test dựng được cùng cấu hình trên `MockEngine`.
     */
    internal fun HttpClientConfig<*>.configure(config: NetworkClientConfig) {
        // Bắt buộc để 4xx/5xx ném ResponseException — networkCall {} (UC6) dựa vào đây để phân loại
        // thành NetworkError.Http, thay vì consumer phải tự kiểm response.status thủ công.
        expectSuccess = true

        install(HttpTimeout) {
            connectTimeoutMillis = config.timeoutMillis
            requestTimeoutMillis = config.timeoutMillis
            socketTimeoutMillis = config.timeoutMillis
        }

        install(ContentNegotiation) {
            json(json)
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

    /**
     * `HttpSend` là plugin lõi Ktor **luôn có sẵn** trên mọi `HttpClient` — không `install()` được
     * interceptor qua `HttpClientConfig` (config block của nó chỉ có `maxSendCount`, không có
     * `intercept()`); phải gọi `client.plugin(HttpSend).intercept { }` sau khi client đã dựng xong.
     * Tách riêng khỏi [configure] (không gộp vào [create]) để test cũng gọi được trên client dựng
     * bằng `MockEngine`. Danh sách rỗng (mặc định) thì không làm gì — không tốn gì thêm cho consumer
     * không dùng tính năng này.
     */
    internal fun HttpClient.applyInterceptors(config: NetworkClientConfig) {
        if (config.interceptors.isEmpty()) return
        plugin(HttpSend).intercept { request ->
            // Chụp lại Sender vào biến cục bộ: bên trong lambda `send` dưới đây có hai receiver ẩn
            // cùng lồng nhau (Sender của intercept{} và HttpClient của applyInterceptors) — gọi tay
            // qua `sender` để chắc chắn dùng Sender.execute (public), không lẫn HttpClient.execute
            // (internal, chữ ký khác).
            val sender = this
            InterceptorChain(
                request = request,
                interceptors = config.interceptors,
                index = 0,
                send = { req -> sender.execute(req) },
            ).proceed(request)
        }
    }

    // Ktor nối path tương đối vào URL nền theo luật URL chuẩn: thiếu dấu "/" cuối thì segment cuối
    // của baseUrl bị THAY THẾ thay vì được nối tiếp (vd "https://a.com/base" + "sub" -> ".../sub",
    // mất "base"). Cùng bẫy `PromotionHttpClient` đã gặp.
    private fun String.ensureTrailingSlash(): String = if (endsWith("/")) this else "$this/"

    private fun String.toBearerToken(): String =
        if (startsWith(BEARER_PREFIX, ignoreCase = true)) this else "$BEARER_PREFIX$this"

    private const val BEARER_PREFIX = "Bearer "

    /**
     * Ba cờ dưới đây tái hiện hành vi Gson mà mọi backend trong hệ sinh thái Viettel vẫn đang phục
     * vụ (`network-kit-android` cũ cũng dùng Gson) — cùng lý do, cùng bốn cờ như `PromotionHttpClient`
     * của `:promotionLogic` (xem NetworkingGuide.md §2). `isLenient` quan trọng nhất: server trả số
     * cho field tiền tệ khai kiểu String (`"originalAmount": 500000`) là chuyện thường, không phải lỗi
     * server.
     */
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        isLenient = true
    }
}
