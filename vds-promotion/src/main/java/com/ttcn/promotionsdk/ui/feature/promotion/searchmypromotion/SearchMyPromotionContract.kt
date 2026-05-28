package com.ttcn.promotionsdk.ui.feature.promotion.searchmypromotion

data class SearchMyPromotionUiState(
    val query: String = "",
    val isLoading: Boolean = false,
)

sealed interface SearchMyPromotionAction

sealed interface SearchMyPromotionEffect
