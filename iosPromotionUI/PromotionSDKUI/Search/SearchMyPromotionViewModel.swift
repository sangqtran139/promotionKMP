//
//  SearchMyPromotionViewModel.swift
//  PromotionSDK
//
//  Created by thachlh on 11/5/26.
//

import Foundation
import Combine
@_implementationOnly import PRMKotlinBridge

private let minKeywordLength = 1
private let debounceMs = 400

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
    /// Context (customerId/token) đọc từ lõi — đối xứng Android, không threading qua DataModel.
    private var requestContext: PromotionRequestContextProvider { PromotionContainer.shared.requestContextProvider }
    private(set) var input: Input!
    private let searchVouchersUseCase: SearchCustomerVouchersUseCase

    // MARK: - State
    /// Gương của `input.searchText` — giữ giá trị hiện tại cho `withLatestFrom`/load-more.
    private let searchTextSubject = CurrentValueSubject<String, Never>("")
    private let accumulatedPromotions = CurrentValueSubject<[VoucherItem], Never>([])
    /// Keyword của lần search hiện hành — dùng highlight title item (khớp Android).
    private let searchedKeyword = CurrentValueSubject<String, Never>("")
    private let isLoadingSubject = CurrentValueSubject<Bool, Never>(false)
    private let isLoadingMoreSubject = CurrentValueSubject<Bool, Never>(false)
    private let validationErrorSubject = CurrentValueSubject<String?, Never>(nil)
    private var currentPage = 0
    private var canLoadMore = false
    /// Token chống race: mỗi lần search tăng 1; response mang token cũ bị bỏ qua (khớp Android cancel job).
    private var latestSearchToken = 0

    init(router: SearchMyPromotionRouter,
         data: SearchMyPromotionBuilder.DataModel,
         searchVouchersUseCase: SearchCustomerVouchersUseCase = SearchCustomerVouchersUseCase()) {
        self.data = data
        self.searchVouchersUseCase = searchVouchersUseCase
        super.init(router: router)
    }

    func transform(input: Input) -> Output {
        self.input = input

        // Gương text vào subject để load-more / withLatestFrom đọc giá trị mới nhất.
        input.searchText
            .sink { [weak self] in self?.searchTextSubject.send($0) }
            .store(in: &cancellables)

        // Keyword đã debounce (thay `.debounce().distinctUntilChanged()`).
        let debounced = searchTextSubject
            .debounce(for: .milliseconds(debounceMs), scheduler: DispatchQueue.main)
            .removeDuplicates()
            .eraseToAnyPublisher()

        // Bấm search → dùng keyword hiện tại (thay `searchAction.withLatestFrom(searchText)`).
        let actionKeyword = input.searchAction
            .map { [weak self] in self?.searchTextSubject.value ?? "" }
            .eraseToAnyPublisher()

        // Trigger search khi debounce xong hoặc bấm nút search.
        debounced.merge(with: actionKeyword)
            .sink { [weak self] keyword in self?.handleSearchTrigger(keyword) }
            .store(in: &cancellables)

        // Bật loading NGAY khi gõ (stream raw, trước debounce) → shimmer che ngay, tránh nhấp nháy
        // "không có kết quả" trong ~400ms chờ debounce. Rỗng → tắt loading.
        input.searchText
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .removeDuplicates()
            .sink { [weak self] trimmed in
                guard let self = self else { return }
                if trimmed.isEmpty {
                    self.isLoadingSubject.send(false)
                } else if trimmed.count >= minKeywordLength {
                    self.isLoadingSubject.send(true)
                }
            }
            .store(in: &cancellables)

        // Load more
        input.loadMoreTrigger
            .sink { [weak self] in
                guard let self = self else { return }
                let trimmed = self.searchTextSubject.value.trimmingCharacters(in: .whitespacesAndNewlines)
                guard trimmed.count >= minKeywordLength,
                      self.canLoadMore,
                      !self.isLoadingSubject.value,
                      !self.isLoadingMoreSubject.value else { return }
                self.performSearch(keyword: trimmed, page: self.currentPage + 1, isRefresh: false)
            }
            .store(in: &cancellables)

        // Navigate to detail
        input.selectPromotionByIDRelay
            .sink { [weak self] id in
                guard let self = self else { return }
                if let promotion = self.accumulatedPromotions.value.first(where: { $0.voucherId == id }) {
                    self.router.routeToDetail(promotion: promotion)
                }
            }
            .store(in: &cancellables)

        let promotions = Publishers.CombineLatest(accumulatedPromotions, searchedKeyword)
            .map { models, keyword in
                models.map { MyPromotionCellViewModel(voucher: $0, highlightKeyword: keyword) }
            }
            .receive(on: DispatchQueue.main)
            .eraseToAnyPublisher()

        let isEmpty = Publishers.CombineLatest3(accumulatedPromotions, isLoadingSubject, searchTextSubject)
            .map { promotions, loading, keyword -> Bool in
                let trimmed = keyword.trimmingCharacters(in: .whitespacesAndNewlines)
                return !loading && !trimmed.isEmpty && trimmed.count >= minKeywordLength && promotions.isEmpty
            }
            .receive(on: DispatchQueue.main)
            .eraseToAnyPublisher()

        return Output(
            promotions: promotions,
            isLoading: isLoadingSubject.receive(on: DispatchQueue.main).eraseToAnyPublisher(),
            isLoadingMore: isLoadingMoreSubject.receive(on: DispatchQueue.main).eraseToAnyPublisher(),
            isEmpty: isEmpty,
            validationError: validationErrorSubject.receive(on: DispatchQueue.main).eraseToAnyPublisher()
        )
    }

    /// Xử lý một lần trigger search (thay body `searchTrigger.subscribe(onNext:)`).
    private func handleSearchTrigger(_ keyword: String) {
        let trimmed = keyword.trimmingCharacters(in: .whitespacesAndNewlines)

        if trimmed.isEmpty {
            accumulatedPromotions.send([])
            searchedKeyword.send("")
            currentPage = 0
            canLoadMore = false
            validationErrorSubject.send(nil)
            return
        }

        if trimmed.count < minKeywordLength {
            accumulatedPromotions.send([])
            validationErrorSubject.send("Keyword too short")
            return
        }

        validationErrorSubject.send(nil)
        performSearch(keyword: trimmed, page: 0, isRefresh: true)
    }

    private func performSearch(keyword: String, page: Int, isRefresh: Bool) {
        latestSearchToken += 1
        let token = latestSearchToken
        searchedKeyword.send(keyword)

        if isRefresh {
            isLoadingSubject.send(true)
            // Search mới hủy hiệu lực load-more đang chờ → tắt spinner đáy để response cũ bị bỏ qua không kẹt.
            isLoadingMoreSubject.send(false)
        } else {
            isLoadingMoreSubject.send(true)
        }

        // Token do host cấp qua `PromotionRequestContextProvider` của lõi.
        let request = SearchCustomerVouchersRequest(
            customerId: requestContext.getCustomerId() ?? "",
            keyword: keyword,
            serviceCode: nil,
            tab: "all",
            page: boxed(page),
            size: boxed(10)
        )

        let useCase = searchVouchersUseCase
        Task { @MainActor [weak self] in
            let listModel: SearchCustomerVouchersResult?
            do {
                listModel = try await useCase.invoke(request: request)
            } catch {
                // Bỏ qua response của search cũ (đã có search mới hơn).
                guard let self = self, token == self.latestSearchToken else { return }
                self.isLoadingSubject.send(false)
                self.isLoadingMoreSubject.send(false)
                return
            }

            guard let self = self, token == self.latestSearchToken else { return }
            self.isLoadingSubject.send(false)
            self.isLoadingMoreSubject.send(false)
            guard let listModel else { return }
            if isRefresh {
                self.accumulatedPromotions.send(listModel.content)
            } else {
                self.accumulatedPromotions.send(self.accumulatedPromotions.value + listModel.content)
            }
            self.currentPage = page
            self.canLoadMore = !(listModel.last?.boolValue ?? true)
        }
    }
}
