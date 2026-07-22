package com.ttcn.promotionsdk.ui.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
import com.ttcn.promotionsdk.core.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.core.domain.usecase.GetCustomerVoucherDetailUseCase
import com.ttcn.promotionsdk.core.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.core.domain.usecase.ValidateStackableDiscountsUseCase
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.ChoosePromotionViewModel
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyPromotionViewModel
import com.ttcn.promotionsdk.ui.feature.promotion.promotiondetail.PromotionDetailViewModel
import com.ttcn.promotionsdk.ui.feature.promotion.searchmypromotion.SearchMyPromotionViewModel

internal class PromotionViewModelFactory(
    private val searchCustomerVouchersUseCase: SearchCustomerVouchersUseCase,
    private val validateStackableDiscountsUseCase: ValidateStackableDiscountsUseCase,
    private val getCustomerVoucherDetailUseCase: GetCustomerVoucherDetailUseCase,
    private val findEligibleCampaignsUseCase: FindEligibleCampaignsUseCase,
    private val requestContextProvider: PromotionRequestContextProvider,
    private val config: PromotionSDKConfig,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            MyPromotionViewModel::class.java -> MyPromotionViewModel(
                searchCustomerVouchersUseCase = searchCustomerVouchersUseCase,
                config = config,
            ) as T

            PromotionDetailViewModel::class.java -> PromotionDetailViewModel(
                getCustomerVoucherDetailUseCase = getCustomerVoucherDetailUseCase,
                config = config,
            ) as T

            ChoosePromotionViewModel::class.java -> ChoosePromotionViewModel(
                findEligibleCampaignsUseCase = findEligibleCampaignsUseCase,
            ) as T

            SearchMyPromotionViewModel::class.java -> SearchMyPromotionViewModel(
                searchCustomerVouchersUseCase = searchCustomerVouchersUseCase,
            ) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}