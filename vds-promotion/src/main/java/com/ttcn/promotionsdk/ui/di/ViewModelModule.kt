package com.ttcn.promotionsdk.ui.di

import com.ttcn.promotionsdk.core.di.get
import com.ttcn.promotionsdk.core.di.module
import com.ttcn.promotionsdk.core.di.single

object ViewModelModule {
    internal val module = module {
        single<PromotionViewModelFactory> {
            PromotionViewModelFactory(
                promotionRepository = get(),
                requestContextProvider = get(),
            )
        }
    }
}
