package com.ttcn.promotionsdk.core.di

import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
import com.ttcn.promotionsdk.core.di.internal.SdkDi
import com.ttcn.promotionsdk.core.di.internal.get
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

    /** Nguồn token / customerId / ngôn ngữ do host cấp. Thuộc cấu hình, nên nằm ở đây. */
    val requestContextProvider: PromotionRequestContextProvider
        get() = get()
}
