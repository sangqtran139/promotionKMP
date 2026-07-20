//
//  PRMBaseViewModel.swift
//  PromotionSDK
//

import Foundation
@_implementationOnly import RxSwift
@_implementationOnly import RxCocoa

protocol PRMViewModelType {
    associatedtype Input
    associatedtype Output

    func transform(input: Input) -> Output
}

class PRMBaseViewModel<R: PRMBaseRouterProtocol> {

    let router: R
    let disposeBag = DisposeBag()

    init(router: R) {
        self.router = router
    }

    func routeToParent() {
        self.router.routeToParent()
    }
}
