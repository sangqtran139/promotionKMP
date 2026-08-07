package com.ttcn.promotionsdk.data.repository

import com.ttcn.promotionsdk.data.remote.PromotionRemoteDataSource
import com.ttcn.promotionsdk.di.internal.get
import com.ttcn.promotionsdk.di.internal.module
import com.ttcn.promotionsdk.di.internal.single
import com.ttcn.promotionsdk.domain.repository.FeatureFlagRepository
import com.ttcn.promotionsdk.domain.repository.PromotionRepository

/** Khai báo DI cho **tầng repository** — xem ghi chú ở [com.ttcn.promotionsdk.data.remote.NetworkModule]. */
internal object RepositoryModule {
    val module = module {
        single<PromotionRepository> {
            PromotionRepositoryImpl(
                remoteDataSource = get<PromotionRemoteDataSource>(),
            )
        }

        single<FeatureFlagRepository> {
            FeatureFlagRepositoryImpl(
                remoteDataSource = get(),
                localDataSource = get(),
            )
        }
    }
}
