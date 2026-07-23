//
//  ChoosePromotionViewModel.swift
//  PRMSDK
//
//  Lớp bọc mỏng quanh ChoosePromotionStore (tầng UI-logic dùng chung ở promotionLogic).
//  ĐỒNG NHẤT với `ChoosePromotionViewModel` bên Android — cùng `store` / `bindStore` /
//  `handleAction` / `render` / `handleError`, cùng thứ tự. Load/paging/search/selection và rule
//  "Xem thêm" nằm ở store; VM chỉ dựng sections để VC render.
//
//  KHÔNG dùng Combine: store đã phơi callback (`watchState`), nên VM cũng phơi callback
//  (`onState` / `onEffect`) — đối ứng 1-1 `uiState: StateFlow` / `uiEffect: Flow` bên Android.
//

import Foundation
import UIKit
@_implementationOnly import PRMFoundation   // UIImage.sdk(_:) cho ảnh checkbox của cell
@_implementationOnly import PRMKotlinBridge

final class ChoosePromotionViewModel: PRMBaseViewModel<ChoosePromotionRouter> {

    enum SectionType: String {
        case myPromotions = "myPromotions"
        case otherPromotions = "otherPromotions"
    }

    enum SeeMoreState { case none, expand, collapse }

    struct PromotionSection {
        let type: SectionType
        let title: String
        let items: [MyPromotionCellViewModel]
        let totalCount: Int
        let seeMoreState: SeeMoreState
    }

    /// Bề mặt view (đã format) — đối ứng `ChoosePromotionUiState` bên Android.
    struct UiState {
        var sections: [PromotionSection] = []
        var isLoading = false
    }

    /// Đối ứng `ChoosePromotionAction` bên Android — chỉ những gì màn thật sự phát.
    enum Action {
        case loadInitial
        case queryChanged(String)
        case search
        case toggleSelection(String)
        case seeMoreMy
        case loadMoreOtherVouchers
        case openDetail(String)
        case validateAndApply
    }

    /// Sự kiện một-lần — đối ứng `ChoosePromotionEffect` bên Android.
    enum Effect {
        case showError(String)
        case applySelectedOffers([EligibleOffer])
    }

    let data: ChoosePromotionBuilder.DataModel

    /// State hiện tại + kênh phát. Gán `onState` là **nhận ngay** state hiện tại — mô phỏng đúng
    /// hành vi replay của `StateFlow` bên Android.
    private(set) var uiState = UiState() {
        didSet { onState?(uiState) }
    }
    var onState: ((UiState) -> Void)? {
        didSet { onState?(uiState) }
    }
    /// Một-lần, KHÔNG replay (giống effect bên Android).
    var onEffect: ((Effect) -> Void)?

    // ─── Store ────────────────────────────────────────────────────────────────
    // Selection (`selectedIds`) + mở/thu gọn (`myExpanded`) nay do store quản — VC chỉ render.
    private let store: ChoosePromotionStore
    private var storeCancellable: PromotionCancellable?
    private var didStart = false

    init(router: ChoosePromotionRouter,
         data: ChoosePromotionBuilder.DataModel,
         findEligibleUseCase: FindEligibleCampaignsUseCase = FindEligibleCampaignsUseCase()) {
        self.data = data
        self.store = ChoosePromotionStore(findEligibleCampaignsUseCase: findEligibleUseCase)
        super.init(router: router)
        bindStore()   // đối ứng `init { bindStore() }` bên Android
    }

    deinit {
        storeCancellable?.cancel()
        store.clear()
    }

    // ─── Store observation (đối ứng Android.bindStore) ──────────────────────────
    private func bindStore() {
        storeCancellable = observeStore(watch: { [store] in store.watchState(onEach: $0) }) { [weak self] state in
            guard let self = self else { return }
            self.render(state)
            self.handleError(state)
        }
    }

    // ─── Intent forwarding (đối ứng Android.handleAction) ───────────────────────
    func handleAction(_ action: Action) {
        switch action {
        case .loadInitial:
            // Seed pre-select rồi preload/fetch — đối ứng `ChoosePromotionFragment.observeData`
            // (SetPreSelected → PreloadVouchers). `didStart` chặn chạy lại khi màn được bind lại.
            guard !didStart else { return }
            didStart = true
            store.dispatch(intent: ChoosePromotionIntentSetPreSelected(ids: data.preSelectedVoucherIds))
            store.dispatch(intent: ChoosePromotionIntentPreload(
                myOffers: data.preloadedMy,
                otherOffers: data.preloadedOther,
                myIsLastPage: data.myIsLastPage,
                otherIsLastPage: data.otherIsLastPage
            ))
        case .queryChanged(let keyword):
            // Xoá trắng → `ClearKeyword` (reload ngay, không chờ debounce) — khớp Fragment Android.
            if keyword.isEmpty {
                store.dispatch(intent: ChoosePromotionIntentClearKeyword.shared)
            } else {
                store.dispatch(intent: ChoosePromotionIntentQueryChanged(keyword: keyword))
            }
        case .search:
            store.dispatch(intent: ChoosePromotionIntentSearch.shared)
        case .toggleSelection(let id):
            // Rule single/multi do store quyết định — dùng chung Android.
            store.dispatch(intent: ChoosePromotionIntentToggleSelection(id: id))
        case .seeMoreMy:
            // Store chạy state-machine "mở hết → tải trang kế → thu gọn".
            store.dispatch(intent: ChoosePromotionIntentSeeMoreMy.shared)
        case .loadMoreOtherVouchers:
            // Store tự bỏ nếu hết trang/đang tải.
            store.dispatch(intent: ChoosePromotionIntentLoadMoreOtherVouchers.shared)
        case .openDetail(let id):
            guard let promotion = Self.allLoaded(store.currentState()).first(where: { $0.id == id })
            else { return }
            router.routeToDetail(promotion: promotion)
        case .validateAndApply:
            applySelected()
        }
    }

