package com.ttcn.promotionsdk.ui.feature.promotion.promotiondetail

import com.ttcn.promotionsdk.ui.base.PRMBaseViewModel

class PromotionDetailViewModel :
    PRMBaseViewModel<PromotionDetailUiState, PromotionDetailAction, PromotionDetailEffect>(
        PromotionDetailUiState(),
    ) {

    override fun handleAction(action: PromotionDetailAction) = Unit
}
