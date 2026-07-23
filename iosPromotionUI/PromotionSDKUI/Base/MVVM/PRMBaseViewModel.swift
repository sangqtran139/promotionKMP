//
//  PRMBaseViewModel.swift
//  PromotionSDK
//

import Foundation

/// Khuôn chung của mọi ViewModel trong SDK — **không dùng Combine**.
///
/// Mỗi VM bọc một store dùng chung ở `promotionLogic` và phơi ra đúng 3 thứ, đối ứng 1-1 với Android:
///  - `onState: ((UiState) -> Void)?`  ↔ `uiState: StateFlow<UiState>` (gán là nhận ngay state hiện tại)
///  - `onEffect: ((Effect) -> Void)?`  ↔ `uiEffect: Flow<Effect>` (một-lần, không replay)
///  - `handleAction(_:)`               ↔ `handleAction(action)`
///
/// Trước đây có `PRMViewModelType` (`transform(input:) -> Output` với publisher) — đã bỏ: khúc
/// Combine đó chỉ làm đường ống giữa callback của store và UIKit, không có combineLatest/debounce
/// nào (debounce nằm ở store, dùng chung 2 nền tảng).
class PRMBaseViewModel<R: PRMBaseRouterProtocol> {

    let router: R

    init(router: R) {
        self.router = router
    }

    func routeToParent() {
        self.router.routeToParent()
    }
}