    // ─── State → View ─────────────────────────────────────────────────────────
    private func render(_ state: ChoosePromotionState) {
        uiState = state.toUiState()
    }

    // ─── Error ──────────────────────────────────────────────────────────────────
    private func handleError(_ state: ChoosePromotionState) {
        guard let code = state.errorCode else { return }
        onEffect?(.showError(code))   // view map code → chuỗi
        store.dispatch(intent: ChoosePromotionIntentConsumeError.shared)
    }

    // ─── Áp dụng: chỉ trả offers đang chọn cho widget (EndowStore validate) ──────
    // Selection do store giữ (`selectedIds`); resolve về EligibleOffer đang chọn.
    /// Đối ứng `ChoosePromotionViewModel.applySelected()` bên Android.
    private func applySelected() {
        let state = store.currentState()
        let selectedOffers = Self.allLoaded(state).filter { state.selectedIds.contains($0.id) }
        guard !selectedOffers.isEmpty else { return }
        onEffect?(.applySelectedOffers(selectedOffers))
    }

    fileprivate static func allLoaded(_ state: ChoosePromotionState) -> [EligibleOffer] {
        state.myOffers.map { $0.source } + state.otherOffers.map { $0.source }
    }
}

// ─── Map state dùng chung (store) → model UI iOS ──────────────────────────────

/// Chiếu state dùng chung ([ChoosePromotionState]) → **bề mặt view iOS** (`UiState`).
/// Đối ứng 1-1 `private fun ChoosePromotionState.toUiState()` bên Android (cũng là hàm mức file).
private extension ChoosePromotionState {

    func toUiState() -> ChoosePromotionViewModel.UiState {
        ChoosePromotionViewModel.UiState(
            sections: buildSections(),
            isLoading: isLoading
        )
    }

    /// iOS-only UI: dựng sections cho `UITableView` (Android dựng list item ở adapter).
    /// Selection + mở/thu gọn đều đã nằm ở store — hàm này chỉ đọc.
    func buildSections() -> [ChoosePromotionViewModel.PromotionSection] {
        let trimmed = keyword.trimmingCharacters(in: .whitespacesAndNewlines)
        let isSearching = !trimmed.isEmpty
        let selected = selectedIds
        let cell: (ChooseOffer) -> MyPromotionCellViewModel = { offer in
            MyPromotionCellViewModel(
                offer: offer.source,
                isEnabled: offer.isUsable,
                buttonTitle: PromotionUIStrings.detail,
                showsCheckbox: true,
                isChecked: selected.contains(offer.source.id),
                checkedImage: UIImage.sdk("prm_ic_circle_check"),
                uncheckedImage: UIImage.sdk("prm_ic_circle_uncheck"),
                highlightKeyword: isSearching ? trimmed : nil,
                expiringInDays: offer.expiringInDays?.intValue
            )
        }

        var sections: [ChoosePromotionViewModel.PromotionSection] = []
        if !myOffers.isEmpty {
            // Slice + trạng thái nút "Xem thêm/Thu gọn" đều lấy từ **rule dùng chung** ở promotionLogic
            // (`visibleMyOffers()` / `mySeeMoreState()`) — y như Android, không bên nào tự suy lại.
            sections.append(.init(type: .myPromotions, title: PromotionUIStrings.myPromotions,
                                  items: visibleMyOffers().map(cell), totalCount: myOffers.count,
                                  seeMoreState: mySeeMoreState().toSeeMoreState()))
        }
        if !otherOffers.isEmpty {
            sections.append(.init(type: .otherPromotions, title: PromotionUIStrings.otherPromotions,
                                  items: otherOffers.map(cell), totalCount: otherOffers.count,
                                  seeMoreState: .none))
        }
        return sections
    }
}

/// `ChooseSeeMoreState` (rule dùng chung ở promotionLogic) → enum hiển thị của VC.
private extension ChooseSeeMoreState {
    func toSeeMoreState() -> ChoosePromotionViewModel.SeeMoreState {
        switch self {
        case .hidden: return .none
        case .collapse: return .collapse
        case .expand: return .expand
        default: return .expand
        }
    }
}
