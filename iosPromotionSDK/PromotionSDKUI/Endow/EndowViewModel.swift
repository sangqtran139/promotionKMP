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

    /// **Tắt** cầu lỗi → effect tự động của base: widget tự hiện lỗi rồi tự `consumeError`.
    ///
    /// Trước đây tắt vì lý do khác — phải giữ `errorCode` cho tới khi vòng validate kết thúc, do
    /// `validateAndApply` là fire-and-forget nên nơi gọi phải đọc nhờ dòng state chung. Lý do đó
    /// hết: store `suspend` và **trả thẳng state cuối** của lượt gọi.
    override var autoConsumesError: Bool { false }

    init(findEligibleUseCase: FindEligibleCampaignsUseCase = FindEligibleCampaignsUseCase(),
         validateUseCase: ValidateStackableDiscountsUseCase = ValidateStackableDiscountsUseCase(),
         createRedemptionUseCase: CreateRedemptionSessionUseCase = CreateRedemptionSessionUseCase()) {
        super.init(store: EndowStore(
            findEligibleCampaignsUseCase: findEligibleUseCase,
            validateStackableDiscountsUseCase: validateUseCase,
            createRedemptionSessionUseCase: createRedemptionUseCase
        ))
    }

    // ─── Observe (đối ứng Android `uiState.collect`) ────────────────────────────
    /// Quan sát state (base đã hop main). Gọi một lần khi widget attach.
    ///
    /// Gán thẳng khe `onState` của base. Trước đây phải đi vòng qua `externalOnState` vì VM còn
    /// chiếm khe đó để chạy `handleSettle` trên MỌI state — `handleSettle` đã bỏ nên khe trả về
    /// đúng một người dùng.
    func observe(_ onState: @escaping (EndowState) -> Void) {
        self.onState = onState
        onState(currentState)   // phát ngay state hiện tại, khớp hành vi replay của StateFlow
    }

    // ─── Public API (forward xuống store) ───────────────────────────────────────
    func loadInitial() { dispatch(EndowIntentLoadInitial.shared) }

    /// Cờ hiển thị widget, đọc cache (fail-open). Rule + tên cờ nằm ở store, dùng chung Android.
    func availabilityFromCache() -> Bool { store.availabilityFromCache() }

    /// Làm mới cờ từ server rồi trả giá trị thật.
    ///
    /// `.boolValue`: hàm `suspend` trả `Boolean` bên Kotlin sang Swift thành **`KotlinBoolean`** (kiểu
    /// hộp), khác hẳn `availabilityFromCache()` không suspend — cái đó ra `Bool` thẳng.
    /// Hỏng mạng → `try?` cho nil → `true`, fail-open đúng như [PromotionFeatureGate.refresh].
    func refreshAvailability() async -> Bool {
        (try? await store.refreshAvailability())?.boolValue ?? true
    }

    /// Validate + áp offers đã chọn; [completion] gọi MỘT lần khi validate xong (thành công/thất bại).
    /// Widget-state cập nhật qua [observe]; [completion] để `PromotionSDKImpl` điều hướng (pop, callback host).
    func validateAndApply(_ offers: [EligibleOffer], completion: ((EndowState) -> Void)? = nil) {
        Task { @MainActor in
            // `suspend` bên Kotlin → `async throws` bên Swift. Trả state cuối của ĐÚNG lượt này nên
            // không còn `settleCompletion`/`sawValidating`/`handleSettle` — Android bỏ y hệt.
            let state = (try? await store.validateAndApply(offers: offers)) ?? currentState
            completion?(state)
            // Đã giao lỗi cho nơi gọi → giờ mới xoá, để lỗi cũ không dính sang vòng validate sau.
            if state.errorCode != nil { consumeError() }
        }
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

}
