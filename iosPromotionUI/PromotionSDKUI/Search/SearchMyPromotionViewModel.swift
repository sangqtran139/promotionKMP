//
//  SearchMyPromotionViewModel.swift
//  PromotionSDK
//
//  Lớp bọc mỏng quanh SearchMyPromotionStore (tầng UI-logic dùng chung ở promotionLogic).
//  ĐỒNG NHẤT với `SearchMyPromotionViewModel` bên Android — cùng `store` / `bindStore` / `render` /
//  `handleError` + forward intent cùng thứ tự (xem `MyPromotionViewModel` để hiểu quy ước chung).
//  Debounce/search/paging nằm ở store; VM chỉ forward input → dispatch, map state → Output.
//

import Foundation
import Combine
@_implementationOnly import PRMKotlinBridge

final class SearchMyPromotionViewModel: PRMBaseViewModel<SearchMyPromotionRouter>, PRMViewModelType {

    struct Input {
        let searchText: AnyPublisher<String, Never>
        let searchAction: AnyPublisher<Void, Never>
        let loadMoreTrigger: AnyPublisher<Void, Never>
        let selectPromotionByIDRelay: PassthroughSubject<String, Never>
    }

    struct Output {
        let promotions: AnyPublisher<[MyPromotionCellViewModel], Never>
        let isLoading: AnyPublisher<Bool, Never>
        let isLoadingMore: AnyPublisher<Bool, Never>
        let isEmpty: AnyPublisher<Bool, Never>
        let validationError: AnyPublisher<String?, Never>
    }

    let data: SearchMyPromotionBuilder.DataModel
    private(set) var input: Input!

    // ─── Store ────────────────────────────────────────────────────────────────
    private let store: SearchMyPromotionStore
    private let stateSubject: CurrentValueSubject<SearchMyPromotionState, Never>
    private let errorSubject = CurrentValueSubject<String?, Never>(nil)
    private var storeCancellable: PromotionCancellable?

    init(router: SearchMyPromotionRouter,
         data: SearchMyPromotionBuilder.DataModel,
         searchVouchersUseCase: SearchCustomerVouchersUseCase = SearchCustomerVouchersUseCase()) {
        self.data = data
        self.store = SearchMyPromotionStore(searchCustomerVouchersUseCase: searchVouchersUseCase)
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
        input.searchText
            .sink { [weak self] text in self?.store.dispatch(intent: SearchMyPromotionIntentQueryChanged(keyword: text)) }
            .store(in: &cancellables)
        input.searchAction
            .sink { [weak self] in self?.store.dispatch(intent: SearchMyPromotionIntentSearch.shared) }
            .store(in: &cancellables)
        input.loadMoreTrigger
            .sink { [weak self] in self?.store.dispatch(intent: SearchMyPromotionIntentLoadMore.shared) }
            .store(in: &cancellables)
        input.selectPromotionByIDRelay
            .sink { [weak self] id in
                guard let self = self,
                      let voucher = self.stateSubject.value.vouchers.first(where: { $0.source.voucherId == id })
                else { return }
                self.router.routeToDetail(promotion: voucher.source)
            }
            .store(in: &cancellables)
    }

    // ─── State → View ─────────────────────────────────────────────────────────
    private func render(_ state: SearchMyPromotionState) {
        stateSubject.send(state)
    }

    // ─── Error ──────────────────────────────────────────────────────────────────
    private func handleError(_ state: SearchMyPromotionState) {
        guard let code = state.errorCode else { return }
        errorSubject.send(code)   // view map code → chuỗi
        store.dispatch(intent: SearchMyPromotionIntentConsumeError.shared)
    }

    /// Chiếu state dùng chung → bề mặt view iOS (Output). Đối ứng Android `SearchMyPromotionState.toUiState()`.
    private func buildOutput() -> Output {
        Output(
            promotions: stateSubject
                .map { state in state.vouchers.map { MyPromotionCellViewModel(voucher: $0.source, highlightKeyword: state.keyword, expiringInDays: $0.expiringInDays?.intValue) } }
                .eraseToAnyPublisher(),
            isLoading: stateSubject.map { $0.isLoading }.eraseToAnyPublisher(),
            isLoadingMore: stateSubject.map { $0.isLoadingMore }.eraseToAnyPublisher(),
            isEmpty: stateSubject.map { !$0.isLoading && $0.isEmpty }.eraseToAnyPublisher(),
            validationError: errorSubject.eraseToAnyPublisher()
        )
    }
}
