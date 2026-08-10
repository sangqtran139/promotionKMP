package com.ttcn.prm.ui.feature.promotiondetail

import com.ttcn.prm.ui.base.PRMStoreViewModel
import com.ttcn.prm.ui.feature.ext.toServiceSelectorUiItem
import com.ttcn.prm.ui.feature.mypromotion.ServiceSelectorUiItem
import com.ttcn.promotionsdk.domain.usecase.GetCustomerVoucherDetailUseCase
import com.ttcn.promotionsdk.presentation.promotiondetail.PromotionDetailIntent
import com.ttcn.promotionsdk.presentation.promotiondetail.PromotionDetailState
import com.ttcn.promotionsdk.presentation.promotiondetail.PromotionDetailStore
import com.ttcn.promotionsdk.presentation.serviceselector.configuredServicesFor

/**
 * Màn "Chi tiết ưu đãi". Nghiệp vụ nằm trọn ở [PromotionDetailStore] (dùng chung với iOS); lớp này
 * chỉ giữ store sống qua xoay màn — xem [PRMStoreViewModel].
 *
 * Fragment đọc thẳng [PromotionDetailState] và phát thẳng [PromotionDetailIntent]. Trước kia ở đây
 * có `PromotionDetailUiState` chép nguyên si sáu field của store (kể cả `detail = detail`), một
 * `PromotionDetailAction` song sinh với `PromotionDetailIntent`, cùng `bindStore`/`render`/
 * `handleError` giống hệt ba màn còn lại — bỏ hết.
 */
internal class PromotionDetailViewModel(
    getCustomerVoucherDetailUseCase: GetCustomerVoucherDetailUseCase,
) : PRMStoreViewModel<PromotionDetailState, PromotionDetailIntent>(
    { scope -> PromotionDetailStore(getCustomerVoucherDetailUseCase, scope) },
) {

    /**
     * Danh sách dịch vụ cho bottom sheet "Chọn dịch vụ" — phần **thuần Android** ở đây chỉ là map
     * sang model của sheet native; lấy config + luật lọc theo `applicableProducts` dùng chung với
     * iOS qua [configuredServicesFor].
     *
     * Trả thẳng list thay vì bắn effect: Fragment gọi xong là mở sheet ngay tại chỗ, thêm một vòng
     * effect ở giữa chỉ để quay về đúng nơi vừa gọi.
     */
    fun serviceOptions(): List<ServiceSelectorUiItem> =
        configuredServicesFor(state.value.detail?.applicableProducts.orEmpty())
            .map { it.toServiceSelectorUiItem() }
}
