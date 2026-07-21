//
//  PRMBaseViewModel.swift
//  PromotionSDK
//

import Foundation
import Combine

protocol PRMViewModelType {
    associatedtype Input
    associatedtype Output

    func transform(input: Input) -> Output
}

class PRMBaseViewModel<R: PRMBaseRouterProtocol> {

    let router: R
    /// Combine subscriptions.
    var cancellables = Set<AnyCancellable>()

    init(router: R) {
        self.router = router
    }

    func routeToParent() {
        self.router.routeToParent()
    }
}
