//
//  PRMBaseBuilder.swift
//  PromotionSDK
//

import UIKit

/// `@MainActor`: builder dựng ViewController và ViewModel — cả hai đều MainActor-isolated. Đây là
/// hệ quả kéo theo của `@MainActor` trên `PromotionSDK`/`PRMStoreViewModel`, không phải ràng buộc
/// mới: builder vốn chỉ được gọi từ luồng điều hướng, tức main.
@MainActor
class PRMBaseBuilder<VC: UIViewController, VM, R, Dependency> {

    init() {}

    func createRouter() -> R {
        fatalError("Subclass must override createRouter()")
    }

    func createViewModel(router: R, data: Dependency) -> VM {
        fatalError("Subclass must override createViewModel(router:data:)")
    }

    func createViewController(viewModel: VM) -> VC {
        fatalError("Subclass must override createViewController(viewModel:)")
    }

    func configureRouter(_ router: R, viewController: VC, navigator: UINavigationController?) {
        if let baseRouter = router as? PRMBaseRouter<VC> {
            baseRouter.setViewController(viewController)
            baseRouter.setNavigator(navigator)
        }
    }

    func build(with data: Dependency, navigator: UINavigationController? = nil) -> VC {
        let router = createRouter()
        let viewModel = createViewModel(router: router, data: data)
        let viewController = createViewController(viewModel: viewModel)
        configureRouter(router, viewController: viewController, navigator: navigator)
        return viewController
    }
}
