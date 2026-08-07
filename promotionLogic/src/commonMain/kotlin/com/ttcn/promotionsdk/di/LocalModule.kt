package com.ttcn.promotionsdk.di

import com.ttcn.promotionsdk.data.local.FeatureFlagLocalDataSource
import com.ttcn.promotionsdk.data.local.PromotionPreferences
import com.ttcn.promotionsdk.data.local.createPreferences
import com.ttcn.promotionsdk.di.internal.get
import com.ttcn.promotionsdk.di.internal.module
import com.ttcn.promotionsdk.di.internal.single

object LocalModule {
    internal val module = module {
        single<PromotionPreferences> { createPreferences() }
        single { FeatureFlagLocalDataSource(storage = get()) }
    }
}
