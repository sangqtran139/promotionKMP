package vn.viettelpay.networkkit

import io.ktor.client.call.HttpClientCall
import io.ktor.client.request.HttpRequestBuilder

/**
 * Hook vào vòng đời request→response — chạy TRƯỚC khi body được đọc, dùng cho việc cần thấy toàn bộ
 * request/response thô: retry theo điều kiện HTTP thật (khác retry mù 3 lần của
 * `RequestInterceptor`/`network-kit-android` cũ), log, đo thời gian... `:networkKit` không định nghĩa
 * chính sách nào ở đây — đăng ký qua [NetworkClientConfig.interceptors] lúc tạo client.
 */
public fun interface NetworkInterceptor {
    public suspend fun intercept(chain: InterceptorChain): HttpClientCall
}

/**
 * Model chain-of-responsibility giống OkHttp `Interceptor.Chain` — [proceed] gọi tiếp interceptor kế
 * tiếp (hoặc gửi request thật nếu đã hết danh sách), cho phép một interceptor gọi [proceed] nhiều lần
 * để tự retry.
 */
public class InterceptorChain internal constructor(
    public val request: HttpRequestBuilder,
    private val interceptors: List<NetworkInterceptor>,
    private val index: Int,
    private val send: suspend (HttpRequestBuilder) -> HttpClientCall,
) {
    public suspend fun proceed(request: HttpRequestBuilder): HttpClientCall {
        if (index >= interceptors.size) return send(request)
        val next = InterceptorChain(request, interceptors, index + 1, send)
        return interceptors[index].intercept(next)
    }
}
