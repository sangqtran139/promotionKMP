package com.ttcn.prm.ui.di

import com.ttcn.promotionsdk.core.di.PromotionContainer
import com.ttcn.promotionsdk.core.domain.usecase.FindEligibleCampaignsUseCase
import com.ttcn.promotionsdk.core.domain.usecase.GetCustomerVoucherDetailUseCase
import com.ttcn.promotionsdk.core.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.core.domain.usecase.ValidateStackableDiscountsUseCase

/**
 * Dựng [PromotionViewModelFactory] mà không cần DI container của `promotionLogic`.
 *
 * `promotionLogic` chỉ phơi ra `PromotionContainer` (init + config) và các use case dựng thẳng được.
 * DSL `module/single/get` là chi tiết nội bộ của nó, không thấy được từ module này. Use case tự lấy
 * repository từ đồ thị đã dựng bởi `PromotionContainer.initialize(...)`.
 */
internal fun promotionViewModelFactory(): PromotionViewModelFactory =
    PromotionViewModelFactory(
        searchCustomerVouchersUseCase = SearchCustomerVouchersUseCase(),
        validateStackableDiscountsUseCase = ValidateStackableDiscountsUseCase(),
        getCustomerVoucherDetailUseCase = GetCustomerVoucherDetailUseCase(),
        findEligibleCampaignsUseCase = FindEligibleCampaignsUseCase(),
        config = PromotionContainer.requireConfig(),
    )
