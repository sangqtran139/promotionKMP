package com.ttcn.prm.ui.feature.searchmypromotion

import com.ttcn.prm.ui.base.PRMStoreViewModel
import com.ttcn.prm.ui.feature.ext.toServiceSelectorUiItem
import com.ttcn.prm.ui.feature.mypromotion.MyVoucherListItem
import com.ttcn.prm.ui.feature.mypromotion.ServiceSelectorUiItem
import com.ttcn.promotionsdk.domain.usecase.SearchCustomerVouchersUseCase
import com.ttcn.promotionsdk.presentation.searchmypromotion.SearchMyPromotionIntent
import com.ttcn.promotionsdk.presentation.searchmypromotion.SearchMyPromotionState
import com.ttcn.promotionsdk.presentation.searchmypromotion.SearchMyPromotionStore
import com.ttcn.promotionsdk.presentation.serviceselector.configuredServicesFor

/**
 * Màn "Tìm ưu đãi". Nghiệp vụ nằm trọn ở [SearchMyPromotionStore] (dùng chung với iOS); lớp này chỉ
 * giữ store sống qua xoay màn — xem [PRMStoreViewModel].
 *
 * Fragment đọc thẳng [SearchMyPromotionState] và phát thẳng [SearchMyPromotionIntent].
 */
internal class SearchMyPromotionViewModel(
    searchCustomerVouchersUseCase: SearchCustomerVouchersUseCase,
) : PRMStoreViewModel<SearchMyPromotionState, SearchMyPromotionIntent>(
    { scope -> SearchMyPromotionStore(searchCustomerVouchersUseCase, scope) },
) {

    /** Dịch vụ cho bottom sheet "Chọn dịch vụ" — phần thuần Android, xem `PromotionDetailViewModel`. */
    fun serviceOptions(voucher: MyVoucherListItem): List<ServiceSelectorUiItem> =
        configuredServicesFor(voucher.applicableProducts).map { it.toServiceSelectorUiItem() }
}
