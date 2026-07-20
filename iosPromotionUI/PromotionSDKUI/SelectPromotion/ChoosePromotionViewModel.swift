//
//  ChoosePromotionViewModel.swift
//  PromotionSDK
//
//  Created by thachlh on 14/5/26.
//

import Foundation
import UIKit
@_implementationOnly import PRMFoundation
@_implementationOnly import RxSwift
@_implementationOnly import RxCocoa
@_implementationOnly import PRMKotlinBridge

final class ChoosePromotionViewModel: BaseViewModel<ChoosePromotionRouter>, ViewModelType {

    enum SectionType: String {
        case myPromotions = "myPromotions"
        case otherPromotions = "otherPromotions"
    }

    /// Số "Ưu đãi của tôi" hiển thị ban đầu trước khi bấm "Xem thêm" (khớp Android COLLAPSED_COUNT = 2).
    private static let myPromotionsInitialVisibleCount = 2
    /// Kích thước trang khi gọi API load thêm.
    private static let pageSize = 10

    struct Input {
        let searchText: Observable<String>
        let toggleSelectionRelay: PublishRelay<String>
        /// Nút "Xem thêm" của "Ưu đãi của tôi".
        let seeMoreMyRelay: PublishRelay<Void>
        /// Cuộn tới đáy "Ưu đãi khác".
        let loadMoreOtherRelay: PublishRelay<Void>
    }

    /// Trạng thái nút cuối section "Ưu đãi của tôi".
    enum SeeMoreState {
        case none      // không hiện nút
        case expand    // "Xem thêm"
        case collapse  // "Thu gọn" (đã hiện hết)
    }

    struct PromotionSection {
        let type: SectionType
        let title: String
        let items: [MyPromotionCellViewModel]
        let totalCount: Int
        /// Trạng thái nút "Xem thêm/Thu gọn" (chỉ "Ưu đãi của tôi").
        let seeMoreState: SeeMoreState
    }

    struct Output {
        let sections: Driver<[PromotionSection]>
        let hasSelection: Driver<Bool>
        let selectedPromotion: Driver<EligibleOffer?>
        /// Đang tải trang đầu (để hiện shimmer).
        let isLoading: Driver<Bool>
    }

    /// State phân trang tích luỹ cho 2 list.
    private struct PagingState {
        var myLoaded: [EligibleOffer]
        var otherLoaded: [EligibleOffer]
        var myPage: Int
        var otherPage: Int
        var myIsLastPage: Bool
        var otherIsLastPage: Bool
        /// Số item "Ưu đãi của tôi" đang hiện (tăng dần theo "Xem thêm").
        var myVisibleCount: Int
    }

    /// Debounce search theo ký tự (giống màn Search Android).
    private static let searchDebounceMs = 400

    let data: ChoosePromotionBuilder.DataModel
    private let findEligibleUseCase: FindEligibleCampaignsUseCase
    private(set) var input: Input!

    private let stateRelay: BehaviorRelay<PagingState>
    private let isFetchingMy = BehaviorRelay<Bool>(value: false)
    private let isFetchingOther = BehaviorRelay<Bool>(value: false)
    /// Đang tải trang đầu (bắt đầu true → hiện shimmer ngay khi mở màn).
    private let isLoadingRelay = BehaviorRelay<Bool>(value: true)
    private var didLoadInitial = false
    /// Keyword đang search (lọc CLIENT-SIDE trên data đã tải — Eligible API không có param keyword).
    private let keywordRelay = BehaviorRelay<String>(value: "")

    init(router: ChoosePromotionRouter,
         data: ChoosePromotionBuilder.DataModel,
         findEligibleUseCase: FindEligibleCampaignsUseCase = FindEligibleCampaignsUseCase()) {
        self.data = data
        self.findEligibleUseCase = findEligibleUseCase
        // Seed bằng data preload từ widget (nếu có) — tránh double call. Rỗng → fetch khi transform.
        self.stateRelay = BehaviorRelay(value: PagingState(
            myLoaded: data.preloadedMy,
            otherLoaded: data.preloadedOther,
            myPage: 0,
            otherPage: 0,
            myIsLastPage: data.myIsLastPage,
            otherIsLastPage: data.otherIsLastPage,
            myVisibleCount: Self.myPromotionsInitialVisibleCount
        ))
        super.init(router: router)
    }

