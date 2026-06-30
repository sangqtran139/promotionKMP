package com.ttcn.promotionsdk.ui.di

import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
import com.ttcn.promotionsdk.core.di.internal.get
import com.ttcn.promotionsdk.core.di.internal.module
import com.ttcn.promotionsdk.core.di.internal.single

object ViewModelModule {
    internal val module = module {
        single<PromotionViewModelFactory> {
            PromotionViewModelFactory(
                searchCustomerVouchersUseCase = get(),
                validateStackableDiscountsUseCase = get(),
                getCustomerVoucherDetailUseCase = get(),
                requestContextProvider = get(),
                config = get<PromotionSDKConfig>(),
            )
        }
    }
}