package vn.viettelpay.networkkit

/**
 * Nguồn access token cho một [NetworkClientConfig] — đồng bộ, giống hệt
 * `PromotionRequestContextProvider.getAccessToken()` của `:promotionLogic` (không có refresh-token
 * flow ở đây; đó là mối quan tâm của tầng consumer, gắn qua business status-code handler ở UC7).
 * Trả `null` hoặc chuỗi rỗng/toàn khoảng trắng thì không gắn header `Authorization`.
 */
public fun interface TokenProvider {
    public fun provideToken(): String?
}
