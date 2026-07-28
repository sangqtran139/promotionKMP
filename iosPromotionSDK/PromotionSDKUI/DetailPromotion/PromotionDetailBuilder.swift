//
//  PromotionDetailBuilder.swift
//  PromotionSDK
//
//  Created by thachlh on 13/5/26.
//

import UIKit
@_implementationOnly import PRMKotlinBridge

final class PromotionDetailBuilder: PRMBaseBuilder<PromotionDetailViewController, PromotionDetailViewModel, PromotionDetailRouter, PromotionDetailBuilder.DataModel> {
    
    /**
     Màn Chi tiết mở từ đâu — quyết định **nhãn nút và hành vi khi bấm** (TLNV MOB_002 control #5):
     - `myPromotion`: "Sử dụng ngay" → chọn dịch vụ (1 dịch vụ thì đi thẳng).
     - `checkout`: "Áp dụng" → quay lại màn "Chọn ưu đãi" với voucher này đã được tick.

     Đối ứng `PromotionDetailEntry` bên Android.
     */
    enum Entry {
        case myPromotion
        case checkout
    }

    struct DataModel {
        /// Promotion cơ bản (từ list) để hiện card NGAY; màn tự fetch detail đầy đủ theo voucherId.
        /// token/service KHÔNG ở đây — ViewModel đọc từ PromotionRequestContextProvider
        /// của lõi (đối xứng Android).
        let promotion: PRMPromotionCardSeed
        let entry: Entry
        /// Chỉ dùng khi `entry == .checkout`: màn "Chọn ưu đãi" nhận lại voucherId để tick ô chọn.
        /// Router của màn Chi tiết tự pop; closure chỉ lo phần selection.
        let onApplyFromCheckout: ((String) -> Void)?

        init(
            promotion: PRMPromotionCardSeed,
            entry: Entry = .myPromotion,
            onApplyFromCheckout: ((String) -> Void)? = nil
        ) {
            self.promotion = promotion
            self.entry = entry
            self.onApplyFromCheckout = onApplyFromCheckout
        }
    }
    
    static func build(with data: DataModel, navigator: UINavigationController? = nil) -> PromotionDetailViewController {
        let builder = PromotionDetailBuilder()
        return builder.build(with: data, navigator: navigator)
    }
    
    override func createRouter() -> PromotionDetailRouter {
        return PromotionDetailRouter()
    }
    
    override func createViewModel(router: PromotionDetailRouter, data: DataModel) -> PromotionDetailViewModel {
        return PromotionDetailViewModel(router: router, data: data)
    }
    
    override func createViewController(viewModel: PromotionDetailViewModel) -> PromotionDetailViewController {
        return PromotionDetailViewController(viewModel: viewModel)
    }
}
