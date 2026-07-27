//
//  ChoosePromotionBuilder.swift
//  PromotionSDK
//
//  Created by thachlh on 14/5/26.
//

import UIKit
@_implementationOnly import PRMKotlinBridge


final class ChoosePromotionBuilder: PRMBaseBuilder<ChoosePromotionViewController, ChoosePromotionViewModel, ChoosePromotionRouter, ChoosePromotionBuilder.DataModel> {
    
    struct DataModel {
        // token/orderId/orderValue KHÔNG còn ở đây — ViewModel đọc thẳng từ
        // PromotionRequestContextProvider của lõi (đối xứng Android). Chỉ giữ data riêng của màn.
        /// Dòng đơn hàng cho Find Eligible Campaigns — không có trong provider nên vẫn truyền qua đây.
        let orderItems: [EligibleOrderItem]
        /// Data preload từ widget (cachedListModel) để tránh double API call — giống Android PreloadVouchers.
        /// Rỗng → màn tự fetch trang 0 (hiện shimmer).
        let preloadedMy: [EligibleOffer]
        let preloadedOther: [EligibleOffer]
        let myIsLastPage: Bool
        let otherIsLastPage: Bool
        /// Voucher đang áp dụng (nếu có) → pre-select khi mở lại màn chọn (khớp Android `preSelectedVoucherIds`).
        /// Danh sách để sẵn sàng multi-select; hiện tại thường 0/1 phần tử.
        let preSelectedVoucherIds: [String]

        init(orderItems: [EligibleOrderItem] = [],
             preloadedMy: [EligibleOffer] = [],
             preloadedOther: [EligibleOffer] = [],
             myIsLastPage: Bool = true,
             otherIsLastPage: Bool = true,
             preSelectedVoucherIds: [String] = []) {
            self.orderItems = orderItems
            self.preloadedMy = preloadedMy
            self.preloadedOther = preloadedOther
            self.myIsLastPage = myIsLastPage
            self.otherIsLastPage = otherIsLastPage
            self.preSelectedVoucherIds = preSelectedVoucherIds
        }
    }

    
    static func build(with data: DataModel, navigator: UINavigationController? = nil) -> ChoosePromotionViewController {
        let builder = ChoosePromotionBuilder()
        return builder.build(with: data, navigator: navigator)
    }
    
    override func createRouter() -> ChoosePromotionRouter {
        return ChoosePromotionRouter()
    }
    
    override func createViewModel(router: ChoosePromotionRouter, data: DataModel) -> ChoosePromotionViewModel {
        return ChoosePromotionViewModel(router: router, data: data)
    }
    
    override func createViewController(viewModel: ChoosePromotionViewModel) -> ChoosePromotionViewController {
        return ChoosePromotionViewController(viewModel: viewModel)
    }
}
