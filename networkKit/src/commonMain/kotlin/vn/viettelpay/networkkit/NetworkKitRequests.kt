package vn.viettelpay.networkkit

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.appendPathSegments

/**
 * GET an toàn: mỗi phần tử [pathSegments] và mỗi giá trị trong [queryParameters] tự percent-encode
 * qua API có cấu trúc của Ktor (`appendPathSegments`/`parameter`) — tránh đúng bẫy
 * `VDORequest.Builder.buildPath()` của network-kit-android: nối chuỗi tay không encode, hỏng URL khi
 * giá trị chứa `&`/`=`/khoảng trắng/ký tự có dấu.
 */
public suspend fun HttpClient.getRequest(
    vararg pathSegments: String,
    queryParameters: Map<String, String> = emptyMap(),
): HttpResponse = get {
    url { appendPathSegments(*pathSegments) }
    queryParameters.forEach { (key, value) -> parameter(key, value) }
}

/**
 * POST cùng quy tắc encode path/query như [getRequest]. [body] truyền thẳng cho `setBody` —
 * `ContentNegotiation` (cài trong [NetworkKitHttpClient.configure]) tự serialize JSON nếu [body] là
 * kiểu `@Serializable`.
 */
public suspend fun HttpClient.postRequest(
    vararg pathSegments: String,
    queryParameters: Map<String, String> = emptyMap(),
    body: Any? = null,
): HttpResponse = post {
    url { appendPathSegments(*pathSegments) }
    queryParameters.forEach { (key, value) -> parameter(key, value) }
    if (body != null) setBody(body)
}

/**
 * [getRequest] + giải mã JSON thành [T] bằng kotlinx.serialization — [T] phải `@Serializable`.
 * Không ép envelope cụ thể nào: [T] có thể là DTO thô của consumer, hay envelope riêng của họ
 * (`ApiResponseTemplate<...>` của Promotion, `VDOBaseResponse`-style của SDK khác…) — đó là chính
 * sách của consumer (xem ranh giới ở SharedNetworkKit.md §2).
 */
public suspend inline fun <reified T> HttpClient.getJson(
    vararg pathSegments: String,
    queryParameters: Map<String, String> = emptyMap(),
): T = getRequest(*pathSegments, queryParameters = queryParameters).body()

/** [postRequest] + giải mã JSON thành [T], cùng quy tắc [getJson]. */
public suspend inline fun <reified T> HttpClient.postJson(
    vararg pathSegments: String,
    queryParameters: Map<String, String> = emptyMap(),
    body: Any? = null,
): T = postRequest(*pathSegments, queryParameters = queryParameters, body = body).body()
