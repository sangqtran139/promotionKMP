//
//  SearchMyPromotionBuilder.swift
//  PRMSDK
//
//  Created by thachlh on 11/5/26.
//

import UIKit
@_implementationOnly import PRMKotlinBridge

final class SearchMyPromotionBuilder: PRMBaseBuilder<SearchMyPromotionViewController, SearchMyPromotionViewModel, SearchMyPromotionRouter, SearchMyPromotionBuilder.DataModel> {
    
    /// Rỗng: màn Tìm kiếm không có tham số đầu vào (context đọc từ lõi, keyword do user gõ).
    /// Chỉ tồn tại để khớp generic `Dependency` của `PRMBaseBuilder` — **không** truyền vào ViewModel.
    struct DataModel {
        init() {}
    }
    
    static func build(with data: DataModel, navigator: UINavigationController? = nil) -> SearchMyPromotionViewController {
        let builder = SearchMyPromotionBuilder()
        return builder.build(with: data, navigator: navigator)
    }
    
    override func createRouter() -> SearchMyPromotionRouter {
        return SearchMyPromotionRouter()
    }
    
    override func createViewModel(router: SearchMyPromotionRouter, data: DataModel) -> SearchMyPromotionViewModel {
        return SearchMyPromotionViewModel(router: router)
    }
    
    override func createViewController(viewModel: SearchMyPromotionViewModel) -> SearchMyPromotionViewController {
        return SearchMyPromotionViewController(viewModel: viewModel)
    }
}
