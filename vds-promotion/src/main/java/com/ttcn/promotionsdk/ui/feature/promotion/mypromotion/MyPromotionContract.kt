package com.ttcn.promotionsdk.ui.feature.promotion.mypromotion

data class MyPromotionUiState(
    val isLoading: Boolean = false,
)

sealed interface MyPromotionAction

sealed interface MyPromotionEffect
