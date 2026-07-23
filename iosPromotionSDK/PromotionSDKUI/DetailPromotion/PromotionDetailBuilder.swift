//
//  PromotionDetailBuilder.swift
//  PromotionSDK
//
//  Created by thachlh on 13/5/26.
//

import UIKit
@_implementationOnly import PRMKotlinBridge

final class PromotionDetailBuilder: PRMBaseBuilder<PromotionDetailViewController, PromotionDetailViewModel, PromotionDetailRouter, PromotionDetailBuilder.DataModel> {
    
    struct DataModel {
        /// Promotion cơ bản (từ list) để hiện card NGAY; màn tự fetch detail đầy đủ theo voucherId.
        /// customerId/token/service KHÔNG ở đây — ViewModel đọc từ PromotionRequestContextProvider
        /// của lõi (đối xứng Android).
        let promotion: PRMPromotionCardSeed

        init(promotion: PRMPromotionCardSeed) {
            self.promotion = promotion
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
