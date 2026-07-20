//
//  SearchMyPromotionBuilder.swift
//  PromotionSDK
//
//  Created by thachlh on 11/5/26.
//

import UIKit
@_implementationOnly import PRMKotlinBridge

final class SearchMyPromotionBuilder: PRMBaseBuilder<SearchMyPromotionViewController, SearchMyPromotionViewModel, SearchMyPromotionRouter, SearchMyPromotionBuilder.DataModel> {
    
    struct DataModel {
        let customerId: String
        let token: String?

        init(customerId: String, token: String? = nil) {
            self.customerId = customerId
            self.token = token
        }
    }
    
    static func build(with data: DataModel, navigator: UINavigationController? = nil) -> SearchMyPromotionViewController {
        let builder = SearchMyPromotionBuilder()
        return builder.build(with: data, navigator: navigator)
    }
    
    override func createRouter() -> SearchMyPromotionRouter {
        return SearchMyPromotionRouter()
    }
    
    override func createViewModel(router: SearchMyPromotionRouter, data: DataModel) -> SearchMyPromotionViewModel {
        return SearchMyPromotionViewModel(router: router, data: data)
    }
    
    override func createViewController(viewModel: SearchMyPromotionViewModel) -> SearchMyPromotionViewController {
        return SearchMyPromotionViewController(viewModel: viewModel)
    }
}
