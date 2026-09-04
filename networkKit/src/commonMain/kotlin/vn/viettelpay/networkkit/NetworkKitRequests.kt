package vn.viettelpay.networkkit

import io.ktor.client.HttpClient
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
 * POST cùng quy tắc encode path/query như [getRequest]. [body] truyền thẳng cho `setBody` — serialize
 * JSON là việc của UC5 (`ContentNegotiation`), module chưa cấu hình ở đây.
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
