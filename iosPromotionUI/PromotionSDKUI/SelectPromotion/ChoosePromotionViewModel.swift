//
//  ChoosePromotionViewModel.swift
//  PromotionSDK
//
//  Lớp bọc mỏng quanh ChoosePromotionStore (tầng UI-logic dùng chung ở promotionLogic).
//  ĐỒNG NHẤT với `ChoosePromotionViewModel` bên Android — cùng `store` / `bindStore` / `render` /
//  `handleError` + forward intent cùng thứ tự (xem `MyPromotionViewModel`). Load/paging/search/selection
//  và rule "Xem thêm" nằm ở store; VM chỉ dựng sections để VC render.
//

import Foundation
import UIKit
import Combine
@_implementationOnly import PRMFoundation
@_implementationOnly import PRMKotlinBridge

final class ChoosePromotionViewModel: PRMBaseViewModel<ChoosePromotionRouter>, PRMViewModelType {

    enum SectionType: String {
        case myPromotions = "myPromotions"
        case otherPromotions = "otherPromotions"
    }

    struct Input {
        let searchText: AnyPublisher<String, Never>
        let searchAction: AnyPublisher<Void, Never>
        let toggleSelectionRelay: PassthroughSubject<String, Never>
        let seeMoreMyRelay: PassthroughSubject<Void, Never>
        let loadMoreOtherRelay: PassthroughSubject<Void, Never>
    }

    enum SeeMoreState { case none, expand, collapse }

    struct PromotionSection {
        let type: SectionType
        let title: String
        let items: [MyPromotionCellViewModel]
        let totalCount: Int
        let seeMoreState: SeeMoreState
    }

    struct Output {
        let sections: AnyPublisher<[PromotionSection], Never>
        let hasSelection: AnyPublisher<Bool, Never>
        let selectedPromotions: AnyPublisher<[EligibleOffer], Never>
        let isLoading: AnyPublisher<Bool, Never>
        /// Phát **mã lỗi** (raw) — VC map code → chuỗi (đồng nhất Android; trước đây Choose iOS nuốt lỗi).
        let errorCode: AnyPublisher<String, Never>
    }

    let data: ChoosePromotionBuilder.DataModel
    private(set) var input: Input!

    // ─── Store ────────────────────────────────────────────────────────────────
    // Selection (`selectedIds`) + mở/thu gọn (`myExpanded`) nay do store quản — VC chỉ render.
    private let store: ChoosePromotionStore
    private let stateSubject: CurrentValueSubject<ChoosePromotionState, Never>
    private let errorSubject = PassthroughSubject<String, Never>()
    private var storeCancellable: PromotionCancellable?
    private var didStart = false

    init(router: ChoosePromotionRouter,
         data: ChoosePromotionBuilder.DataModel,
         findEligibleUseCase: FindEligibleCampaignsUseCase = FindEligibleCampaignsUseCase()) {
        self.data = data
        self.store = ChoosePromotionStore(findEligibleCampaignsUseCase: findEligibleUseCase)
        self.stateSubject = CurrentValueSubject(store.currentState())
        super.init(router: router)
    }

    deinit {
        storeCancellable?.cancel()
        store.clear()
    }

    func transform(input: Input) -> Output {
        self.input = input
        bindStore()
        forward(input)
        return buildOutput()
    }

    // ─── Store observation (đối ứng Android.bindStore) ──────────────────────────
    private func bindStore() {
        storeCancellable = observeStore(watch: { self.store.watchState(onEach: $0) }) { [weak self] state in
            guard let self = self else { return }
            self.render(state)
            self.handleError(state)
        }
    }

    // ─── Intent forwarding (đối ứng Android.handleAction) ───────────────────────
    private func forward(_ input: Input) {
        // Search server-side: gõ → store debounce + reload (store lo debounce, VM gửi raw text).
        // Xoá trắng → `ClearKeyword` (reload ngay, không chờ debounce) — khớp `ChoosePromotionFragment`.
        input.searchText
            .dropFirst()
            .sink { [weak self] text in
                guard let self = self else { return }
                if text.isEmpty {
                    self.store.dispatch(intent: ChoosePromotionIntentClearKeyword.shared)
                } else {
                    self.store.dispatch(intent: ChoosePromotionIntentQueryChanged(keyword: text))
                }
            }
            .store(in: &cancellables)

        // Phím "Tìm" trên bàn phím → tìm ngay, bỏ debounce (đối ứng `setOnSearchActionListener` Android).
        input.searchAction
            .sink { [weak self] in self?.store.dispatch(intent: ChoosePromotionIntentSearch.shared) }
            .store(in: &cancellables)

        // Chọn/bỏ chọn: rule single/multi do store quyết định (ToggleSelection) — dùng chung Android.
        input.toggleSelectionRelay
            .sink { [weak self] id in self?.store.dispatch(intent: ChoosePromotionIntentToggleSelection(id: id)) }
            .store(in: &cancellables)

        // "Xem thêm/Thu gọn" nhóm của tôi: store chạy state-machine (mở hết → tải trang kế → thu gọn).
        input.seeMoreMyRelay
            .sink { [weak self] in self?.store.dispatch(intent: ChoosePromotionIntentSeeMoreMy.shared) }
            .store(in: &cancellables)

        // Cuộn đáy "Ưu đãi khác" → load page kế (store tự bỏ nếu hết trang/đang tải).
        input.loadMoreOtherRelay
            .sink { [weak self] in self?.store.dispatch(intent: ChoosePromotionIntentLoadMoreOtherVouchers.shared) }
            .store(in: &cancellables)

        // iOS tự kích load lần đầu (VC gọi transform một lần): seed pre-select rồi preload/fetch.
        if !didStart {
            didStart = true
            store.dispatch(intent: ChoosePromotionIntentSetPreSelected(ids: data.preSelectedVoucherIds))
            store.dispatch(intent: ChoosePromotionIntentPreload(
                myOffers: data.preloadedMy,
                otherOffers: data.preloadedOther,
                myIsLastPage: data.myIsLastPage,
                otherIsLastPage: data.otherIsLastPage
            ))
        }
    }

