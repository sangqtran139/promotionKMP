//
//  PRMBaseRouter.swift
//  PromotionSDK
//

import UIKit

/// `@MainActor`: router chỉ làm điều hướng (`push`/`present`/`pop`) — thuần UIKit.
@MainActor
protocol PRMBaseRouterProtocol {
    func routeToParent()
}

@MainActor
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

    /// Back của các màn SDK.
    ///
    /// Chỉ `navigator?.popViewController` là **không đủ**: khi host mở màn SDK từ một VC **không có**
    /// `navigationController`, SDK tự bọc `UINavigationController` rồi `present` — nhưng `navigator`
    /// truyền vào lúc build là `nil`, nên back sẽ **không làm gì** và modal không bao giờ đóng.
    ///
    /// Vì vậy: lấy nav controller **thật lúc chạy** trước; còn stack thì pop, là màn gốc của nav do SDK
    /// tự bọc thì `dismiss` (pop không có tác dụng với root). Đối ứng `PRMBaseFragment.onBackFragment()`
    /// bên Android — cũng không được phép đóng màn/Activity của host một cách thô bạo.
    func routeToParent() {
        guard let viewController = viewController else { return }
        if let nav = viewController.navigationController ?? navigator, nav.viewControllers.count > 1 {
            nav.popViewController(animated: true)
        } else {
            viewController.dismiss(animated: true)
        }
    }
}
