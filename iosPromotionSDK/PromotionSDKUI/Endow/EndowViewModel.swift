//
//  EndowViewModel.swift
//  PromotionSDK
//
//  ViewModel cho widget `PRMEndowView` — bọc `EndowStore` (tầng UI-logic dùng chung ở promotionLogic).
//  ĐỐI ỨNG Android `EndowViewModel`: findEligible và validate&apply đi qua `EndowStore` dùng chung;
//  `PromotionSDKImpl` chỉ quan sát state + render.
//
//  Dùng `PRMStoreViewModel` (bản KHÔNG router — widget không điều hướng), nên phần giữ store /
//  observe / hop main / huỷ lúc deinit không còn viết lại ở đây.
//

import Foundation
@_implementationOnly import PRMKotlinBridge

final class EndowViewModel: PRMStoreViewModel<EndowStore> {

    /// **Tắt** cầu lỗi → effect tự động của base.
    ///
    /// Widget phải giữ `errorCode` lại cho tới khi vòng validate kết thúc ([handleSettle] mới quyết
    /// định giao lỗi cho nơi gọi rồi mới xoá). Để base tự `ConsumeError` mỗi lần state đổi là nuốt
    /// mất lỗi đúng lúc user đang chờ kết quả bấm "Áp dụng". Đối ứng `consumeErrorUnlessSettling`
    /// bên Android.
    override var autoConsumesError: Bool { false }

    /// One-shot cho validate&apply: chờ `isValidating` true→false rồi gọi completion đúng một lần.
    private var settleCompletion: ((EndowState) -> Void)?
    private var sawValidating = false

    /// Closure của nơi quan sát widget — base chỉ có **một** khe `onState`, mà VM này còn phải chạy
    /// [handleSettle] trên mỗi state, nên nó giữ khe đó và forward tiếp ra ngoài.
    private var externalOnState: ((EndowState) -> Void)?

    init(findEligibleUseCase: FindEligibleCampaignsUseCase = FindEligibleCampaignsUseCase(),
         validateUseCase: ValidateStackableDiscountsUseCase = ValidateStackableDiscountsUseCase(),
         createRedemptionUseCase: CreateRedemptionSessionUseCase = CreateRedemptionSessionUseCase()) {
        super.init(store: EndowStore(
            findEligibleCampaignsUseCase: findEligibleUseCase,
            validateStackableDiscountsUseCase: validateUseCase,
            createRedemptionSessionUseCase: createRedemptionUseCase
        ))
        onState = { [weak self] state in
            guard let self = self else { return }
            self.handleSettle(state)
            self.externalOnState?(state)
        }
    }

    // ─── Observe (đối ứng Android `uiState.collect`) ────────────────────────────
    /// Quan sát state (base đã hop main). Gọi một lần khi widget attach.
    func observe(_ onState: @escaping (EndowState) -> Void) {
        externalOnState = onState
        onState(currentState)   // phát ngay state hiện tại, khớp hành vi replay của StateFlow
    }

    // ─── Public API (forward xuống store) ───────────────────────────────────────
    func loadInitial() { dispatch(EndowIntentLoadInitial.shared) }

    /// Validate + áp offers đã chọn; [completion] gọi MỘT lần khi validate xong (thành công/thất bại).
    /// Widget-state cập nhật qua [observe]; [completion] để `PromotionSDKImpl` điều hướng (pop, callback host).
    func validateAndApply(_ offers: [EligibleOffer], completion: ((EndowState) -> Void)? = nil) {
        // Rỗng → store xoá áp NGAY, không có vòng validate nào để chờ; bắn completion luôn cho khỏi treo.
        if offers.isEmpty {
            dispatch(EndowIntentValidateAndApply(offers: offers))
            completion?(currentState)
            return
        }
        settleCompletion = completion
        sawValidating = false
        dispatch(EndowIntentValidateAndApply(offers: offers))
    }

    func setApplied(_ discounts: [EndowAppliedDiscount], unavailable: Bool) {
        dispatch(EndowIntentSetApplied(discounts: discounts, unavailable: unavailable))
    }

    /// Bấm "Thanh toán" — nghiệp vụ nằm trọn ở `EndowStore.confirmRedemption` (dùng chung Android).
    ///
    /// Trước đây iOS **không có** luồng này: `createRedemption` chỉ tồn tại ở `PromotionSDKApi`
    /// (headless, host tự gọi), còn Android thì giấu trong `PromotionIntegrateManager`. Nay một đường.
    func confirmRedemption(completion: @escaping (EndowConfirmResult) -> Void) {
        Task { @MainActor in
            // `suspend` bên Kotlin — Swift nhập vào thành `async throws`. Chỉ `CancellationException`
            // mới thoát ra được (store nuốt lỗi nghiệp vụ), nhưng vẫn bắt để không nổ ở tầng host.
            let result = (try? await store.confirmRedemption())
                ?? EndowConfirmResultFailure(errorCode: PromotionErrorCodes.shared.GENERAL)
            completion(result)
        }
    }

    func markUnavailable() { dispatch(EndowIntentMarkUnavailable.shared) }
    func clearApplied() { dispatch(EndowIntentClearApplied.shared) }
    func consumeError() { dispatch(EndowIntentConsumeError.shared) }

    private func handleSettle(_ state: EndowState) {
        if state.isValidating {
            sawValidating = true
            return
        }
        guard sawValidating, let completion = settleCompletion else { return }
        settleCompletion = nil
        sawValidating = false
        completion(state)
        // Xoá sau khi đã giao cho nơi gọi, để lỗi cũ không dính sang vòng validate sau.
        if state.errorCode != nil { consumeError() }
    }
}
