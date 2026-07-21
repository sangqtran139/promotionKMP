//
//  ChoosePromotionViewModel.swift
//  PromotionSDK
//
//  Created by thachlh on 14/5/26.
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
    /// Kích thước trang khi gọi API load thêm.
    private static let pageSize = 10

    struct Input {
        let searchText: AnyPublisher<String, Never>
        let toggleSelectionRelay: PassthroughSubject<String, Never>
        /// Nút "Xem thêm" của "Ưu đãi của tôi".
        let seeMoreMyRelay: PassthroughSubject<Void, Never>
        /// Cuộn tới đáy "Ưu đãi khác".
        let loadMoreOtherRelay: PassthroughSubject<Void, Never>
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
        let sections: AnyPublisher<[PromotionSection], Never>
        let hasSelection: AnyPublisher<Bool, Never>
        let selectedPromotion: AnyPublisher<EligibleOffer?, Never>
        /// Đang tải trang đầu (để hiện shimmer).
        let isLoading: AnyPublisher<Bool, Never>
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

    // MARK: - State
    private let stateSubject: CurrentValueSubject<PagingState, Never>
    private let isFetchingMy = CurrentValueSubject<Bool, Never>(false)
    private let isFetchingOther = CurrentValueSubject<Bool, Never>(false)
    /// Đang tải trang đầu (bắt đầu true → hiện shimmer ngay khi mở màn).
    private let isLoadingSubject = CurrentValueSubject<Bool, Never>(true)
    private var didLoadInitial = false
    /// Keyword đang search (lọc CLIENT-SIDE trên data đã tải — Eligible API không có param keyword).
    private let keywordSubject = CurrentValueSubject<String, Never>("")
    /// Voucher đang chọn; seed = voucher đang áp (trùng id đang chọn → bỏ chọn).
    private let selectedPromotionSubject: CurrentValueSubject<String?, Never>

    init(router: ChoosePromotionRouter,
         data: ChoosePromotionBuilder.DataModel,
         findEligibleUseCase: FindEligibleCampaignsUseCase = FindEligibleCampaignsUseCase()) {
        self.data = data
        self.findEligibleUseCase = findEligibleUseCase
        // Seed bằng data preload từ widget (nếu có) — tránh double call. Rỗng → fetch khi transform.
        self.stateSubject = CurrentValueSubject(PagingState(
            myLoaded: data.preloadedMy,
            otherLoaded: data.preloadedOther,
            myPage: 0,
            otherPage: 0,
            myIsLastPage: data.myIsLastPage,
            otherIsLastPage: data.otherIsLastPage,
            myVisibleCount: Self.myPromotionsInitialVisibleCount
        ))
        // Pre-select voucher đang áp (nếu có) — khớp Android `preSelectedVoucherIds`.
        self.selectedPromotionSubject = CurrentValueSubject(data.preSelectedVoucherId)
        super.init(router: router)
    }

    func transform(input: Input) -> Output {
        self.input = input
        loadInitialPage()

        // Search theo ký tự (debounce) → lọc CLIENT-SIDE trên data đã tải (Eligible API không có keyword).
        input.searchText
            .dropFirst()
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .removeDuplicates()
            .debounce(for: .milliseconds(Self.searchDebounceMs), scheduler: DispatchQueue.main)
            .sink { [weak self] keyword in self?.keywordSubject.send(keyword) }
            .store(in: &cancellables)

        // Chọn/bỏ chọn voucher: trùng id đang chọn → bỏ chọn.
        input.toggleSelectionRelay
            .sink { [weak self] id in
                guard let self = self else { return }
                let current = self.selectedPromotionSubject.value
                self.selectedPromotionSubject.send(current == id ? nil : id)
            }
            .store(in: &cancellables)

        // Nút cuối "Ưu đãi của tôi": lộ thêm data đã tải -> gọi page kế -> khi hết thì "Thu gọn".
        input.seeMoreMyRelay
            .sink { [weak self] in
                guard let self = self else { return }
                var st = self.stateSubject.value
                if st.myVisibleCount < st.myLoaded.count {
                    st.myVisibleCount = st.myLoaded.count
                    self.stateSubject.send(st)
                } else if !st.myIsLastPage {
                    self.fetchNextMyPage()
                } else {
                    // Đã hiện hết tất cả -> thu gọn về số ban đầu.
                    st.myVisibleCount = Self.myPromotionsInitialVisibleCount
                    self.stateSubject.send(st)
                }
            }
            .store(in: &cancellables)

        // Cuộn tới đáy "Ưu đãi khác": gọi page kế (giữ keyword hiện tại; VM tự bỏ nếu hết trang/đang tải).
        input.loadMoreOtherRelay
            .sink { [weak self] in self?.fetchNextOtherPage() }
            .store(in: &cancellables)

        // Sections: data từ state (server), lọc CLIENT-SIDE theo keyword (nếu có) qua PRMOfferSearchFilter.
        let sections = Publishers.CombineLatest3(selectedPromotionSubject, stateSubject, keywordSubject)
            .map { [weak self] tuple -> [PromotionSection] in
                let (selectedId, st, keyword) = tuple
                guard let self = self else { return [] }
                let isSearching = !keyword.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                let filteredMy = isSearching ? PRMOfferSearchFilter.search(query: keyword, in: st.myLoaded) : st.myLoaded
                let filteredOther = isSearching ? PRMOfferSearchFilter.search(query: keyword, in: st.otherLoaded) : st.otherLoaded

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
            .receive(on: DispatchQueue.main)
            .eraseToAnyPublisher()

        let hasSelection = selectedPromotionSubject
            .map { $0 != nil }
            .receive(on: DispatchQueue.main)
            .eraseToAnyPublisher()

        let selectedPromotion = selectedPromotionSubject
            .map { [weak self] id -> EligibleOffer? in
                guard let self = self, let id = id else { return nil }
                return self.allLoadedPromotions().first { $0.id == id }
            }
            .receive(on: DispatchQueue.main)
            .eraseToAnyPublisher()

        return Output(
            sections: sections,
            hasSelection: hasSelection,
            selectedPromotion: selectedPromotion,
            isLoading: isLoadingSubject.receive(on: DispatchQueue.main).eraseToAnyPublisher()
        )
    }

    // MARK: - Initial load (trang 0: my + other) — màn tự fetch khi xuất hiện

    private func loadInitialPage() {
        guard !didLoadInitial else { return }
        didLoadInitial = true
        // Đã có data preload từ widget → dùng ngay, KHÔNG gọi API, KHÔNG shimmer (giống Android PreloadVouchers).
        let st = stateSubject.value
        if !st.myLoaded.isEmpty || !st.otherLoaded.isEmpty {
            isLoadingSubject.send(false)
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

    /// Gọi Find Eligible Campaigns (suspend Kotlin).
    private func findEligible(section: EligibleSection?, myPage: Int, otherPage: Int) async throws -> EligibleOffersResult? {
        let request = makeEligibleInput(section: section, myPage: myPage, otherPage: otherPage)
        let useCase = findEligibleUseCase
        return try await useCase.invoke(request: request)
    }

    /// Fetch trang 0 (my + other) qua Eligible API, reset state.
    private func fetchFirstPage() {
        isLoadingSubject.send(true)
        Task { @MainActor [weak self] in
            guard let self = self else { return }
            let model: EligibleOffersResult?
            do {
                model = try await self.findEligible(section: nil, myPage: 0, otherPage: 0)
            } catch {
                self.isLoadingSubject.send(false)
                return
            }
            guard let model else { return }
            var st = self.stateSubject.value
            st.myLoaded = model.myOffers
            st.otherLoaded = model.otherOffers
            st.myPage = 0
            st.otherPage = 0
            st.myIsLastPage = model.myIsLastPage
            st.otherIsLastPage = model.otherIsLastPage
            st.myVisibleCount = Self.myPromotionsInitialVisibleCount
            self.stateSubject.send(st)
            self.isLoadingSubject.send(false)
        }
    }

    // MARK: - Pagination (load-more độc lập từng nhóm qua sectionCode)

    private func fetchNextMyPage() {
        let st0 = stateSubject.value
        guard !st0.myIsLastPage, !isFetchingMy.value else { return }
        isFetchingMy.send(true)
        let nextPage = st0.myPage + 1
        // Chỉ load-more nhóm myOffers (sectionCode=my_offers) → otherOffers trả null, bỏ qua.
        Task { @MainActor [weak self] in
            guard let self = self else { return }
            let model: EligibleOffersResult?
            do {
                model = try await self.findEligible(section: EligibleSection.myOffers, myPage: nextPage, otherPage: st0.otherPage)
            } catch {
                self.isFetchingMy.send(false)
                return
            }
            guard let model else { return }
            var st = self.stateSubject.value
            st.myLoaded += model.myOffers
            st.myPage = nextPage
            st.myIsLastPage = model.myIsLastPage
            st.myVisibleCount = st.myLoaded.count
            self.stateSubject.send(st)
            self.isFetchingMy.send(false)
        }
    }

    private func fetchNextOtherPage() {
        let st0 = stateSubject.value
        guard !st0.otherIsLastPage, !isFetchingOther.value else { return }
        isFetchingOther.send(true)
        let nextPage = st0.otherPage + 1
        // Chỉ load-more nhóm otherOffers (sectionCode=other_offers) → myOffers trả null, bỏ qua.
        Task { @MainActor [weak self] in
            guard let self = self else { return }
            let model: EligibleOffersResult?
            do {
                model = try await self.findEligible(section: EligibleSection.otherOffers, myPage: st0.myPage, otherPage: nextPage)
            } catch {
                self.isFetchingOther.send(false)
                return
            }
            guard let model else { return }
            var st = self.stateSubject.value
            st.otherLoaded += model.otherOffers
            st.otherPage = nextPage
            st.otherIsLastPage = model.otherIsLastPage
            self.stateSubject.send(st)
            self.isFetchingOther.send(false)
        }
    }

    private func allLoadedPromotions() -> [EligibleOffer] {
        let st = stateSubject.value
        return st.myLoaded + st.otherLoaded
    }

    func routeToDetail(id: String) {
        if let promotion = allLoadedPromotions().first(where: { $0.id == id }) {
            router.routeToDetail(promotion: promotion, customerId: data.customerId, token: data.token)
        }
    }
}
