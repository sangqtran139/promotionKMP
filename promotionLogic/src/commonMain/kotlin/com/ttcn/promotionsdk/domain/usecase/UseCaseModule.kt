package com.ttcn.promotionsdk.domain.usecase

import com.ttcn.promotionsdk.di.internal.get
import com.ttcn.promotionsdk.di.internal.module
import com.ttcn.promotionsdk.di.internal.single

/** Khai báo DI cho **tầng use case** — xem ghi chú ở [com.ttcn.promotionsdk.data.remote.NetworkModule]. */
internal object UseCaseModule {
    val module = module {
        single { CreateRedemptionSessionUseCase(repository = get()) }
        single { ValidateStackableDiscountsUseCase(repository = get()) }
        single { SearchCustomerVouchersUseCase(repository = get()) }
        single { GetCustomerVoucherDetailUseCase(repository = get()) }
        single { FindEligibleCampaignsUseCase(repository = get()) }

        single {
            PromotionUseCases(
                searchVouchersUseCase = get(),
                voucherDetailUseCase = get(),
                validateDiscountsUseCase = get(),
                createRedemptionUseCase = get(),
                findEligibleCampaignsUseCase = get(),
            )
        }

        // ─── Feature flag ─────────────────────────────────────────────────────
        single { FetchFeatureFlagsUseCase(repository = get()) }
        single { IsFeatureEnabledUseCase(repository = get()) }
        single { GetFeatureFlagsUseCase(repository = get()) }
        single { GetPromotionFeatureFlagsUseCase(repository = get()) }

        single {
            PromotionFeatureFlagUseCases(
                fetchFeatureFlags = get(),
                isFeatureEnabled = get(),
                getFeatureFlags = get(),
                getPromotionFeatureFlags = get(),
            )
        }
    }
}
