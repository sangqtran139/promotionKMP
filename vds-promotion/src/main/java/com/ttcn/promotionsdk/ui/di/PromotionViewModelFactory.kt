package com.ttcn.promotionsdk.ui.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.domain.repository.PromotionRepository
import com.ttcn.promotionsdk.core.domain.usecase.CreateRedemptionSessionUseCase
import com.ttcn.promotionsdk.core.domain.usecase.ValidateStackableDiscountsUseCase
import com.ttcn.promotionsdk.ui.feature.promotion.PaymentIntegrateViewModel
import com.ttcn.promotionsdk.ui.feature.promotion.RedemptionViewModel
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.ChoosePromotionViewModel
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyPromotionViewModel
import com.ttcn.promotionsdk.ui.feature.promotion.promotiondetail.PromotionDetailViewModel
import com.ttcn.promotionsdk.ui.feature.promotion.searchmypromotion.SearchMyPromotionViewModel

class PromotionViewModelFactory(
    private val promotionRepository: PromotionRepository,
    private val requestContextProvider: PromotionRequestContextProvider,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            MyPromotionViewModel::class.java -> {
                MyPromotionViewModel(
                    repository = promotionRepository,
                    requestContextProvider = requestContextProvider,
                ) as T
            }

            PromotionDetailViewModel::class.java -> {
                PromotionDetailViewModel(
                    repository = promotionRepository,
                    requestContextProvider = requestContextProvider,
                ) as T
            }

            ChoosePromotionViewModel::class.java -> {
                ChoosePromotionViewModel(
                    repository = promotionRepository,
                    requestContextProvider = requestContextProvider,
                ) as T
            }

            PaymentIntegrateViewModel::class.java -> {
                PaymentIntegrateViewModel(
                    repository = promotionRepository,
                    requestContextProvider = requestContextProvider,
                ) as T
            }

            RedemptionViewModel::class.java -> {
                RedemptionViewModel(
                    validateStackableDiscountsUseCase = ValidateStackableDiscountsUseCase(
                        promotionRepository
                    ),
                    createRedemptionSessionUseCase = CreateRedemptionSessionUseCase(
                        promotionRepository
                    ),
                    requestContextProvider = requestContextProvider,
                ) as T
            }

            SearchMyPromotionViewModel::class.java -> {
                SearchMyPromotionViewModel(
                    repository = promotionRepository,
                    requestContextProvider = requestContextProvider,
                ) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}