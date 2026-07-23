//
//  EndowViewModel.swift
//  PRMSDK
//
//  ViewModel cho widget `PRMEndowView` — bọc `EndowStore` (tầng UI-logic dùng chung ở promotionLogic).
//  ĐỐI ỨNG Android `PRMEndowViewModel`: trước đây iOS KHÔNG có ViewModel cho custom view — toàn bộ
//  nghiệp vụ (findEligible + validate + set widget-state) dồn trong `PromotionSDKImpl`. Nay findEligible
//  và validate&apply đi qua `EndowStore` dùng chung; `PromotionSDKImpl` chỉ quan sát state + render.
//

import Foundation
@_implementationOnly import PRMKotlinBridge

final class EndowViewModel {

    private let store: EndowStore
    private var stateCancellable: PromotionCancellable?

    /// One-shot cho validate&apply: chờ `isValidating` true→false rồi gọi completion đúng một lần.
    private var settleCompletion: ((EndowState) -> Void)?
    private var sawValidating = false

    init(findEligibleUseCase: FindEligibleCampaignsUseCase = FindEligibleCampaignsUseCase(),
         validateUseCase: ValidateStackableDiscountsUseCase = ValidateStackableDiscountsUseCase()) {
        self.store = EndowStore(
            findEligibleCampaignsUseCase: findEligibleUseCase,
            validateStackableDiscountsUseCase: validateUseCase
        )
    }

    deinit {
        stateCancellable?.cancel()
        store.clear()
    }

    /// State hiện tại (đồng bộ) — dùng dựng preload cho màn Chọn.
    var state: EndowState { store.currentState() }

    // ─── Observe (đối ứng Android `uiState.collect`) ────────────────────────────
    /// Quan sát state (đã hop main qua `observeStore`). Gọi một lần khi widget attach.
    func observe(_ onState: @escaping (EndowState) -> Void) {
        stateCancellable = observeStore(watch: { self.store.watchState(onEach: $0) }) { [weak self] state in
            self?.handleSettle(state)
            onState(state)
        }
    }

    // ─── Public API (forward xuống store) ───────────────────────────────────────
    func loadInitial() { store.dispatch(intent: EndowIntentLoadInitial.shared) }

    /// Validate + áp offers đã chọn; [completion] gọi MỘT lần khi validate xong (thành công/thất bại).
    /// Widget-state cập nhật qua [observe]; [completion] để `PromotionSDKImpl` điều hướng (pop, callback host).
    func validateAndApply(_ offers: [EligibleOffer], completion: ((EndowState) -> Void)? = nil) {
        // Rỗng → store xoá áp NGAY, không có vòng validate nào để chờ; bắn completion luôn cho khỏi treo.
        if offers.isEmpty {
            store.dispatch(intent: EndowIntentValidateAndApply(offers: offers))
            completion?(store.currentState())
            return
        }
        settleCompletion = completion
        sawValidating = false
        store.dispatch(intent: EndowIntentValidateAndApply(offers: offers))
    }

    func setApplied(_ discounts: [EndowAppliedDiscount], unavailable: Bool) {
        store.dispatch(intent: EndowIntentSetApplied(discounts: discounts, unavailable: unavailable))
    }

    func markUnavailable() { store.dispatch(intent: EndowIntentMarkUnavailable.shared) }
    func clearApplied() { store.dispatch(intent: EndowIntentClearApplied.shared) }
    func consumeError() { store.dispatch(intent: EndowIntentConsumeError.shared) }

    private func handleSettle(_ state: EndowState) {
        if state.isValidating {
            sawValidating = true
            return
        }
        guard sawValidating, let completion = settleCompletion else { return }
        settleCompletion = nil
        sawValidating = false
        completion(state)
    }
}
