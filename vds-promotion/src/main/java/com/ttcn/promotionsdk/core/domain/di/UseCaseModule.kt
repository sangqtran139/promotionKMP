package com.ttcn.promotionsdk.core.domain.di

import com.ttcn.promotionsdk.core.di.get
import com.ttcn.promotionsdk.core.di.module
import com.ttcn.promotionsdk.core.di.single
import com.ttcn.promotionsdk.core.domain.usecase.CreateRedemptionSessionUseCase
import com.ttcn.promotionsdk.core.domain.usecase.GetCustomerVoucherDetailUseCase
import com.ttcn.promotionsdk.core.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.core.domain.usecase.ValidateStackableDiscountsUseCase

object UseCaseModule {
    internal val module = module {
        single {
            CreateRedemptionSessionUseCase(
                repository = get(),
            )
        }

        single {
            ValidateStackableDiscountsUseCase(
                repository = get(),
            )
        }

        single {
            SearchCustomerVouchersUseCase(
                repository = get(),
            )
        }

        single {
            GetCustomerVoucherDetailUseCase(
                repository = get()
            )
        }
    }
}