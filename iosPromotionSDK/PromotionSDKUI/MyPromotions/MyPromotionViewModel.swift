//
//  MyPromotionViewModel.swift
//  PromotionSDK
//
//  Lớp bọc mỏng quanh MyPromotionStore (tầng UI-logic dùng chung ở promotionLogic).
//
//  ĐỒNG NHẤT với `MyPromotionViewModel` bên Android từ tên thuộc tính (`store`) tới cấu trúc hàm
//  (`bindStore` / `handleAction` / `render` / `handleError`, cùng thứ tự).
//
//  KHÔNG dùng Combine: store đã phơi callback (`watchState`), nên VM cũng phơi callback
//  (`onState` / `onEffect`) — đối ứng 1-1 `uiState: StateFlow` / `uiEffect: Flow` bên Android.
//

import Foundation
@_implementationOnly import PRMKotlinBridge

final class MyPromotionViewModel: PRMBaseViewModel<MyPromotionRouter> {

    /// Bề mặt view (đã format) — đối ứng `MyPromotionUiState` bên Android.
    struct UiState {
        var isRefreshing = false
        var isLoadingMore = false
        var isLoading = false
        var promotions: [MyPromotionCellViewModel] = []
        var tabs: [MyPromotionTab] = []
        var selectedTabCode = "all"
        var canLoadMore = false
        var isEmpty = false
    }

    /// Đối ứng `MyPromotionAction` bên Android — chỉ những gì màn thật sự phát.
    enum Action {
        case loadInitialIfNeeded
        case refresh
        case loadMore
        case selectTab(String)
        case selectPromotion(String)
        case openServiceSelector(String)
        case serviceSelected(ServiceSelectorItem)
    }

    /// Sự kiện một-lần — đối ứng `MyPromotionEffect` bên Android.
    enum Effect {
        case showError(String)
        case showServiceSelector(voucherId: String, services: [ServiceSelectorItem])
    }

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
    private let store: MyPromotionStore
    private var storeCancellable: PromotionCancellable?

    /// Không nhận `DataModel`: màn này không có tham số đầu vào — token đọc từ
    /// `PromotionRequestContextProvider` của lõi. `MyPromotionBuilder.DataModel` là struct rỗng,
    /// chỉ tồn tại để khớp generic `Dependency` của `PRMBaseBuilder`.
    init(router: MyPromotionRouter,
         searchVouchersUseCase: SearchCustomerVouchersUseCase = SearchCustomerVouchersUseCase()) {
        self.store = MyPromotionStore(searchCustomerVouchersUseCase: searchVouchersUseCase)
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
        case .loadInitialIfNeeded:
            store.dispatch(intent: MyPromotionIntentLoadInitialIfNeeded.shared)
        case .refresh:
            store.dispatch(intent: MyPromotionIntentRefresh.shared)
        case .loadMore:
            store.dispatch(intent: MyPromotionIntentLoadMore.shared)
        case .selectTab(let code):
            store.dispatch(intent: MyPromotionIntentSelectTab(tabCode: code))
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
    private func render(_ state: MyPromotionState) {
        uiState = state.toUiState()
    }

    // ─── Error ──────────────────────────────────────────────────────────────────
    private func handleError(_ state: MyPromotionState) {
        guard let code = state.errorCode else { return }
        onEffect?(.showError(code))   // view map code → chuỗi
        store.dispatch(intent: MyPromotionIntentConsumeError.shared)
    }

    // ─── Bottom sheet "Chọn dịch vụ" (render native; lọc dùng chung ở promotionLogic) ──
    /// Đối ứng `MyPromotionViewModel.openServiceSelector(voucher)` bên Android.
    private func openServiceSelector(voucherId: String) {
        guard let voucher = store.currentState().vouchers.first(where: { $0.source.voucherId == voucherId })
        else { return }
        let services = ServiceSelectorBuilder.items(forApplicableProducts: voucher.source.applicableProducts)
        onEffect?(.showServiceSelector(voucherId: voucherId, services: services))
    }

    // ─── iOS-only: điều hướng sang màn Tìm kiếm (Android mở bằng Fragment ở tầng view) ──
    func routeToSearch() {
        router.routeToSearch()
    }
}

// ─── Map state dùng chung (store) → model UI iOS ──────────────────────────────

/// Chiếu state dùng chung ([MyPromotionState]) → **bề mặt view iOS** (`UiState`).
/// Đối ứng 1-1 `private fun MyPromotionState.toUiState()` bên Android (cũng là hàm mức file).
private extension MyPromotionState {
    func toUiState() -> MyPromotionViewModel.UiState {
        MyPromotionViewModel.UiState(
            isRefreshing: isRefreshing,
            isLoadingMore: isLoadingMore,
            isLoading: isLoading && vouchers.isEmpty,
            promotions: vouchers.map {
                MyPromotionCellViewModel(voucher: $0.source,
                                         isEnabled: $0.isEnabled,
                                         expiringInDays: $0.expiringInDays?.intValue)
            },
            tabs: tabs,
            selectedTabCode: selectedTabCode ?? "all",
            canLoadMore: !isLastPage,
            isEmpty: !isLoading && isEmpty
        )
    }
}
