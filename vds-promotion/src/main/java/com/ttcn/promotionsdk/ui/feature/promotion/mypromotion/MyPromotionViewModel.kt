package com.ttcn.promotionsdk.ui.feature.promotion.mypromotion

import com.ttcn.promotionsdk.ui.base.PRMBaseViewModel

class MyPromotionViewModel :
    PRMBaseViewModel<MyPromotionUiState, MyPromotionAction, MyPromotionEffect>(
        MyPromotionUiState(),
    ) {

    override fun handleAction(action: MyPromotionAction) = Unit
}
