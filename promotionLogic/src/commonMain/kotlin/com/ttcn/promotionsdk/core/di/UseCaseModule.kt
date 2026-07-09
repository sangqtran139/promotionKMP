package com.ttcn.promotionsdk.core.di

import com.ttcn.promotionsdk.core.di.internal.get
import com.ttcn.promotionsdk.core.di.internal.module
import com.ttcn.promotionsdk.core.di.internal.single
import com.ttcn.promotionsdk.core.domain.usecase.CreateRedemptionSessionUseCase
import com.ttcn.promotionsdk.core.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.core.domain.usecase.GetCustomerVoucherDetailUseCase
import com.ttcn.promotionsdk.core.domain.usecase.PromotionUseCases
import com.ttcn.promotionsdk.core.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.core.domain.usecase.ValidateStackableDiscountsUseCase

object UseCaseModule {
    internal val module = module {
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
    }
}
