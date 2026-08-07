package com.ttcn.promotionsdk.di

import com.ttcn.promotionsdk.data.remote.PromotionRemoteDataSource
import com.ttcn.promotionsdk.data.repository.PromotionRepositoryImpl
import com.ttcn.promotionsdk.di.internal.get
import com.ttcn.promotionsdk.di.internal.module
import com.ttcn.promotionsdk.di.internal.single
import com.ttcn.promotionsdk.domain.repository.PromotionRepository

object RepositoryModule {
    internal val module = module {
        single<PromotionRepository> {
            PromotionRepositoryImpl(
                remoteDataSource = get<PromotionRemoteDataSource>(),
            )
        }
    }
}
