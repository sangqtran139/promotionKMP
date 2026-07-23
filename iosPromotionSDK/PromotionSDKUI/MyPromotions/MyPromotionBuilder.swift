//
//  MyPromotionBuilder.swift
//  PRMSDK
//
//  Created by thachlh on 6/5/26.
//

import UIKit

final class MyPromotionBuilder: PRMBaseBuilder<MyPromotionViewController, MyPromotionViewModel, MyPromotionRouter, MyPromotionBuilder.DataModel> {
    
    /// Rỗng: màn này không có tham số đầu vào (context đọc từ lõi).
    /// Chỉ tồn tại để khớp generic `Dependency` của `PRMBaseBuilder` — **không** truyền vào ViewModel.
    struct DataModel {
        init() {}
    }
    
    static func build(with data: DataModel, navigator: UINavigationController? = nil) -> MyPromotionViewController {
        let builder = MyPromotionBuilder()
        return builder.build(with: data, navigator: navigator)
    }
    
    override func createRouter() -> MyPromotionRouter {
        return MyPromotionRouter()
    }
    
    override func createViewModel(router: MyPromotionRouter, data: DataModel) -> MyPromotionViewModel {
        return MyPromotionViewModel(router: router)
    }
    
    override func createViewController(viewModel: MyPromotionViewModel) -> MyPromotionViewController {
        return MyPromotionViewController(viewModel: viewModel)
    }
}
