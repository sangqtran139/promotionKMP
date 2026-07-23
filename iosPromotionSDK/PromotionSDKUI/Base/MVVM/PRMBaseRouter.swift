//
//  PRMBaseRouter.swift
//  PRMSDK
//

import UIKit

protocol PRMBaseRouterProtocol {
    func routeToParent()
}

class PRMBaseRouter<VC: UIViewController>: PRMBaseRouterProtocol {

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
