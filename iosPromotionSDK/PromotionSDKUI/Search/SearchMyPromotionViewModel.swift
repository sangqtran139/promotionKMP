//
//  SearchMyPromotionViewModel.swift
//  PromotionSDK
//
//  Lớp bọc mỏng quanh SearchMyPromotionStore (tầng UI-logic dùng chung ở promotionLogic).
//  ĐỒNG NHẤT với `SearchMyPromotionViewModel` bên Android — cùng `store` / `bindStore` /
//  `handleAction` / `render` / `handleError`, cùng thứ tự. Debounce/search/paging nằm ở store;
//  VM chỉ forward action → dispatch, và chiếu state → bề mặt view.
//
//  KHÔNG dùng Combine: store đã phơi callback (`watchState`), nên VM cũng phơi callback
//  (`onState` / `onEffect`) — đối ứng 1-1 `uiState: StateFlow` / `uiEffect: Flow` bên Android.
//  Trước đây khúc giữa Combine (Input/Output publisher + transform) chỉ làm đường ống: không có
//  combineLatest/debounce/merge nào, nên bỏ đi là mất boilerplate chứ không mất chức năng.
//

import Foundation
@_implementationOnly import PRMKotlinBridge

final class SearchMyPromotionViewModel: PRMBaseViewModel<SearchMyPromotionRouter> {

    /// Bề mặt view (đã format) — đối ứng `SearchMyPromotionUiState` bên Android.
    struct UiState {
        var promotions: [MyPromotionCellViewModel] = []
        var isLoading = false
        var isLoadingMore = false
        var isEmpty = false
    }

    /// Đối ứng `SearchMyPromotionAction` bên Android — chỉ những gì màn thật sự phát.
    enum Action {
        case queryChanged(String)
        case search
        case loadMore
        case selectPromotion(String)
        case openServiceSelector(String)
        case serviceSelected(ServiceSelectorItem)
    }

    /// Sự kiện một-lần — đối ứng `SearchMyPromotionEffect` bên Android.
    enum Effect {
        case showError(String)
        case showServiceSelector(voucherId: String, services: [ServiceSelectorItem])
    }

    /// State hiện tại + kênh phát. Gán `onState` là **nhận ngay** state hiện tại — mô phỏng đúng
    /// hành vi replay của `StateFlow` bên Android (`collectFlow` nhận value mới nhất khi bắt đầu).
    private(set) var uiState = UiState() {
        didSet { onState?(uiState) }
    }
    var onState: ((UiState) -> Void)? {
        didSet { onState?(uiState) }
    }
    /// Một-lần, KHÔNG replay (giống effect bên Android): subscriber mới không nhận lại lỗi cũ.
    var onEffect: ((Effect) -> Void)?

    // ─── Store ────────────────────────────────────────────────────────────────
    private let store: SearchMyPromotionStore
    private var storeCancellable: PromotionCancellable?

    /// Không nhận `DataModel`: màn Tìm kiếm không có tham số đầu vào nào — customerId/token đọc từ
    /// `PromotionRequestContextProvider` của lõi, keyword do user gõ. `SearchMyPromotionBuilder.DataModel`
    /// là struct rỗng, chỉ tồn tại để khớp generic `Dependency` của `PRMBaseBuilder`.
    init(router: SearchMyPromotionRouter,
         searchVouchersUseCase: SearchCustomerVouchersUseCase = SearchCustomerVouchersUseCase()) {
        self.store = SearchMyPromotionStore(searchCustomerVouchersUseCase: searchVouchersUseCase)
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
        case .queryChanged(let keyword):
            store.dispatch(intent: SearchMyPromotionIntentQueryChanged(keyword: keyword))
        case .search:
            store.dispatch(intent: SearchMyPromotionIntentSearch.shared)
        case .loadMore:
            store.dispatch(intent: SearchMyPromotionIntentLoadMore.shared)
        case .selectPromotion(let id):
            // Mở Detail bằng promotion cơ bản; màn Detail tự fetch chi tiết (điều hướng — không ở store).
            guard let voucher = store.currentState().vouchers.first(where: { $0.source.voucherId == id })
            else { return }
            router.routeToDetail(promotion: voucher.source)
        case .openServiceSelector(let id):
            openServiceSelector(voucherId: id)
        case .serviceSelected:
            break   // TODO: điều hướng màn dịch vụ khi có đích đến
        }
    }

    // ─── State → View ─────────────────────────────────────────────────────────
    private func render(_ state: SearchMyPromotionState) {
        uiState = state.toUiState()
    }

    // ─── Error ──────────────────────────────────────────────────────────────────
    private func handleError(_ state: SearchMyPromotionState) {
        guard let code = state.errorCode else { return }
        onEffect?(.showError(code))   // view map code → chuỗi
        store.dispatch(intent: SearchMyPromotionIntentConsumeError.shared)
    }

    // ─── Bottom sheet "Chọn dịch vụ" (render native; lọc dùng chung ở promotionLogic) ──
    /// Đối ứng `SearchMyPromotionViewModel.openServiceSelector(voucher)` bên Android.
    private func openServiceSelector(voucherId: String) {
        guard let voucher = store.currentState().vouchers.first(where: { $0.source.voucherId == voucherId })
        else { return }
        let services = ServiceSelectorBuilder.items(forApplicableProducts: voucher.source.applicableProducts)
        onEffect?(.showServiceSelector(voucherId: voucherId, services: services))
    }
}

// ─── Map state dùng chung (store) → model UI iOS ──────────────────────────────

/// Chiếu state dùng chung ([SearchMyPromotionState]) → **bề mặt view iOS** (`UiState`).
/// Đối ứng 1-1 `private fun SearchMyPromotionState.toUiState()` bên Android (cũng là hàm mức file).
private extension SearchMyPromotionState {
    func toUiState() -> SearchMyPromotionViewModel.UiState {
        SearchMyPromotionViewModel.UiState(
            promotions: vouchers.map {
                MyPromotionCellViewModel(voucher: $0.source,
                                         isEnabled: $0.isEnabled,
                                         highlightKeyword: keyword,
                                         expiringInDays: $0.expiringInDays?.intValue)
            },
            isLoading: isLoading,
            isLoadingMore: isLoadingMore,
            isEmpty: !isLoading && isEmpty
        )
    }
}
