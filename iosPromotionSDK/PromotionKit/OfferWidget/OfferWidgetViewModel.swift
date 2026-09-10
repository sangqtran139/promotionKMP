//
//  OfferWidgetViewModel.swift
//  PromotionSDK
//
//  ViewModel cho widget `PRMOfferWidget` — bọc `OfferWidgetStore` (tầng UI-logic dùng chung ở promotionLogic).
//  ĐỐI ỨNG Android `OfferWidgetViewModel`: findEligible và validate&apply đi qua `OfferWidgetStore` dùng chung;
//  `PromotionSDKImpl` chỉ quan sát state + render.
//
//  Dùng `PRMStoreViewModel` (bản KHÔNG router — widget không điều hướng), nên phần giữ store /
//  observe / hop main / huỷ lúc deinit không còn viết lại ở đây.
//

import Foundation
@_implementationOnly import PRMKotlinBridge

final class OfferWidgetViewModel: PRMStoreViewModel<OfferWidgetStore> {

    /// **Tắt** cầu lỗi → effect tự động của base: widget tự hiện lỗi rồi tự `consumeError`.
    ///
    /// Trước đây tắt vì lý do khác — phải giữ `errorCode` cho tới khi vòng validate kết thúc, do
    /// `validateAndApply` là fire-and-forget nên nơi gọi phải đọc nhờ dòng state chung. Lý do đó
    /// hết: store `suspend` và **trả thẳng kết cục** (`OfferWidgetApplyOutcome`) của lượt gọi.
    override var autoConsumesError: Bool { false }

    init(findEligibleUseCase: FindEligibleCampaignsUseCase = FindEligibleCampaignsUseCase(),
         validateUseCase: ValidateStackableDiscountsUseCase = ValidateStackableDiscountsUseCase(),
         createRedemptionUseCase: CreateRedemptionSessionUseCase = CreateRedemptionSessionUseCase()) {
        super.init(store: OfferWidgetStore(
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
    func observe(_ onState: @escaping (OfferWidgetState) -> Void) {
        self.onState = onState
        onState(currentState)   // phát ngay state hiện tại, khớp hành vi replay của StateFlow
    }

    // ─── Public API (forward xuống store) ───────────────────────────────────────
    func loadInitial() { dispatch(OfferWidgetIntentLoadInitial.shared) }

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

    /// Validate + áp offers đã chọn; [completion] gọi MỘT lần khi lượt validate ngã ngũ, kèm
    /// [OfferWidgetApplyOutcome] — áp được / bị server từ chối / không hỏi được server.
    ///
    /// Widget-state cập nhật qua [observe]; [completion] để `PromotionSDKImpl` điều hướng (pop) và
    /// màn "Chọn ưu đãi" quyết định ở lại hay đóng. Đối ứng `OfferWidgetViewModel.validateAndApply` Android.
    func validateAndApply(_ offers: [EligibleOffer], completion: ((OfferWidgetApplyOutcome) -> Void)? = nil) {
        Task { @MainActor in
            // `suspend` bên Kotlin → `async throws` bên Swift. Trả kết cục của ĐÚNG lượt này nên
            // không còn `settleCompletion`/`sawValidating`/`handleSettle` — Android bỏ y hệt.
            // Chỉ `CancellationException` mới thoát ra được (store nuốt lỗi nghiệp vụ), nhưng vẫn
            // bắt để không nổ ở tầng host — cùng cách với `confirmRedemption`.
            let outcome = (try? await store.validateAndApply(offers: offers))
                ?? OfferWidgetApplyOutcomeFailed(errorCode: PromotionErrorCodes.shared.GENERAL)
            completion?(outcome)
            // Đã giao kết quả cho nơi gọi → giờ mới xoá lỗi, để lỗi cũ không dính sang vòng sau.
            if currentState.errorCode != nil { consumeError() }
        }
    }

    func setApplied(_ discounts: [OfferWidgetAppliedDiscount], unavailable: Bool) {
        dispatch(OfferWidgetIntentSetApplied(discounts: discounts, unavailable: unavailable))
    }

    /// Bấm "Thanh toán" — nghiệp vụ nằm trọn ở `OfferWidgetStore.confirmRedemption` (dùng chung Android).
    ///
    /// Trước đây iOS **không có** luồng này: `createRedemption` chỉ tồn tại ở `PromotionSDKApi`
    /// (headless, host tự gọi), còn Android thì giấu trong `PromotionIntegrateManager`. Nay một đường.
    func confirmRedemption(completion: @escaping (OfferWidgetConfirmResult) -> Void) {
        Task { @MainActor in
            // `suspend` bên Kotlin — Swift nhập vào thành `async throws`. Chỉ `CancellationException`
            // mới thoát ra được (store nuốt lỗi nghiệp vụ), nhưng vẫn bắt để không nổ ở tầng host.
            let result = (try? await store.confirmRedemption())
                ?? OfferWidgetConfirmResultFailure(errorCode: PromotionErrorCodes.shared.GENERAL)
            completion(result)
        }
    }

    func markUnavailable() { dispatch(OfferWidgetIntentMarkUnavailable.shared) }
    func clearApplied() { dispatch(OfferWidgetIntentClearApplied.shared) }
    func consumeError() { dispatch(OfferWidgetIntentConsumeError.shared) }

}
