//
//  MyPromotionViewModel.swift
//  PromotionSDK
//
//  Created by thachlh on 6/5/26.
//

import Foundation
@_implementationOnly import RxSwift
@_implementationOnly import RxCocoa
@_implementationOnly import PRMKotlinBridge

final class MyPromotionViewModel: PRMBaseViewModel<MyPromotionRouter>, PRMViewModelType {
    
    struct Input {
        let refreshTrigger: Observable<Void>
        let loadMoreTrigger: Observable<Void>
        let selectPromotionByIDRelay: PublishRelay<String>
        /// Code tab được chọn (tabs động lấy từ API).
        let selectTabRelay: PublishRelay<String>
    }

    struct Output {
        let isRefreshing: Driver<Bool>
        let isLoadingMore: Driver<Bool>
        let isLoading: Driver<Bool>
        let promotions: Driver<[MyPromotionCellViewModel]>
        /// Tabs động từ API (code/label/count, đã sort).
        let tabs: Driver<[VoucherTabItem]>
        let selectedTabCode: Driver<String>
        /// Còn trang để load thêm không — feed cho `PRMRefreshTableView.isHasMorePage`.
        let canLoadMore: Driver<Bool>
        /// List rỗng (đã tải xong, không có voucher) → hiện empty view (khớp Android ctlNoResult).
        let isEmpty: Driver<Bool>
        /// Thông báo lỗi fetch → hiện toast (khớp Android ShowError).
        let errorMessage: Signal<String>
    }

    let data: MyPromotionBuilder.DataModel
    private(set) var input: Input!
    private let domainPromotions = BehaviorRelay<[VoucherItem]>(value: [])
    /// Code tab đang chọn (mặc định "all"; tabs về từ API sẽ render động).
    private let selectedTabCode = BehaviorRelay<String>(value: "all")
    private let tabsRelay = BehaviorRelay<[VoucherTabItem]>(value: [])
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
        let isRefreshing = PublishRelay<Bool>()
        let isLoadingMore = PublishRelay<Bool>()
        let isLoading = BehaviorRelay<Bool>(value: true)
        let errorRelay = PublishRelay<String>()

        let currentPage = BehaviorRelay<Int>(value: 0)
        let canLoadMore = BehaviorRelay<Bool>(value: true)
        // Cờ "đang có request chạy" — chặn load-more chồng (khớp Android guard isLoading/isLoadingMore/...).
        let isFetching = BehaviorRelay<Bool>(value: false)

        // Yêu cầu tải: (page, silent). silent = refresh ngầm khi tab đã có cache (không hiện spinner/skeleton).
        let fetchTrigger = PublishRelay<(page: Int, silent: Bool)>()

        // Đổi tab: tab đã có cache -> hiện ngay từ cache rồi refresh ngầm; chưa cache -> giữ list cũ + tải mới.
        input.selectTabRelay
            .subscribe(onNext: { [weak self] code in
                guard let self = self, code != self.selectedTabCode.value else { return }
                self.selectedTabCode.accept(code)
                if let cache = self.tabCaches[code] {
                    self.domainPromotions.accept(cache.promotions)
                    currentPage.accept(cache.page)
                    canLoadMore.accept(!cache.isLastPage)
                    fetchTrigger.accept((0, true))
                } else {
                    fetchTrigger.accept((0, false))
                }
            })
            .disposed(by: disposeBag)

        input.refreshTrigger
            .subscribe(onNext: { _ in fetchTrigger.accept((0, false)) })
            .disposed(by: disposeBag)

        input.loadMoreTrigger
            .withLatestFrom(currentPage)
            // Bỏ qua khi đang có request chạy hoặc hết trang → không stack load-more, không rớt trang (khớp Android).
            .filter { _ in !isFetching.value && canLoadMore.value }
            .subscribe(onNext: { page in fetchTrigger.accept((page + 1, false)) })
            .disposed(by: disposeBag)

