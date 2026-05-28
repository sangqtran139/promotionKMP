package com.ttcn.promotionsdk.ui.feature.promotion.searchmypromotion

import com.ttcn.promotionsdk.ui.base.PRMBaseViewModel

class SearchMyPromotionViewModel :
    PRMBaseViewModel<SearchMyPromotionUiState, SearchMyPromotionAction, SearchMyPromotionEffect>(
        SearchMyPromotionUiState(),
    ) {

    override fun handleAction(action: SearchMyPromotionAction) = Unit
}
