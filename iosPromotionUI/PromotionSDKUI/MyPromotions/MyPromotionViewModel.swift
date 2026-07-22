//
//  MyPromotionViewModel.swift
//  PromotionSDK
//
//  Lớp bọc mỏng quanh MyPromotionStore (tầng UI-logic dùng chung ở promotionLogic).
//
//  ĐỒNG NHẤT với `MyPromotionViewModel` bên Android từ tên thuộc tính (`store`) tới cấu trúc hàm
//  (`bindStore` / `render` / `handleError` + forward intent cùng thứ tự). Khác biệt duy nhất là bất
//  khả kháng do paradigm: iOS theo MVVM+Combine (`transform`/publishers, quan sát qua `watchState`),
//  Android theo MVI (`handleAction`/`setState`, collect Flow trực tiếp).
//

import Foundation
import Combine
@_implementationOnly import PRMKotlinBridge

final class MyPromotionViewModel: PRMBaseViewModel<MyPromotionRouter>, PRMViewModelType {

    struct Input {
        let refreshTrigger: AnyPublisher<Void, Never>
        let loadMoreTrigger: AnyPublisher<Void, Never>
        let selectPromotionByIDRelay: PassthroughSubject<String, Never>
        let selectTabRelay: PassthroughSubject<String, Never>
    }

    struct Output {
        let isRefreshing: AnyPublisher<Bool, Never>
        let isLoadingMore: AnyPublisher<Bool, Never>
        let isLoading: AnyPublisher<Bool, Never>
        let promotions: AnyPublisher<[MyPromotionCellViewModel], Never>
        let tabs: AnyPublisher<[MyPromotionTab], Never>
        let selectedTabCode: AnyPublisher<String, Never>
        let canLoadMore: AnyPublisher<Bool, Never>
        let isEmpty: AnyPublisher<Bool, Never>
        /// Phát **mã lỗi** (raw) — view map code → chuỗi (đồng nhất Android: Fragment.mapErrorMessage).
        let errorCode: AnyPublisher<String, Never>
    }

    let data: MyPromotionBuilder.DataModel
    private(set) var input: Input!

    // ─── Store ────────────────────────────────────────────────────────────────
    private let store: MyPromotionStore
    /// Gương state của store trên iOS (đã hop về main). Seed = state hiện tại.
    private let stateSubject: CurrentValueSubject<MyPromotionState, Never>
    private let errorSubject = PassthroughSubject<String, Never>()
    private var storeCancellable: PromotionCancellable?
    private var didStart = false

    init(router: MyPromotionRouter,
         data: MyPromotionBuilder.DataModel,
         searchVouchersUseCase: SearchCustomerVouchersUseCase = SearchCustomerVouchersUseCase()) {
        self.data = data
        self.store = MyPromotionStore(searchCustomerVouchersUseCase: searchVouchersUseCase)
        self.stateSubject = CurrentValueSubject(store.currentState())
        super.init(router: router)
    }

    deinit {
        storeCancellable?.cancel()
        store.clear()
    }

    func routeToSearch() {
        router.routeToSearch()
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
        input.refreshTrigger
            .sink { [weak self] in self?.store.dispatch(intent: MyPromotionIntentRefresh.shared) }
            .store(in: &cancellables)
        input.loadMoreTrigger
            .sink { [weak self] in self?.store.dispatch(intent: MyPromotionIntentLoadMore.shared) }
            .store(in: &cancellables)
        input.selectTabRelay
            .sink { [weak self] code in self?.store.dispatch(intent: MyPromotionIntentSelectTab(tabCode: code)) }
            .store(in: &cancellables)
        // Mở Detail bằng promotion cơ bản; màn Detail tự fetch chi tiết (điều hướng — không ở store).
        input.selectPromotionByIDRelay
            .sink { [weak self] id in
                guard let self = self,
                      let voucher = self.stateSubject.value.vouchers.first(where: { $0.source.voucherId == id })
                else { return }
                self.router.routeToDetail(promotion: voucher.source)
            }
            .store(in: &cancellables)
        // iOS tự kích load lần đầu (VC gọi transform một lần); Android do Fragment kích LoadInitialIfNeeded.
        if !didStart {
            didStart = true
            store.dispatch(intent: MyPromotionIntentLoadInitialIfNeeded.shared)
        }
    }

    // ─── State → View ─────────────────────────────────────────────────────────
    private func render(_ state: MyPromotionState) {
        stateSubject.send(state)
    }

    // ─── Error ──────────────────────────────────────────────────────────────────
    private func handleError(_ state: MyPromotionState) {
        guard let code = state.errorCode else { return }
        errorSubject.send(code)   // view map code → chuỗi
        store.dispatch(intent: MyPromotionIntentConsumeError.shared)
    }

    /// Chiếu state dùng chung (`MyPromotionState`) → **bề mặt view iOS** (`Output` = các Combine publisher).
    /// Cùng vai trò với `MyPromotionState.toUiState()` bên Android; khác cấu trúc do idiom: iOS **dựng
    /// graph publisher** (tách theo field, map mỗi lần state đổi), Android gom thành **một** object UiState.
    private func buildOutput() -> Output {
        Output(
            isRefreshing: stateSubject.map { $0.isRefreshing }.eraseToAnyPublisher(),
            isLoadingMore: stateSubject.map { $0.isLoadingMore }.eraseToAnyPublisher(),
            isLoading: stateSubject.map { $0.isLoading && $0.vouchers.isEmpty }.eraseToAnyPublisher(),
            promotions: stateSubject.map { $0.vouchers.map { MyPromotionCellViewModel(voucher: $0.source) } }.eraseToAnyPublisher(),
            tabs: stateSubject.map { $0.tabs }.eraseToAnyPublisher(),
            selectedTabCode: stateSubject.map { $0.selectedTabCode ?? "all" }.eraseToAnyPublisher(),
            canLoadMore: stateSubject.map { !$0.isLastPage }.eraseToAnyPublisher(),
            isEmpty: stateSubject.map { !$0.isLoading && $0.isEmpty }.eraseToAnyPublisher(),
            errorCode: errorSubject.eraseToAnyPublisher()
        )
    }
}
