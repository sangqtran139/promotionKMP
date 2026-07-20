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
        let customerId: String
        let token: String?
        /// Thông tin đơn cho Find Eligible Campaigns (màn tự re-fetch/phân trang bằng eligible).
        let orderId: String?
        let orderValue: String?
        let orderItems: [EligibleOrderItem]
        /// Data preload từ widget (cachedListModel) để tránh double API call — giống Android PreloadVouchers.
        /// Rỗng → màn tự fetch trang 0 (hiện shimmer).
        let preloadedMy: [EligibleOffer]
        let preloadedOther: [EligibleOffer]
        let myIsLastPage: Bool
        let otherIsLastPage: Bool
        /// Voucher đang áp dụng (nếu có) → pre-select khi mở lại màn chọn (khớp Android `preSelectedVoucherIds`).
        let preSelectedVoucherId: String?

        init(customerId: String,
             token: String?,
             orderId: String? = nil,
             orderValue: String? = nil,
             orderItems: [EligibleOrderItem] = [],
             preloadedMy: [EligibleOffer] = [],
             preloadedOther: [EligibleOffer] = [],
             myIsLastPage: Bool = true,
             otherIsLastPage: Bool = true,
             preSelectedVoucherId: String? = nil) {
            self.customerId = customerId
            self.token = token
            self.orderId = orderId
            self.orderValue = orderValue
            self.orderItems = orderItems
            self.preloadedMy = preloadedMy
            self.preloadedOther = preloadedOther
            self.myIsLastPage = myIsLastPage
            self.otherIsLastPage = otherIsLastPage
            self.preSelectedVoucherId = preSelectedVoucherId
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
