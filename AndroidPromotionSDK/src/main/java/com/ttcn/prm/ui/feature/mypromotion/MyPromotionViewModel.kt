package com.ttcn.prm.ui.feature.mypromotion

import com.ttcn.prm.ui.base.PRMStoreViewModel
import com.ttcn.prm.ui.feature.ext.toServiceSelectorUiItem
import com.ttcn.promotionsdk.config.PromotionSDKConfig
import com.ttcn.promotionsdk.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionIntent
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionState
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionStore
import com.ttcn.promotionsdk.presentation.serviceselector.servicesForApplicableProducts

/**
 * Màn "Ưu đãi của tôi". Nghiệp vụ nằm trọn ở [MyPromotionStore] (dùng chung với iOS); lớp này chỉ
 * giữ store sống qua xoay màn — xem [PRMStoreViewModel].
 *
 * Fragment đọc thẳng [MyPromotionState] và phát thẳng [MyPromotionIntent].
 */
internal class MyPromotionViewModel(
    searchCustomerVouchersUseCase: SearchCustomerVouchersUseCase,
    private val config: PromotionSDKConfig,
) : PRMStoreViewModel<MyPromotionState, MyPromotionIntent>(
    { scope -> MyPromotionStore(searchCustomerVouchersUseCase, scope) },
) {

    /** Dịch vụ cho bottom sheet "Chọn dịch vụ" — phần thuần Android, xem `PromotionDetailViewModel`. */
    fun serviceOptions(voucher: MyVoucherListItem): List<ServiceSelectorUiItem> =
        servicesForApplicableProducts(voucher.applicableProducts, config.availableServices)
            .map { it.toServiceSelectorUiItem() }
}
