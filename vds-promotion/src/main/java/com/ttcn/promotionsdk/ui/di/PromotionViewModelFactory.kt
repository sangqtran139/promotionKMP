package com.ttcn.promotionsdk.ui.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.domain.usecase.GetCustomerVoucherDetailUseCase
import com.ttcn.promotionsdk.core.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.core.domain.usecase.ValidateStackableDiscountsUseCase
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.ChoosePromotionViewModel
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyPromotionViewModel
import com.ttcn.promotionsdk.ui.feature.promotion.promotiondetail.PromotionDetailViewModel
import com.ttcn.promotionsdk.ui.feature.promotion.searchmypromotion.SearchMyPromotionViewModel

class PromotionViewModelFactory(
    private val searchCustomerVouchersUseCase: SearchCustomerVouchersUseCase,
    private val validateStackableDiscountsUseCase: ValidateStackableDiscountsUseCase,
    private val getCustomerVoucherDetailUseCase: GetCustomerVoucherDetailUseCase,
    private val requestContextProvider: PromotionRequestContextProvider,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            MyPromotionViewModel::class.java -> MyPromotionViewModel(
                searchCustomerVouchersUseCase = searchCustomerVouchersUseCase,
                requestContextProvider = requestContextProvider,
            ) as T

            PromotionDetailViewModel::class.java -> PromotionDetailViewModel(
                getCustomerVoucherDetailUseCase = getCustomerVoucherDetailUseCase,
                requestContextProvider = requestContextProvider,
            ) as T

            ChoosePromotionViewModel::class.java -> ChoosePromotionViewModel(
                searchCustomerVouchersUseCase = searchCustomerVouchersUseCase,
                validateStackableDiscountsUseCase = validateStackableDiscountsUseCase,
                requestContextProvider = requestContextProvider,
            ) as T

            SearchMyPromotionViewModel::class.java -> SearchMyPromotionViewModel(
                searchCustomerVouchersUseCase = searchCustomerVouchersUseCase,
                requestContextProvider = requestContextProvider,
            ) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}