//
//  PRMBaseBuilder.swift
//  PRMSDK
//

import UIKit

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
