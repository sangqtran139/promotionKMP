package com.ttcn.promotionsdk.data.remote

import com.ttcn.promotionsdk.config.EmptyPromotionRequestContextProvider
import com.ttcn.promotionsdk.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.config.PromotionSDKConfig
import com.ttcn.promotionsdk.di.internal.get
import com.ttcn.promotionsdk.di.internal.module
import com.ttcn.promotionsdk.di.internal.single
import io.ktor.client.HttpClient

/**
 * Khai báo DI cho **tầng remote** — nằm cùng package với chính các class nó dựng, không dồn vào
 * `di/`: sửa `PromotionApiService` thì chỗ khai nó nằm ngay bên cạnh, khỏi phải nhớ đi tìm.
 *
 * `PromotionContainer.initialize` gom các module lại theo đúng thứ tự lớp (remote → local →
 * repository → use case).
 */
internal object NetworkModule {
    val module = module {
        single<PromotionRequestContextProvider> {
            get<PromotionSDKConfig>().requestContextProvider
                ?: EmptyPromotionRequestContextProvider()
        }

        single<HttpClient> {
            val config = get<PromotionSDKConfig>()
            PromotionHttpClient.create(
                baseUrl = config.baseUrl,
                requestContextProvider = get(),
                isDebug = config.isDebug,
            )
        }

        single<PromotionApiService> {
            KtorPromotionApiService(client = get())
        }

        single<PromotionRemoteDataSource> {
            PromotionRemoteDataSource(
                apiService = get(),
                // Cùng `PromotionRequestContextProvider` mà `HttpClient` đọc token — nhờ vậy lượt
                // thử lại sau refresh nhặt đúng token mới. Xem [TokenRefreshGate].
                tokenRefreshGate = TokenRefreshGate(contextProvider = get()),
            )
        }

        // ─── Feature flag ─────────────────────────────────────────────────────
        // Cùng tầng remote nên khai luôn ở đây, thay vì một `FeatureFlagModule` cắt ngang bốn lớp.
        single<FeatureFlagApiService> { KtorFeatureFlagApiService(client = get()) }
        single { FeatureFlagRemoteDataSource(apiService = get()) }
    }
}
