package vn.viettelpay.networkkit

import io.ktor.client.HttpClient

/**
 * Facade lắp ráp toàn bộ UC1–UC8 vào một điểm vào duy nhất. Khác `HttpClient.getJson`/`postJson`
 * (UC4/UC5) gọi trực tiếp — hai hàm ở đây **luôn** đi qua [networkCall] (UC6), nên lỗi transport thô
 * của Ktor không bao giờ lọt ra ngoài `NetworkClient` bằng nhầm lẫn (trước UC9, quên bọc `networkCall {}`
 * ở call site là lỗi hoàn toàn có thể xảy ra — không có gì ép buộc).
 *
 * [checkBusinessStatus] (UC7) cố tình **không** gộp vào đây — không phải mọi response đều có envelope
 * cần kiểm business status, và `chain`/`toBusinessStatus` là chính sách riêng từng consumer. Gọi tiếp
 * lên kết quả trả về khi cần:
 * ```kotlin
 * val result = networkClient.getJson<ApiResponseTemplate<VoucherDto>>("vouchers")
 *     .checkBusinessStatus(chain) { it.toBusinessStatus() }
 * ```
 */
public class NetworkClient(public val httpClient: HttpClient) {

    /** Dựng [httpClient] qua [NetworkKitHttpClient.create] — cách dùng thông thường. */
    public constructor(config: NetworkClientConfig) : this(NetworkKitHttpClient.create(config))

    public suspend inline fun <reified T> getJson(
        vararg pathSegments: String,
        queryParameters: Map<String, String> = emptyMap(),
    ): T = networkCall { httpClient.getJson(*pathSegments, queryParameters = queryParameters) }

    public suspend inline fun <reified T> postJson(
        vararg pathSegments: String,
        queryParameters: Map<String, String> = emptyMap(),
        body: Any? = null,
    ): T = networkCall { httpClient.postJson(*pathSegments, queryParameters = queryParameters, body = body) }
}
