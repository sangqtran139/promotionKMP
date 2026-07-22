//
//  ChoosePromotionViewModel.swift
//  PromotionSDK
//
//  Lớp bọc mỏng quanh ChoosePromotionStore (tầng UI-logic dùng chung ở promotionLogic).
//  ĐỒNG NHẤT với `ChoosePromotionViewModel` bên Android — cùng `store` / `bindStore` / `render` /
//  `handleError` + forward intent cùng thứ tự (xem `MyPromotionViewModel`). Load/paging/search nằm ở
//  store; selection + "Xem thêm" (visibleCount) + build sections là UI native của iOS.
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

    /// Số "Ưu đãi của tôi" hiển thị ban đầu trước khi bấm "Xem thêm" (khớp Android COLLAPSED_COUNT = 2).
    private static let myPromotionsInitialVisibleCount = 2
    /// Cho phép chọn nhiều voucher (khớp `ChoosePromotionFragment.isMultiSelection`). Hiện = chọn đơn.
    private static let isMultiSelection = false

    struct Input {
        let searchText: AnyPublisher<String, Never>
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
    }

    let data: ChoosePromotionBuilder.DataModel
    private(set) var input: Input!

    // ─── Store ────────────────────────────────────────────────────────────────
    private let store: ChoosePromotionStore
    private let stateSubject: CurrentValueSubject<ChoosePromotionState, Never>
    /// Voucher đang chọn (id, sẵn sàng multi-select) — UI native.
    private let selectedPromotionSubject: CurrentValueSubject<[String], Never>
    /// Số item "Ưu đãi của tôi" đang hiện ("Xem thêm") — UI native (Android hiện hết trong list).
    private let visibleCountSubject: CurrentValueSubject<Int, Never>
    private var storeCancellable: PromotionCancellable?
    private var didStart = false

    init(router: ChoosePromotionRouter,
         data: ChoosePromotionBuilder.DataModel,
         findEligibleUseCase: FindEligibleCampaignsUseCase = FindEligibleCampaignsUseCase()) {
        self.data = data
        self.store = ChoosePromotionStore(findEligibleCampaignsUseCase: findEligibleUseCase)
        self.stateSubject = CurrentValueSubject(store.currentState())
        self.selectedPromotionSubject = CurrentValueSubject(data.preSelectedVoucherIds)
        self.visibleCountSubject = CurrentValueSubject(Self.myPromotionsInitialVisibleCount)
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
        storeCancellable = store.watchState { [weak self] state in
            DispatchQueue.main.async {
                guard let self = self else { return }
                self.render(state)
                self.handleError(state)
            }
        }
    }

    // ─── Intent forwarding (đối ứng Android.handleAction) ───────────────────────
    private func forward(_ input: Input) {
        // Search server-side: gõ → store debounce + reload (store lo debounce, VM gửi raw text).
        input.searchText
            .dropFirst()
            .sink { [weak self] text in self?.store.dispatch(intent: ChoosePromotionIntentQueryChanged(keyword: text)) }
            .store(in: &cancellables)

        // Chọn/bỏ chọn (khớp Android): trùng id → bỏ; chưa chọn → multi thì thêm, đơn thì thay cả list.
        input.toggleSelectionRelay
            .sink { [weak self] id in
                guard let self = self else { return }
                var selected = self.selectedPromotionSubject.value
                if selected.contains(id) {
                    selected.removeAll { $0 == id }
                } else if Self.isMultiSelection {
                    selected.append(id)
                } else {
                    selected = [id]
                }
                self.selectedPromotionSubject.send(selected)
            }
            .store(in: &cancellables)

        // "Xem thêm" nhóm của tôi: lộ hết data đã tải → load page kế → hết thì "Thu gọn".
        input.seeMoreMyRelay
            .sink { [weak self] in self?.onSeeMoreMy() }
            .store(in: &cancellables)

        // Cuộn đáy "Ưu đãi khác" → load page kế (store tự bỏ nếu hết trang/đang tải).
        input.loadMoreOtherRelay
            .sink { [weak self] in self?.store.dispatch(intent: ChoosePromotionIntentLoadMoreOtherVouchers.shared) }
            .store(in: &cancellables)

        // iOS tự kích load lần đầu (VC gọi transform một lần): preload từ widget nếu có, không thì store fetch.
        if !didStart {
            didStart = true
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
        guard state.errorCode != nil else { return }
        // Màn chọn không có kênh lỗi ra UI (giữ như bản cũ) — chỉ consume để không phát lại.
        store.dispatch(intent: ChoosePromotionIntentConsumeError.shared)
    }

    /// Chiếu state → bề mặt view iOS (Output). Đối ứng Android `ChoosePromotionState.toUiState()`.
    private func buildOutput() -> Output {
        let sections = Publishers.CombineLatest3(selectedPromotionSubject, stateSubject, visibleCountSubject)
            .map { [weak self] selectedIds, state, visibleCount -> [PromotionSection] in
                self?.buildSections(state: state, selectedIds: selectedIds, visibleCount: visibleCount) ?? []
            }
            .receive(on: DispatchQueue.main)
            .eraseToAnyPublisher()

        let hasSelection = selectedPromotionSubject.map { !$0.isEmpty }
            .receive(on: DispatchQueue.main).eraseToAnyPublisher()

        let selectedPromotions = selectedPromotionSubject
            .map { [weak self] ids in ids.compactMap { id in self?.allLoadedPromotions().first { $0.id == id } } }
            .receive(on: DispatchQueue.main).eraseToAnyPublisher()

        return Output(
            sections: sections,
            hasSelection: hasSelection,
            selectedPromotions: selectedPromotions,
            isLoading: stateSubject.map { $0.isLoading }.receive(on: DispatchQueue.main).eraseToAnyPublisher()
        )
    }

    // ─── iOS-only UI: build sections, see-more, chọn, điều hướng ────────────────
    private func buildSections(state: ChoosePromotionState, selectedIds: [String], visibleCount: Int) -> [PromotionSection] {
        let keyword = state.keyword.trimmingCharacters(in: .whitespacesAndNewlines)
        let isSearching = !keyword.isEmpty
        let cell: (ChooseOffer) -> MyPromotionCellViewModel = { offer in
            MyPromotionCellViewModel(
                offer: offer.source,
                buttonTitle: "Chi tiết",
                showsCheckbox: true,
                isChecked: selectedIds.contains(offer.source.id),
                checkedImage: UIImage.sdk("prm_ic_circle_check"),
                uncheckedImage: UIImage.sdk("prm_ic_circle_uncheck"),
                highlightKeyword: isSearching ? keyword : nil
            )
        }

        var sections: [PromotionSection] = []
        let my = state.myOffers
        if !my.isEmpty {
            let visible = Array(my.prefix(visibleCount))
            let seeMore: SeeMoreState
            if visibleCount < my.count || !state.myIsLastPage {
                seeMore = .expand
            } else if my.count > Self.myPromotionsInitialVisibleCount {
                seeMore = .collapse
            } else {
                seeMore = .none
            }
            sections.append(PromotionSection(type: .myPromotions, title: "Ưu đãi của tôi",
                                             items: visible.map(cell), totalCount: my.count, seeMoreState: seeMore))
        }
        let other = state.otherOffers
        if !other.isEmpty {
            sections.append(PromotionSection(type: .otherPromotions, title: "Ưu đãi khác",
                                             items: other.map(cell), totalCount: other.count, seeMoreState: .none))
        }
        return sections
    }

    private func onSeeMoreMy() {
        let state = stateSubject.value
        let loaded = state.myOffers.count
        if visibleCountSubject.value < loaded {
            visibleCountSubject.send(Int.max)   // lộ hết đã tải + tự hiện item load-more về sau
        } else if !state.myIsLastPage {
            store.dispatch(intent: ChoosePromotionIntentLoadMoreMyVouchers.shared)
        } else {
            visibleCountSubject.send(Self.myPromotionsInitialVisibleCount)   // thu gọn
        }
    }

    private func allLoadedPromotions() -> [EligibleOffer] {
        stateSubject.value.myOffers.map { $0.source } + stateSubject.value.otherOffers.map { $0.source }
    }

    func routeToDetail(id: String) {
        if let promotion = allLoadedPromotions().first(where: { $0.id == id }) {
            router.routeToDetail(promotion: promotion)
        }
    }
}
