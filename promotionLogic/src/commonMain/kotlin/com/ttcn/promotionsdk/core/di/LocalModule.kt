package com.ttcn.promotionsdk.core.di

import com.ttcn.promotionsdk.core.data.local.FeatureFlagLocalDataSource
import com.ttcn.promotionsdk.core.data.local.KeyValueStorage
import com.ttcn.promotionsdk.core.data.local.createKeyValueStorage
import com.ttcn.promotionsdk.core.di.internal.get
import com.ttcn.promotionsdk.core.di.internal.module
import com.ttcn.promotionsdk.core.di.internal.single

object LocalModule {
    internal val module = module {
        single<KeyValueStorage> { createKeyValueStorage() }
        single { FeatureFlagLocalDataSource(storage = get()) }
    }
}
