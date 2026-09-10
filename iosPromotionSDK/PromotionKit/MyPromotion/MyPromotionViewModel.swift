//
//  MyPromotionViewModel.swift
//  PromotionSDK
//
//  ĐỒNG NHẤT với `MyPromotionViewModel` bên Android — xem `PRMStoreViewModel` cho phần dùng chung.
//  Ở đây chỉ còn phần thuần iOS: điều hướng (router) và bottom sheet "Chọn dịch vụ".
//

import Foundation
@_implementationOnly import PRMKotlinBridge

final class MyPromotionViewModel: PRMScreenViewModel<MyPromotionRouter, MyPromotionStore> {

    /// Không nhận `DataModel`: màn này không có tham số đầu vào — token đọc từ
    /// `PromotionRequestContextProvider` của lõi.
    init(router: MyPromotionRouter,
         searchVouchersUseCase: SearchCustomerVouchersUseCase = SearchCustomerVouchersUseCase()) {
        super.init(router: router,
                   store: MyPromotionStore(searchCustomerVouchersUseCase: searchVouchersUseCase))
    }

    /// Mở Detail bằng voucher cơ bản; màn Detail tự fetch chi tiết (điều hướng — không ở store).
    func openDetail(voucherId: String) {
        guard let voucher = voucher(voucherId) else { return }
        router.routeToDetail(promotion: voucher.source)
    }

    /// iOS-only: Android mở màn Tìm kiếm bằng Fragment ở tầng view.
    func routeToSearch() {
        router.routeToSearch()
    }

    /// Đối ứng `MyPromotionViewModel.serviceOptions(voucher)` bên Android.
    func serviceOptions(voucherId: String) -> [ServiceSelectorItem] {
        guard let voucher = voucher(voucherId) else { return [] }
        return ServiceSelectorBuilder.items(forApplicableProducts: voucher.source.applicableProducts)
    }

    /// Tra voucher theo id — luật ở store (`MyPromotionState.voucher(id:)`), dùng chung với
    /// `SearchMyPromotionViewModel` (trước đây mỗi VM chép một bản y hệt).
    private func voucher(_ id: String) -> MyPromotionVoucher? {
        store.currentState().voucher(id: id)
    }
}
