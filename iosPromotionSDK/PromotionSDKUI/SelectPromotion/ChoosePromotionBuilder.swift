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
        /// Voucher đang áp dụng (nếu có) → pre-select khi mở lại màn chọn (khớp Android `preSelectedVoucherIds`).
        /// Danh sách để sẵn sàng multi-select; hiện tại thường 0/1 phần tử.
        let preSelectedVoucherIds: [String]

        // Bốn field preload (`preloadedMy`/`preloadedOther` + hai cờ phân trang) đã bỏ: màn này nay
        // **luôn** gọi lại `findEligible` khi mở (`ChoosePromotionIntent.SeedOnce`), nên dùng lại
        // danh sách widget nạp lúc trước chỉ tổ hiện dữ liệu cũ. Cờ phân trang cũng lấy từ chính
        // response đó. Đối ứng `ChoosePromotionFragment` bên Android.

        init(orderItems: [EligibleOrderItem] = [],
             preSelectedVoucherIds: [String] = []) {
            self.orderItems = orderItems
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
