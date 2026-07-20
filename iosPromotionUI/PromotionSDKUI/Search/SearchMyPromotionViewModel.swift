//
//  SearchMyPromotionViewModel.swift
//  PromotionSDK
//
//  Created by thachlh on 11/5/26.
//

import Foundation
@_implementationOnly import RxSwift
@_implementationOnly import RxCocoa
@_implementationOnly import PRMKotlinBridge

private let minKeywordLength = 1
private let debounceMs: RxTimeInterval = .milliseconds(400)

final class SearchMyPromotionViewModel: BaseViewModel<SearchMyPromotionRouter>, ViewModelType {

    struct Input {
        let searchText: Observable<String>
        let searchAction: Observable<Void>
        let loadMoreTrigger: Observable<Void>
        let selectPromotionByIDRelay: PublishRelay<String>
    }

    struct Output {
        let promotions: Driver<[MyPromotionCellViewModel]>
        let isLoading: Driver<Bool>
        let isLoadingMore: Driver<Bool>
        let isEmpty: Driver<Bool>
        let validationError: Driver<String?>
    }

    let data: SearchMyPromotionBuilder.DataModel
    private(set) var input: Input!
    private let searchVouchersUseCase: SearchCustomerVouchersUseCase
    private let currentPage = BehaviorRelay<Int>(value: 0)
    private let canLoadMore = BehaviorRelay<Bool>(value: false)
    private let accumulatedPromotions = BehaviorRelay<[VoucherItem]>(value: [])
    /// Keyword của lần search hiện hành — dùng highlight title item (khớp Android).
    private let searchedKeyword = BehaviorRelay<String>(value: "")
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

        let isLoading = BehaviorRelay<Bool>(value: false)
        let isLoadingMore = BehaviorRelay<Bool>(value: false)
        let validationError = BehaviorRelay<String?>(value: nil)

        // Search text stream with debounce
        let keywordStream = input.searchText
            .debounce(debounceMs, scheduler: MainScheduler.instance)
            .distinctUntilChanged()
            .share(replay: 1)

        // Trigger search on debounce or explicit search action
        let searchTrigger = Observable.merge(
            keywordStream,
            input.searchAction.withLatestFrom(input.searchText)
        )

        searchTrigger
            .subscribe(onNext: { [weak self] keyword in
                guard let self = self else { return }
                let trimmed = keyword.trimmingCharacters(in: .whitespacesAndNewlines)

                if trimmed.isEmpty {
                    self.accumulatedPromotions.accept([])
                    self.searchedKeyword.accept("")
                    self.currentPage.accept(0)
                    self.canLoadMore.accept(false)
                    validationError.accept(nil)
                    return
                }

                if trimmed.count < minKeywordLength {
                    self.accumulatedPromotions.accept([])
                    validationError.accept("Keyword too short")
                    return
                }

                validationError.accept(nil)
                self.performSearch(keyword: trimmed, page: 0, isRefresh: true,
                                   isLoading: isLoading, isLoadingMore: isLoadingMore)
            })
            .disposed(by: disposeBag)

        // Bật loading NGAY khi gõ (stream raw, trước debounce) → shimmer che ngay, tránh nhấp nháy
        // "không có kết quả" trong ~400ms chờ debounce. Rỗng → tắt loading.
        input.searchText
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .distinctUntilChanged()
            .subscribe(onNext: { trimmed in
                if trimmed.isEmpty {
                    isLoading.accept(false)
                } else if trimmed.count >= minKeywordLength {
                    isLoading.accept(true)
                }
            })
            .disposed(by: disposeBag)

        // Load more
        input.loadMoreTrigger
            .withLatestFrom(input.searchText)
            .subscribe(onNext: { [weak self] keyword in
                guard let self = self else { return }
                let trimmed = keyword.trimmingCharacters(in: .whitespacesAndNewlines)
                guard trimmed.count >= minKeywordLength,
                      self.canLoadMore.value,
                      !isLoading.value,
                      !isLoadingMore.value else { return }
                let nextPage = self.currentPage.value + 1
                self.performSearch(keyword: trimmed, page: nextPage, isRefresh: false,
                                   isLoading: isLoading, isLoadingMore: isLoadingMore)
            })
            .disposed(by: disposeBag)

        // Navigate to detail
        input.selectPromotionByIDRelay
            .subscribe(onNext: { [weak self] id in
                guard let self = self else { return }
                if let promotion = self.accumulatedPromotions.value.first(where: { $0.voucherId == id }) {
                    self.router.routeToDetail(promotion: promotion, customerId: self.data.customerId, token: self.data.token)
                }
            })
            .disposed(by: disposeBag)

        let promotionsDriver = Observable
            .combineLatest(accumulatedPromotions, searchedKeyword)
            .map { models, keyword in
                models.map { MyPromotionCellViewModel(voucher: $0, highlightKeyword: keyword) }
            }
            .asDriver(onErrorJustReturn: [])

        let isEmptyDriver = Observable.combineLatest(
            accumulatedPromotions,
            isLoading.asObservable(),
            input.searchText
        )
        .map { promotions, loading, keyword -> Bool in
            let trimmed = keyword.trimmingCharacters(in: .whitespacesAndNewlines)
            return !loading && !trimmed.isEmpty && trimmed.count >= minKeywordLength && promotions.isEmpty
        }
        .asDriver(onErrorJustReturn: false)

        return Output(
            promotions: promotionsDriver,
            isLoading: isLoading.asDriver(onErrorJustReturn: false),
            isLoadingMore: isLoadingMore.asDriver(onErrorJustReturn: false),
            isEmpty: isEmptyDriver,
            validationError: validationError.asDriver(onErrorJustReturn: nil)
        )
    }

    private func performSearch(keyword: String, page: Int, isRefresh: Bool,
                               isLoading: BehaviorRelay<Bool>,
                               isLoadingMore: BehaviorRelay<Bool>) {
        latestSearchToken += 1
        let token = latestSearchToken
        searchedKeyword.accept(keyword)

        if isRefresh {
            isLoading.accept(true)
            // Search mới hủy hiệu lực load-more đang chờ → tắt spinner đáy để response cũ bị bỏ qua không kẹt.
            isLoadingMore.accept(false)
        } else {
            isLoadingMore.accept(true)
        }

        // Token do host cấp qua `PromotionRequestContextProvider` của lõi.
        let request = SearchCustomerVouchersRequest(
            customerId: data.customerId,
            keyword: keyword,
            serviceCode: nil,
            tab: "all",
            page: boxed(page),
            size: boxed(10)
        )

        let useCase = searchVouchersUseCase
        singleFromKotlin { try await useCase.invoke(request: request) }
            .observeOn(MainScheduler.instance)
            .subscribe(
                onSuccess: { [weak self] listModel in
                    // Bỏ qua response của search cũ (đã có search mới hơn).
                    guard let self = self, token == self.latestSearchToken else { return }
                    isLoading.accept(false)
                    isLoadingMore.accept(false)
                    guard let listModel else { return }
                    if isRefresh {
                        self.accumulatedPromotions.accept(listModel.content)
                    } else {
                        self.accumulatedPromotions.accept(self.accumulatedPromotions.value + listModel.content)
                    }
                    self.currentPage.accept(page)
                    self.canLoadMore.accept(!(listModel.last?.boolValue ?? true))
                },
                onError: { [weak self] _ in
                    guard let self = self, token == self.latestSearchToken else { return }
                    isLoading.accept(false)
                    isLoadingMore.accept(false)
                }
            )
            .disposed(by: disposeBag)
    }
}