    func transform(input: Input) -> Output {
        self.input = input
        loadInitialPage()

        // Search theo ký tự (debounce) → lọc CLIENT-SIDE trên data đã tải (Eligible API không có keyword).
        input.searchText
            .skip(1)
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .distinctUntilChanged()
            .debounce(.milliseconds(Self.searchDebounceMs), scheduler: MainScheduler.instance)
            .bind(to: keywordRelay)
            .disposed(by: disposeBag)

        // Pre-select voucher đang áp (nếu có) — khớp Android `preSelectedVoucherIds`.
        let preSelectedId = data.preSelectedVoucherId
        let selectedPromotionId = input.toggleSelectionRelay
            .scan(preSelectedId) { current, id in
                current == id ? nil : id
            }
            .startWith(preSelectedId)
            .share(replay: 1)

        // Nút cuối "Ưu đãi của tôi": lộ thêm data đã tải -> gọi page kế -> khi hết thì "Thu gọn".
        input.seeMoreMyRelay
            .subscribe(onNext: { [weak self] in
                guard let self = self else { return }
                var st = self.stateRelay.value
                if st.myVisibleCount < st.myLoaded.count {
                    st.myVisibleCount = st.myLoaded.count
                    self.stateRelay.accept(st)
                } else if !st.myIsLastPage {
                    self.fetchNextMyPage()
                } else {
                    // Đã hiện hết tất cả -> thu gọn về số ban đầu.
                    st.myVisibleCount = Self.myPromotionsInitialVisibleCount
                    self.stateRelay.accept(st)
                }
            })
            .disposed(by: disposeBag)

        // Cuộn tới đáy "Ưu đãi khác": gọi page kế (giữ keyword hiện tại; VM tự bỏ nếu hết trang/đang tải).
        input.loadMoreOtherRelay
            .subscribe(onNext: { [weak self] in
                self?.fetchNextOtherPage()
            })
            .disposed(by: disposeBag)

        // Sections: data từ state (server), lọc CLIENT-SIDE theo keyword (nếu có) qua SearchPromotionUseCase.
        let sectionsDriver = Observable.combineLatest(
            selectedPromotionId,
            stateRelay.asObservable(),
            keywordRelay.asObservable()
        )
        .map { [weak self] (selectedId, st, keyword) -> [PromotionSection] in
            guard let self = self else { return [] }
            let isSearching = !keyword.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            let filteredMy = isSearching ? OfferSearchFilter.search(query: keyword, in: st.myLoaded) : st.myLoaded
            let filteredOther = isSearching ? OfferSearchFilter.search(query: keyword, in: st.otherLoaded) : st.otherLoaded

            let mapToViewModel: (EligibleOffer) -> MyPromotionCellViewModel = { entity in
                MyPromotionCellViewModel(
                    offer: entity,
                    buttonTitle: "Chi tiết",
                    showsCheckbox: true,
                    isChecked: selectedId == entity.id,
                    checkedImage: UIImage.sdk("prm_ic_circle_check"),
                    uncheckedImage: UIImage.sdk("prm_ic_circle_uncheck"),
                    highlightKeyword: isSearching ? keyword : nil
                )
            }

            var sections: [PromotionSection] = []

            if !filteredMy.isEmpty {
                // Khi search: hiện hết kết quả khớp, ẩn "Xem thêm", không phân trang.
                let visibleItems = isSearching ? filteredMy : Array(filteredMy.prefix(st.myVisibleCount))
                let seeMoreState: SeeMoreState
                if isSearching {
                    seeMoreState = .none
                } else if st.myVisibleCount < filteredMy.count || !st.myIsLastPage {
                    seeMoreState = .expand
                } else if filteredMy.count > Self.myPromotionsInitialVisibleCount {
                    seeMoreState = .collapse
                } else {
                    seeMoreState = .none
                }
                sections.append(PromotionSection(
                    type: .myPromotions,
                    title: "Ưu đãi của tôi",
                    items: visibleItems.map(mapToViewModel),
                    totalCount: filteredMy.count,
                    seeMoreState: seeMoreState
                ))
            }

            if !filteredOther.isEmpty {
                sections.append(PromotionSection(
                    type: .otherPromotions,
                    title: "Ưu đãi khác",
                    items: filteredOther.map(mapToViewModel),
                    totalCount: filteredOther.count,
                    seeMoreState: .none
                ))
            }

            return sections
        }
        .asDriver(onErrorJustReturn: [])

        let hasSelection = selectedPromotionId
            .map { $0 != nil }
            .asDriver(onErrorJustReturn: false)

        let selectedPromotion = selectedPromotionId
            .map { [weak self] id -> EligibleOffer? in
                guard let self = self, let id = id else { return nil }
                return self.allLoadedPromotions().first { $0.id == id }
            }
            .asDriver(onErrorJustReturn: nil)

        return Output(
            sections: sectionsDriver,
            hasSelection: hasSelection,
            selectedPromotion: selectedPromotion,
            isLoading: isLoadingRelay.asDriver()
        )
    }

    // MARK: - Initial load (trang 0: my + other) — màn tự fetch khi xuất hiện

    private func loadInitialPage() {
        guard !didLoadInitial else { return }
        didLoadInitial = true
        // Đã có data preload từ widget → dùng ngay, KHÔNG gọi API, KHÔNG shimmer (giống Android PreloadVouchers).
        let st = stateRelay.value
        if !st.myLoaded.isEmpty || !st.otherLoaded.isEmpty {
            isLoadingRelay.accept(false)
            return
        }
        fetchFirstPage()
    }

