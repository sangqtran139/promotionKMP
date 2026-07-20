//
//  BaseRouter.swift
//  PromotionSDK
//

import UIKit

protocol BaseRouterProtocol {
    func routeToParent()
}

class BaseRouter<VC: UIViewController>: BaseRouterProtocol {

    weak var viewController: VC?
    weak var navigator: UINavigationController?

    init() {}

    func setViewController(_ viewController: VC) {
        self.viewController = viewController
    }

    func setNavigator(_ navigator: UINavigationController?) {
        self.navigator = navigator
    }

    func routeToParent() {
        self.navigator?.popViewController(animated: true)
    }
}
