package com.ttcn.promotionsdk.data.local

import com.ttcn.promotionsdk.di.internal.get
import com.ttcn.promotionsdk.di.internal.module
import com.ttcn.promotionsdk.di.internal.single
import com.ttcn.promotionsdk.host.PromotionHostServices

/** Khai báo DI cho **tầng local** — xem ghi chú ở [com.ttcn.promotionsdk.data.remote.NetworkModule]. */
internal object LocalModule {
    val module = module {
        // Host cấp kho riêng (điển hình: DB của bản SDK native cũ) → dùng kho đó; không cấp → kho
        // mặc định của nền tảng. Nhánh này là chỗ **duy nhất** quyết định, nên `FeatureFlagLocalDataSource`
        // và tầng UI (`PromotionContainer.preferences`) tự nhiên dùng chung một kho, không thể lệch.
        single<PromotionPreferences> {
            get<PromotionHostServices>().storage ?: createPreferences()
        }
        single { FeatureFlagLocalDataSource(storage = get()) }
    }
}
