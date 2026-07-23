//
//  StoreObserving.swift
//  PromotionSDK
//
//  Cầu observe store dùng chung cho các ViewModel bọc shared store (promotionLogic).
//  Gom phần lặp lại: `store.watchState { ... }` + hop về main thread + trả PromotionCancellable.
//  Trước đây mỗi VM (My/Search/Choose/Detail) tự viết `DispatchQueue.main.async { [weak self] ... }`.
//

import Foundation
@_implementationOnly import PRMKotlinBridge

/// Bọc `store.watchState` (callback + [PromotionCancellable]) và luôn gọi [onMain] trên main thread.
///
/// - Parameters:
///   - watch: hàm `watchState` của store (mỗi store có kiểu State riêng, nên nhận qua closure).
///   - onMain: xử lý mỗi state, đảm bảo chạy trên main (VM chỉ việc `render`/`handleError`).
/// - Returns: cancellable để VM huỷ khi deinit.
func observeStore<State>(
    watch: (@escaping (State) -> Void) -> PromotionCancellable,
    onMain: @escaping (State) -> Void
) -> PromotionCancellable {
    watch { state in
        DispatchQueue.main.async { onMain(state) }
    }
}
