//
//  MyPromotionViewModel.swift
//  PromotionSDK
//
//  Created by thachlh on 6/5/26.
//

import Foundation
import Combine
@_implementationOnly import PRMKotlinBridge

final class MyPromotionViewModel: PRMBaseViewModel<MyPromotionRouter>, PRMViewModelType {

    struct Input {
        let refreshTrigger: AnyPublisher<Void, Never>
        let loadMoreTrigger: AnyPublisher<Void, Never>
        let selectPromotionByIDRelay: PassthroughSubject<String, Never>
        /// Code tab được chọn (tabs động lấy từ API).
        let selectTabRelay: PassthroughSubject<String, Never>
    }

    struct Output {
        let isRefreshing: AnyPublisher<Bool, Never>
        let isLoadingMore: AnyPublisher<Bool, Never>
        let isLoading: AnyPublisher<Bool, Never>
        let promotions: AnyPublisher<[MyPromotionCellViewModel], Never>
        /// Tabs động từ API (code/label/count, đã sort).
        let tabs: AnyPublisher<[VoucherTabItem], Never>
        let selectedTabCode: AnyPublisher<String, Never>
        /// Còn trang để load thêm không — feed cho `PRMRefreshTableView.isHasMorePage`.
        let canLoadMore: AnyPublisher<Bool, Never>
        /// List rỗng (đã tải xong, không có voucher) → hiện empty view (khớp Android ctlNoResult).
        let isEmpty: AnyPublisher<Bool, Never>
        /// Thông báo lỗi fetch → hiện toast (khớp Android ShowError). Không replay (như `Signal`).
        let errorMessage: AnyPublisher<String, Never>
    }

    let data: MyPromotionBuilder.DataModel
    private(set) var input: Input!

    // MARK: - State
    private let domainPromotions = CurrentValueSubject<[VoucherItem], Never>([])
    /// Code tab đang chọn (mặc định "all"; tabs về từ API sẽ render động).
    private let selectedTabCode = CurrentValueSubject<String, Never>("all")
    private let tabsSubject = CurrentValueSubject<[VoucherTabItem], Never>([])
    private let isRefreshingSubject = PassthroughSubject<Bool, Never>()
    private let isLoadingMoreSubject = PassthroughSubject<Bool, Never>()
    private let isLoadingSubject = CurrentValueSubject<Bool, Never>(true)
    private let canLoadMoreSubject = CurrentValueSubject<Bool, Never>(true)
    private let errorSubject = PassthroughSubject<String, Never>()

    private var currentPage = 0
    /// Cờ "đang có request chạy" — chặn load-more chồng (khớp Android guard isLoading/isLoadingMore/...).
    private var isFetching = false
    /// Token "latest wins": mỗi fetch tăng 1; response mang token cũ bị bỏ qua. Kotlin/Native không
    /// hủy coroutine khi hủy `Task`, nên vẫn cần guard này để bỏ đúng response đã cũ.
    private var fetchGeneration = 0
    private var fetchTask: Task<Void, Never>?

    private let searchVouchersUseCase: SearchCustomerVouchersUseCase

    /// Cache list theo tab (RAM, sống cùng ViewModel) — quay lại tab đã xem hiện ngay rồi refresh ngầm (khớp Android).
    private struct TabCache {
        let promotions: [VoucherItem]
        let page: Int
        let isLastPage: Bool
    }
    private var tabCaches: [String: TabCache] = [:]

    init(router: MyPromotionRouter,
         data: MyPromotionBuilder.DataModel,
         searchVouchersUseCase: SearchCustomerVouchersUseCase = SearchCustomerVouchersUseCase()) {
        self.data = data
        self.searchVouchersUseCase = searchVouchersUseCase
        super.init(router: router)
    }

    func routeToSearch() {
        router.routeToSearch(customerId: data.customerId, token: data.token)
    }