    // ─── State → View ─────────────────────────────────────────────────────────
    private func render(_ state: ChoosePromotionState) {
        stateSubject.send(state)
    }

    // ─── Error ──────────────────────────────────────────────────────────────────
    private func handleError(_ state: ChoosePromotionState) {
        guard let code = state.errorCode else { return }
        errorSubject.send(code)   // hiện lỗi ra UI (đồng nhất Android — trước đây iOS chỉ consume)
        store.dispatch(intent: ChoosePromotionIntentConsumeError.shared)
    }

    /// Chiếu state → bề mặt view iOS (Output). Đối ứng Android `ChoosePromotionState.toUiState()`.
    /// Selection + mở/thu gọn đều nằm trong `stateSubject` (store) — không còn subject riêng.
    private func buildOutput() -> Output {
        let sections = stateSubject
            .map { [weak self] state in self?.buildSections(state: state) ?? [] }
            .receive(on: DispatchQueue.main)
            .eraseToAnyPublisher()

        let hasSelection = stateSubject.map { !$0.selectedIds.isEmpty }
            .receive(on: DispatchQueue.main).eraseToAnyPublisher()

        let selectedPromotions = stateSubject
            .map { state in state.selectedIds.compactMap { id in Self.allLoaded(state).first { $0.id == id } } }
            .receive(on: DispatchQueue.main).eraseToAnyPublisher()

        return Output(
            sections: sections,
            hasSelection: hasSelection,
            selectedPromotions: selectedPromotions,
            isLoading: stateSubject.map { $0.isLoading }.receive(on: DispatchQueue.main).eraseToAnyPublisher(),
            errorCode: errorSubject.receive(on: DispatchQueue.main).eraseToAnyPublisher()
        )
    }

    // ─── iOS-only UI: dựng sections từ state store (selection + mở/thu gọn đã ở store) ──
    private func buildSections(state: ChoosePromotionState) -> [PromotionSection] {
        let keyword = state.keyword.trimmingCharacters(in: .whitespacesAndNewlines)
        let isSearching = !keyword.isEmpty
        let selectedIds = state.selectedIds
        let cell: (ChooseOffer) -> MyPromotionCellViewModel = { offer in
            MyPromotionCellViewModel(
                offer: offer.source,
                isEnabled: offer.isUsable,
                buttonTitle: PromotionUIStrings.detail,
                showsCheckbox: true,
                isChecked: selectedIds.contains(offer.source.id),
                checkedImage: UIImage.sdk("prm_ic_circle_check"),
                uncheckedImage: UIImage.sdk("prm_ic_circle_uncheck"),
                highlightKeyword: isSearching ? keyword : nil,
                expiringInDays: offer.expiringInDays?.intValue
            )
        }

        var sections: [PromotionSection] = []
        let my = state.myOffers
        if !my.isEmpty {
            // Slice + trạng thái nút "Xem thêm/Thu gọn" đều lấy từ **rule dùng chung** ở promotionLogic
            // (`visibleMyOffers()` / `mySeeMoreState()`) — y như Android, không bên nào tự suy lại.
            let visible = state.visibleMyOffers()
            sections.append(PromotionSection(type: .myPromotions, title: PromotionUIStrings.myPromotions,
                                             items: visible.map(cell), totalCount: my.count,
                                             seeMoreState: Self.seeMoreState(state.mySeeMoreState())))
        }
        let other = state.otherOffers
        if !other.isEmpty {
            sections.append(PromotionSection(type: .otherPromotions, title: PromotionUIStrings.otherPromotions,
                                             items: other.map(cell), totalCount: other.count, seeMoreState: .none))
        }
        return sections
    }

    /// `ChooseSeeMoreState` (rule dùng chung) → enum hiển thị của VC.
    private static func seeMoreState(_ shared: ChooseSeeMoreState) -> SeeMoreState {
        switch shared {
        case .hidden: return .none
        case .collapse: return .collapse
        case .expand: return .expand
        default: return .expand
        }
    }

    private static func allLoaded(_ state: ChoosePromotionState) -> [EligibleOffer] {
        state.myOffers.map { $0.source } + state.otherOffers.map { $0.source }
    }

    private func allLoadedPromotions() -> [EligibleOffer] {
        Self.allLoaded(stateSubject.value)
    }

    func routeToDetail(id: String) {
        if let promotion = allLoadedPromotions().first(where: { $0.id == id }) {
            router.routeToDetail(promotion: promotion)
        }
    }
}
