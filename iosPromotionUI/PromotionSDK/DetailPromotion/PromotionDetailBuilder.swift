//
//  PromotionDetailBuilder.swift
//  PromotionSDK
//
//  Created by thachlh on 13/5/26.
//

import UIKit
@_implementationOnly import PromotionKit

final class PromotionDetailBuilder: BaseBuilder<PromotionDetailViewController, PromotionDetailViewModel, PromotionDetailRouter, PromotionDetailBuilder.DataModel> {
    
    struct DataModel {
        /// Promotion cơ bản (từ list) để hiện card NGAY; màn tự fetch detail đầy đủ theo voucherId.
        let promotion: PromotionCardSeed
        let customerId: String
        let token: String?
        let service: String?

        init(promotion: PromotionCardSeed, customerId: String, token: String? = nil, service: String? = nil) {
            self.promotion = promotion
            self.customerId = customerId
            self.token = token
            self.service = service
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
