package com.ttcn.promotionsdk.core.di

import android.content.Context
import com.ttcn.promotionsdk.core.data.local.FeatureFlagLocalDataSource
import com.ttcn.promotionsdk.core.data.local.SharedPrefStorage
import com.ttcn.promotionsdk.core.di.internal.get
import com.ttcn.promotionsdk.core.di.internal.module
import com.ttcn.promotionsdk.core.di.internal.single

object LocalModule {
    internal val module = module {
        single { SharedPrefStorage(context = get<Context>()) }
        single { FeatureFlagLocalDataSource(storage = get()) }
    }
}
