package com.ttcn.promotionsdk.core.di

import com.ttcn.promotionsdk.core.data.remote.PromotionRemoteDataSource
import com.ttcn.promotionsdk.core.data.repository.PromotionRepositoryImpl
import com.ttcn.promotionsdk.core.domain.repository.PromotionRepository

object RepositoryModule {
    internal val module = module {
        single<PromotionRepository> {
            PromotionRepositoryImpl(
                remoteDataSource = get<PromotionRemoteDataSource>(),
            )
        }
    }
}
