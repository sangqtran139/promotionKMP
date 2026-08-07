package com.ttcn.promotionsdk.di

import com.ttcn.promotionsdk.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.config.PromotionSDKConfig
import com.ttcn.promotionsdk.data.local.PromotionPreferences
import com.ttcn.promotionsdk.di.internal.SdkDi
import com.ttcn.promotionsdk.di.internal.get
import io.ktor.client.HttpClient
import kotlin.concurrent.Volatile

/**
 * Khởi tạo và dọn dẹp Promotion SDK. Dùng chung cho Android và iOS.
 *
 * Container **chỉ** lo vòng đời và cấu hình. Nó không phơi ra use case — UI dựng thẳng use case
 * mà nó cần, giống cách `GetMyPromotionUseCase()` hoạt động ở bản iOS:
 *
 * ```kotlin
 * PromotionContainer.initialize(PromotionSDKConfig(apiKey = "...", baseUrl = "..."))
 *
 * val useCase = SearchCustomerVouchersUseCase()   // tự lấy repository từ đồ thị đã init
 * val result = useCase(request)
 * ```
 *
 * Trên Android dùng overload `initialize(context, config)` ở androidMain — nó nạp `applicationContext`
 * cho `SharedPreferences` và suy ra cờ debug.
 */
object PromotionContainer {

    @Volatile
    private var config: PromotionSDKConfig? = null

    fun initialize(config: PromotionSDKConfig) {
        this.config = config
        SdkDi.getInstance().start(
            config = config,
            NetworkModule.module,
            LocalModule.module,
            RepositoryModule.module,
            UseCaseModule.module,
            FeatureFlagModule.module,
        )
    }

    fun clear() {
        // Chỉ đóng client nếu nó đã thực sự được dựng — resolve() thẳng sẽ tạo mới rồi đóng ngay.
        if (SdkDi.getInstance().hasInstance(HttpClient::class)) {
            runCatching { get<HttpClient>().close() }
        }
        SdkDi.getInstance().clear()
        clearPlatformState()
        config = null
    }

    fun isInitialized(): Boolean = config != null

    fun requireConfig(): PromotionSDKConfig = requireNotNull(config) {
        "PromotionSDKConfig is unavailable. Call PromotionContainer.initialize() first."
    }

    /** Nguồn token / ngôn ngữ / context đơn hàng do host cấp. Thuộc cấu hình, nên nằm ở đây. */
    val requestContextProvider: PromotionRequestContextProvider
        get() = get()

    /**
     * Kho key-value dùng chung của SDK, cho tầng UI persist cấu hình (theme…). **Một cơ chế lưu**
     * duy nhất, chung với FeatureFlag, giống nhau trên Android và iOS. Chỉ đọc được sau
     * [initialize]; tầng UI tự đặt key (vd `PromotionThemeStore`). Xem [PromotionPreferences].
     */
    val preferences: PromotionPreferences
        get() = get()
}