        fetchTrigger.asObservable()
            .startWith((0, false))   // tải lần đầu khi mở màn
            .filter { (page, _) in page == 0 || canLoadMore.value }
            .do(onNext: { [weak self] (page, silent) in
                guard let self = self else { return }
                isFetching.accept(true)
                if page > 0 {
                    isLoadingMore.accept(true)
                } else if silent {
                    // refresh ngầm tab đã cache: không spinner, không skeleton.
                } else if self.domainPromotions.value.isEmpty {
                    isLoading.accept(true)       // skeleton chỉ khi list trống (khớp Android)
                } else {
                    isRefreshing.accept(true)    // đã có list -> spinner refresh
                }
            })
            .flatMapLatest { [weak self] (page, _) -> Observable<(Int, String, SearchCustomerVouchersResult?)> in
                guard let self = self else { return .empty() }
                let tab = self.selectedTabCode.value
                // Thiếu customerId → không gọi mạng, đẩy nil để subscribe tắt spinner + báo toast riêng (khớp Android MISSING_CUSTOMER_ID).
                guard !self.data.customerId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
                    return .just((page, tab, nil))
                }
                // Token do host cấp qua `PromotionRequestContextProvider` của lõi, không gửi từng request.
                let request = SearchCustomerVouchersRequest(
                    customerId: self.data.customerId,
                    keyword: nil,
                    serviceCode: nil,
                    tab: tab,
                    page: boxed(page),
                    size: boxed(10)
                )
                let useCase = self.searchVouchersUseCase
                return singleFromKotlin { try await useCase.invoke(request: request) }
                    .asObservable()
                    .map { (page, tab, $0) }
                    .catchErrorJustReturn((page, tab, nil))
            }
            .subscribe(onNext: { [weak self] (page, tab, listModel) in
                guard let self = self else { return }

                isRefreshing.accept(false)
                isLoadingMore.accept(false)
                isLoading.accept(false)
                isFetching.accept(false)

                // Chống đè nhầm: bỏ qua response của tab không còn được chọn (khớp Android shouldApplyResponse).
                guard tab == self.selectedTabCode.value else { return }
                // listModel nil = fetch lỗi / thiếu customerId → báo toast, không đè list (khớp Android ShowError).
                guard let listModel = listModel else {
                    let missingCustomer = self.data.customerId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                    errorRelay.accept(missingCustomer ? "Thiếu thông tin khách hàng." : "Đã có lỗi xảy ra. Vui lòng thử lại.")
                    return
                }

                if !listModel.tabs.isEmpty {
                    self.tabsRelay.accept(listModel.tabs)
                }

                let isLastPage = listModel.last?.boolValue ?? true
                let merged: [VoucherItem]
                if page == 0 {
                    merged = listModel.content
                } else {
                    merged = self.domainPromotions.value + listModel.content
                }
                self.domainPromotions.accept(merged)
                currentPage.accept(page)
                canLoadMore.accept(!isLastPage)

                // Lưu cache cho tab vừa tải để lần sau quay lại hiện ngay.
                self.tabCaches[tab] = TabCache(promotions: merged, page: page, isLastPage: isLastPage)
            })
            .disposed(by: disposeBag)
        
        // Mở Detail bằng promotion cơ bản; màn Detail tự fetch detail đầy đủ (giống Android).
        input.selectPromotionByIDRelay
            .subscribe(onNext: { [weak self] id in
                guard let self = self else { return }
                if let promotion = self.domainPromotions.value.first(where: { $0.voucherId == id }) {
                    self.router.routeToDetail(promotion: promotion, customerId: self.data.customerId, token: self.data.token)
                }
            })
            .disposed(by: disposeBag)
        
        let mappedPromotions = domainPromotions.map { models in
            models.map { MyPromotionCellViewModel(voucher: $0) }
        }.asDriver(onErrorJustReturn: [])

        // Empty: đã tải xong (không skeleton) và list rỗng (khớp Android !isLoading && isEmpty).
        let isEmptyDriver = Observable.combineLatest(
            domainPromotions.asObservable(),
            isLoading.asObservable()
        )
        .map { promotions, loading in !loading && promotions.isEmpty }
        .asDriver(onErrorJustReturn: false)

        return Output(
            isRefreshing: isRefreshing.asDriver(onErrorJustReturn: false),
            isLoadingMore: isLoadingMore.asDriver(onErrorJustReturn: false),
            isLoading: isLoading.asDriver(onErrorJustReturn: false),
            promotions: mappedPromotions,
            tabs: tabsRelay.asDriver(onErrorJustReturn: []),
            selectedTabCode: selectedTabCode.asDriver(onErrorJustReturn: "all"),
            canLoadMore: canLoadMore.asDriver(onErrorJustReturn: false),
            isEmpty: isEmptyDriver,
            errorMessage: errorRelay.asSignal()
        )
    }
}
