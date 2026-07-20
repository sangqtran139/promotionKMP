//
//  BaseViewModel.swift
//  PromotionSDK
//

import Foundation
@_implementationOnly import RxSwift
@_implementationOnly import RxCocoa

protocol ViewModelType {
    associatedtype Input
    associatedtype Output

    func transform(input: Input) -> Output
}

class BaseViewModel<R: BaseRouterProtocol> {

    let router: R
    let disposeBag = DisposeBag()

    init(router: R) {
        self.router = router
    }

    func routeToParent() {
        self.router.routeToParent()
    }
}
