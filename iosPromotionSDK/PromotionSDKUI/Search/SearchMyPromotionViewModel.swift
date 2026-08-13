//
//  SearchMyPromotionViewModel.swift
//  PromotionSDK
//
//  ĐỒNG NHẤT với `SearchMyPromotionViewModel` bên Android: không còn `UiState`/`Action`/`Effect`
//  riêng, không còn `bindStore`/`render`/`handleError` — tất cả nằm ở `PRMStoreViewModel`.
//  Còn lại đúng phần thuần iOS: điều hướng (router) và bottom sheet "Chọn dịch vụ".
//

import Foundation
@_implementationOnly import PRMKotlinBridge

final class SearchMyPromotionViewModel:
    PRMScreenViewModel<SearchMyPromotionRouter, SearchMyPromotionStore> {

    /// Không nhận `DataModel`: màn Tìm kiếm không có tham số đầu vào nào — token đọc từ
    /// `PromotionRequestContextProvider` của lõi, keyword do user gõ.
    init(router: SearchMyPromotionRouter,
         searchVouchersUseCase: SearchCustomerVouchersUseCase = SearchCustomerVouchersUseCase()) {
        super.init(router: router,
                   store: SearchMyPromotionStore(searchCustomerVouchersUseCase: searchVouchersUseCase))
    }

    /// Mở Detail bằng voucher cơ bản; màn Detail tự fetch chi tiết (điều hướng — không ở store).
    func openDetail(voucherId: String) {
        guard let voucher = voucher(voucherId) else { return }
        router.routeToDetail(promotion: voucher.source)
    }

    /// Dịch vụ cho bottom sheet — render native, luật lọc dùng chung ở promotionLogic.
    /// Đối ứng `SearchMyPromotionViewModel.serviceOptions(voucher)` bên Android.
    func serviceOptions(voucherId: String) -> [ServiceSelectorItem] {
        guard let voucher = voucher(voucherId) else { return [] }
        return ServiceSelectorBuilder.items(forApplicableProducts: voucher.source.applicableProducts)
    }

    /// Tra voucher theo id — luật ở store (`SearchMyPromotionState.voucher(id:)`), đối xứng
    /// `MyPromotionViewModel.voucher(_:)`.
    private func voucher(_ id: String) -> MyPromotionVoucher? {
        store.currentState().voucher(id: id)
    }
}