    /// Tạo input Eligible từ order data của màn. section=nil → cả 2 nhóm.
    /// Token do host cấp qua `PromotionRequestContextProvider` của lõi, không gửi từng request.
    private func makeEligibleInput(section: EligibleSection?, myPage: Int, otherPage: Int) -> FindEligibleCampaignsRequest {
        FindEligibleCampaignsRequest(
            customerId: data.customerId,
            orderId: data.orderId ?? "",
            orderValue: data.orderValue ?? "0",
            items: data.orderItems,
            currency: "VND",
            channel: "MOBILE",
            customerType: nil,
            segment: nil,
            tier: nil,
            tabCode: nil,
            section: section,
            myPage: Int32(myPage),
            mySize: Int32(Self.pageSize),
            otherPage: Int32(otherPage),
            otherSize: Int32(Self.pageSize),
            filterOptions: EligibleFilterOptions(includeExpired: false, checkBudgetAvailability: true, includePreview: true)
        )
    }

    /// Gọi Find Eligible Campaigns qua adapter Rx.
    private func findEligible(section: EligibleSection?, myPage: Int, otherPage: Int) -> Single<EligibleOffersResult?> {
        let request = makeEligibleInput(section: section, myPage: myPage, otherPage: otherPage)
        let useCase = findEligibleUseCase
        return singleFromKotlin { try await useCase.invoke(request: request) }
    }

    /// Fetch trang 0 (my + other) qua Eligible API, reset state.
    private func fetchFirstPage() {
        isLoadingRelay.accept(true)
        findEligible(section: nil, myPage: 0, otherPage: 0)
            .observeOn(MainScheduler.instance)
            .subscribe(
                onSuccess: { [weak self] model in
                    guard let self = self, let model else { return }
                    var st = self.stateRelay.value
                    st.myLoaded = model.myOffers
                    st.otherLoaded = model.otherOffers
                    st.myPage = 0
                    st.otherPage = 0
                    st.myIsLastPage = model.myIsLastPage
                    st.otherIsLastPage = model.otherIsLastPage
                    st.myVisibleCount = Self.myPromotionsInitialVisibleCount
                    self.stateRelay.accept(st)
                    self.isLoadingRelay.accept(false)
                },
                onError: { [weak self] _ in
                    self?.isLoadingRelay.accept(false)
                }
            )
            .disposed(by: disposeBag)
    }

    // MARK: - Pagination (load-more độc lập từng nhóm qua sectionCode)

    private func fetchNextMyPage() {
        let st = stateRelay.value
        guard !st.myIsLastPage, !isFetchingMy.value else { return }
        isFetchingMy.accept(true)
        let nextPage = st.myPage + 1
        // Chỉ load-more nhóm myOffers (sectionCode=my_offers) → otherOffers trả null, bỏ qua.
        findEligible(section: EligibleSection.myOffers, myPage: nextPage, otherPage: st.otherPage)
            .observeOn(MainScheduler.instance)
            .subscribe(
                onSuccess: { [weak self] model in
                    guard let self = self, let model else { return }
                    var st = self.stateRelay.value
                    st.myLoaded += model.myOffers
                    st.myPage = nextPage
                    st.myIsLastPage = model.myIsLastPage
                    st.myVisibleCount = st.myLoaded.count
                    self.stateRelay.accept(st)
                    self.isFetchingMy.accept(false)
                },
                onError: { [weak self] _ in self?.isFetchingMy.accept(false) }
            )
            .disposed(by: disposeBag)
    }

    private func fetchNextOtherPage() {
        let st = stateRelay.value
        guard !st.otherIsLastPage, !isFetchingOther.value else { return }
        isFetchingOther.accept(true)
        let nextPage = st.otherPage + 1
        // Chỉ load-more nhóm otherOffers (sectionCode=other_offers) → myOffers trả null, bỏ qua.
        findEligible(section: EligibleSection.otherOffers, myPage: st.myPage, otherPage: nextPage)
            .observeOn(MainScheduler.instance)
            .subscribe(
                onSuccess: { [weak self] model in
                    guard let self = self, let model else { return }
                    var st = self.stateRelay.value
                    st.otherLoaded += model.otherOffers
                    st.otherPage = nextPage
                    st.otherIsLastPage = model.otherIsLastPage
                    self.stateRelay.accept(st)
                    self.isFetchingOther.accept(false)
                },
                onError: { [weak self] _ in self?.isFetchingOther.accept(false) }
            )
            .disposed(by: disposeBag)
    }

    private func allLoadedPromotions() -> [EligibleOffer] {
        let st = stateRelay.value
        return st.myLoaded + st.otherLoaded
    }

    func routeToDetail(id: String) {
        if let promotion = allLoadedPromotions().first(where: { $0.id == id }) {
            router.routeToDetail(promotion: promotion, customerId: data.customerId, token: data.token)
        }
    }
}
