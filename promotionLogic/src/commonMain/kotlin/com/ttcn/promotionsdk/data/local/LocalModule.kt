package com.ttcn.promotionsdk.data.local

import com.ttcn.promotionsdk.di.internal.get
import com.ttcn.promotionsdk.di.internal.module
import com.ttcn.promotionsdk.di.internal.single

/** Khai báo DI cho **tầng local** — xem ghi chú ở [com.ttcn.promotionsdk.data.remote.NetworkModule]. */
internal object LocalModule {
    val module = module {
        single<PromotionPreferences> { createPreferences() }
        single { FeatureFlagLocalDataSource(storage = get()) }
    }
}