    func transform(input: Input) -> Output {
        self.input = input

        // Đổi tab: tab đã có cache -> hiện ngay từ cache rồi refresh ngầm; chưa cache -> giữ list cũ + tải mới.
        input.selectTabRelay
            .sink { [weak self] code in
                guard let self = self, code != self.selectedTabCode.value else { return }
                self.selectedTabCode.send(code)
                if let cache = self.tabCaches[code] {
                    self.domainPromotions.send(cache.promotions)
                    self.currentPage = cache.page
                    self.canLoadMoreSubject.send(!cache.isLastPage)
                    self.fetch(page: 0, silent: true)
                } else {
                    self.fetch(page: 0, silent: false)
                }
            }
            .store(in: &cancellables)

        input.refreshTrigger
            .sink { [weak self] in self?.fetch(page: 0, silent: false) }
            .store(in: &cancellables)

        input.loadMoreTrigger
            // Bỏ qua khi đang có request chạy hoặc hết trang → không stack load-more, không rớt trang (khớp Android).
            .sink { [weak self] in
                guard let self = self, !self.isFetching, self.canLoadMoreSubject.value else { return }
                self.fetch(page: self.currentPage + 1, silent: false)
            }
            .store(in: &cancellables)

        // Mở Detail bằng promotion cơ bản; màn Detail tự fetch detail đầy đủ (giống Android).
        input.selectPromotionByIDRelay
            .sink { [weak self] id in
                guard let self = self else { return }
                if let promotion = self.domainPromotions.value.first(where: { $0.voucherId == id }) {
                    self.router.routeToDetail(promotion: promotion, customerId: self.data.customerId, token: self.data.token)
                }
            }
            .store(in: &cancellables)

        // Tải lần đầu khi mở màn.
        fetch(page: 0, silent: false)

        let promotions = domainPromotions
            .map { models in models.map { MyPromotionCellViewModel(voucher: $0) } }
            .receive(on: DispatchQueue.main)
            .eraseToAnyPublisher()

        // Empty: đã tải xong (không skeleton) và list rỗng (khớp Android !isLoading && isEmpty).
        let isEmpty = Publishers.CombineLatest(domainPromotions, isLoadingSubject)
            .map { promotions, loading in !loading && promotions.isEmpty }
            .receive(on: DispatchQueue.main)
            .eraseToAnyPublisher()

        return Output(
            isRefreshing: isRefreshingSubject.receive(on: DispatchQueue.main).eraseToAnyPublisher(),
            isLoadingMore: isLoadingMoreSubject.receive(on: DispatchQueue.main).eraseToAnyPublisher(),
            isLoading: isLoadingSubject.receive(on: DispatchQueue.main).eraseToAnyPublisher(),
            promotions: promotions,
            tabs: tabsSubject.receive(on: DispatchQueue.main).eraseToAnyPublisher(),
            selectedTabCode: selectedTabCode.receive(on: DispatchQueue.main).eraseToAnyPublisher(),
            canLoadMore: canLoadMoreSubject.receive(on: DispatchQueue.main).eraseToAnyPublisher(),
            isEmpty: isEmpty,
            errorMessage: errorSubject.receive(on: DispatchQueue.main).eraseToAnyPublisher()
        )
    }

    // MARK: - Fetch (Task + token guard cho "latest wins")

    private func fetch(page: Int, silent: Bool) {
        // Hủy request trước, chỉ giữ cái mới nhất.
        fetchTask?.cancel()
        fetchGeneration += 1
        let token = fetchGeneration

        isFetching = true
        if page > 0 {
            isLoadingMoreSubject.send(true)
        } else if silent {
            // refresh ngầm tab đã cache: không spinner, không skeleton.
        } else if domainPromotions.value.isEmpty {
            isLoadingSubject.send(true)       // skeleton chỉ khi list trống (khớp Android)
        } else {
            isRefreshingSubject.send(true)    // đã có list -> spinner refresh
        }

        let tab = selectedTabCode.value
        let customerId = data.customerId
        let trimmedCustomerId = customerId.trimmingCharacters(in: .whitespacesAndNewlines)

        fetchTask = Task { @MainActor [weak self] in
            guard let self = self else { return }

            // Thiếu customerId → không gọi mạng (khớp Android MISSING_CUSTOMER_ID).
            let listModel: SearchCustomerVouchersResult?
            if trimmedCustomerId.isEmpty {
                listModel = nil
            } else {
                // Token do host cấp qua `PromotionRequestContextProvider` của lõi, không gửi từng request.
                let request = SearchCustomerVouchersRequest(
                    customerId: customerId,
                    keyword: nil,
                    serviceCode: nil,
                    tab: tab,
                    page: boxed(page),
                    size: boxed(10)
                )
                let useCase = self.searchVouchersUseCase
                do {
                    listModel = try await useCase.invoke(request: request)
                } catch {
                    listModel = nil
                }
            }

            // Đã có fetch mới hơn → bỏ qua.
            guard token == self.fetchGeneration else { return }

            self.isRefreshingSubject.send(false)
            self.isLoadingMoreSubject.send(false)
            self.isLoadingSubject.send(false)
            self.isFetching = false

            // Chống đè nhầm: bỏ qua response của tab không còn được chọn (khớp Android shouldApplyResponse).
            guard tab == self.selectedTabCode.value else { return }
            // listModel nil = fetch lỗi / thiếu customerId → báo toast, không đè list (khớp Android ShowError).
            guard let listModel = listModel else {
                self.errorSubject.send(trimmedCustomerId.isEmpty ? "Thiếu thông tin khách hàng." : "Đã có lỗi xảy ra. Vui lòng thử lại.")
                return
            }

            if !listModel.tabs.isEmpty {
                self.tabsSubject.send(listModel.tabs)
            }

            let isLastPage = listModel.last?.boolValue ?? true
            let merged: [VoucherItem] = (page == 0)
                ? listModel.content
                : (self.domainPromotions.value + listModel.content)
            self.domainPromotions.send(merged)
            self.currentPage = page
            self.canLoadMoreSubject.send(!isLastPage)

            // Lưu cache cho tab vừa tải để lần sau quay lại hiện ngay.
            self.tabCaches[tab] = TabCache(promotions: merged, page: page, isLastPage: isLastPage)
        }
    }
}
