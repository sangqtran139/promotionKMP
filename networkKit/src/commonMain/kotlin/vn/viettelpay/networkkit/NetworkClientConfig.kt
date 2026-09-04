package vn.viettelpay.networkkit

/**
 * Cấu hình cho MỘT [NetworkKitHttpClient] — không có state toàn cục nào khác ngoài giá trị truyền
 * vào đây. Đây là điểm khác biệt cố ý với `object VDONetworkSDK` của network-kit-android: hai host
 * dùng hai [NetworkClientConfig] khác nhau thì không thể lẫn header/timeout của nhau, vì không có gì
 * được chia sẻ qua biến toàn cục.
 */
public data class NetworkClientConfig(
    val baseUrl: String,
    val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    val headers: Map<String, String> = emptyMap(),
    val dynamicHeaders: List<DynamicHeader> = emptyList(),
    val tokenProvider: TokenProvider? = null,
) {
    public companion object {
        public const val DEFAULT_TIMEOUT_MILLIS: Long = 30_000L
    }
}
