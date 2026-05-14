package com.ttcn.promotionsdk.ui.feature.promotion.promotiondetail

data class PromotionDetailUiState(
    val isLoading: Boolean = false,
)

sealed interface PromotionDetailAction

sealed interface PromotionDetailEffect
