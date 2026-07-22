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
        /// Danh sách voucher đang chọn — sẵn sàng multi-select; hiện tại gate đơn nên thường 0/1.
        let selectedPromotions: AnyPublisher<[EligibleOffer], Never>
        /// Đang tải trang đầu (để hiện shimmer).
        let isLoading: AnyPublisher<Bool, Never>
    }

    /// Cho phép chọn nhiều voucher. Hiện tại = false (chọn đơn) — bật lên khi backend/UX sẵn sàng
    /// multi-select. Khớp `ChoosePromotionFragment.isMultiSelection` bên Android.
    private static let isMultiSelection = false

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
    /// Context (customerId/orderId/orderValue/token) đọc từ lõi — **đối xứng Android** (ViewModel lấy
    /// thẳng `PromotionContainer.requestContextProvider`), không threading qua DataModel.
    private var requestContext: PromotionRequestContextProvider { PromotionContainer.shared.requestContextProvider }
    private let findEligibleUseCase: FindEligibleCampaignsUseCase
    private(set) var input: Input!

    // MARK: - State
    private let stateSubject: CurrentValueSubject<PagingState, Never>
    private let isFetchingMy = CurrentValueSubject<Bool, Never>(false)
    private let isFetchingOther = CurrentValueSubject<Bool, Never>(false)
    /// "Latest wins" cho fetch: mỗi lần reset (fetchFirstPage) tăng 1; response mang token cũ bị bỏ.
    /// Kotlin/Native KHÔNG hủy coroutine khi hủy `Task`, nên guard token mới thực sự chặn stale;
    /// `cancel()` chỉ dọn Swift-side. Khớp `MyPromotionViewModel.fetchGeneration`.
    private var fetchGeneration = 0
    private var firstPageTask: Task<Void, Never>?
    private var myPageTask: Task<Void, Never>?
    private var otherPageTask: Task<Void, Never>?
    /// Đang tải trang đầu (bắt đầu true → hiện shimmer ngay khi mở màn).
    private let isLoadingSubject = CurrentValueSubject<Bool, Never>(true)
    private var didLoadInitial = false
    /// Keyword đang search — gửi lên server (SERVER-SIDE, parity Android). Server lọc cả myOffers lẫn
    /// otherOffers (v1.6 §7.3); đổi keyword → reload trang 0. Giữ để tô sáng kết quả.
    private let keywordSubject = CurrentValueSubject<String, Never>("")
    /// Voucher đang chọn (danh sách id, sẵn sàng multi-select); seed = voucher đang áp.
    /// Toggle: trùng id → bỏ chọn; gate đơn thì thay thế cả list bằng id mới.
    private let selectedPromotionSubject: CurrentValueSubject<[String], Never>

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
        self.selectedPromotionSubject = CurrentValueSubject(data.preSelectedVoucherIds)
        super.init(router: router)
    }

    func transform(input: Input) -> Output {
        self.input = input
        loadInitialPage()

        // Search theo ký tự (debounce) → SERVER-SIDE (parity Android): set keyword rồi reload trang 0.
        // Keyword đi kèm cả load-more (makeEligibleInput đọc keywordSubject), server lọc 2 nhóm.
        input.searchText
            .dropFirst()
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .removeDuplicates()
            .debounce(for: .milliseconds(Self.searchDebounceMs), scheduler: DispatchQueue.main)
            .sink { [weak self] keyword in
                guard let self = self else { return }
                self.keywordSubject.send(keyword)
                self.fetchFirstPage()
            }
            .store(in: &cancellables)

        // Chọn/bỏ chọn voucher (khớp Android handleSingleSelection/handleMultiSelection):
        // trùng id → bỏ chọn; chưa chọn → multi thì thêm, đơn thì thay cả list bằng id mới.
        input.toggleSelectionRelay
            .sink { [weak self] id in
                guard let self = self else { return }
                var selected = self.selectedPromotionSubject.value
                if selected.contains(id) {
                    selected.removeAll { $0 == id }
                } else if Self.isMultiSelection {
                    selected.append(id)
                } else {
                    selected = [id]
                }
                self.selectedPromotionSubject.send(selected)
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

        // Sections: data đã do SERVER lọc theo keyword (parity Android) — KHÔNG lọc client nữa.
        // keyword chỉ còn dùng để tô sáng (highlight) kết quả; phân trang giữ nguyên khi search.
        let sections = Publishers.CombineLatest3(selectedPromotionSubject, stateSubject, keywordSubject)
            .map { tuple -> [PromotionSection] in
                let (selectedIds, st, keyword) = tuple
                let trimmedKeyword = keyword.trimmingCharacters(in: .whitespacesAndNewlines)
                let isSearching = !trimmedKeyword.isEmpty
                let myOffers = st.myLoaded
                let otherOffers = st.otherLoaded

                let mapToViewModel: (EligibleOffer) -> MyPromotionCellViewModel = { entity in
                    MyPromotionCellViewModel(
                        offer: entity,
                        buttonTitle: "Chi tiết",
                        showsCheckbox: true,
                        isChecked: selectedIds.contains(entity.id),
                        checkedImage: UIImage.sdk("prm_ic_circle_check"),
                        uncheckedImage: UIImage.sdk("prm_ic_circle_uncheck"),
                        highlightKeyword: isSearching ? trimmedKeyword : nil
                    )
                }

                var sections: [PromotionSection] = []

                if !myOffers.isEmpty {
                    let visibleItems = Array(myOffers.prefix(st.myVisibleCount))
                    let seeMoreState: SeeMoreState
                    if st.myVisibleCount < myOffers.count || !st.myIsLastPage {
                        seeMoreState = .expand
                    } else if myOffers.count > Self.myPromotionsInitialVisibleCount {
                        seeMoreState = .collapse
                    } else {
                        seeMoreState = .none
                    }
                    sections.append(PromotionSection(
                        type: .myPromotions,
                        title: "Ưu đãi của tôi",
                        items: visibleItems.map(mapToViewModel),
                        totalCount: myOffers.count,
                        seeMoreState: seeMoreState
                    ))
                }

                if !otherOffers.isEmpty {
                    sections.append(PromotionSection(
                        type: .otherPromotions,
                        title: "Ưu đãi khác",
                        items: otherOffers.map(mapToViewModel),
                        totalCount: otherOffers.count,
                        seeMoreState: .none
                    ))
                }

                return sections
            }
            .receive(on: DispatchQueue.main)
            .eraseToAnyPublisher()

        let hasSelection = selectedPromotionSubject
            .map { !$0.isEmpty }
            .receive(on: DispatchQueue.main)
            .eraseToAnyPublisher()

        let selectedPromotions = selectedPromotionSubject
            .map { [weak self] ids -> [EligibleOffer] in
                guard let self = self else { return [] }
                // Giữ thứ tự chọn; bỏ id không còn trong data đã tải.
                return ids.compactMap { id in self.allLoadedPromotions().first { $0.id == id } }
            }
            .receive(on: DispatchQueue.main)
            .eraseToAnyPublisher()

        return Output(
            sections: sections,
            hasSelection: hasSelection,
            selectedPromotions: selectedPromotions,
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

    /// Tạo input Eligible từ order data của màn. section=nil → cả 2 nhóm. Rule phân trang ĐỘC LẬP
    /// 2 nhóm nằm ở domain (`FindEligibleCampaignsRequest.forSectionPage`) — dùng chung Android & iOS.
    /// Token do host cấp qua `PromotionRequestContextProvider` của lõi, không gửi từng request.
    private func makeEligibleInput(section: EligibleSection?, nextPage: Int, currentMyPage: Int, currentOtherPage: Int) -> FindEligibleCampaignsRequest {
        // Keyword hiện tại → gửi lên server (server-side, parity Android); rỗng → nil (không lọc).
        let trimmedKeyword = keywordSubject.value.trimmingCharacters(in: .whitespacesAndNewlines)
        let base = FindEligibleCampaignsRequest(
            orderId: requestContext.getOrderId() ?? "",
            orderValue: requestContext.getOrderValue() ?? "0",
            items: data.orderItems,
            currency: "VND",
            channel: "MOBILE",
            customerType: nil,
            segment: nil,
            tier: nil,
            tabCode: nil,
            section: nil,
            keyword: trimmedKeyword.isEmpty ? nil : trimmedKeyword,
            myPage: 0,
            mySize: Int32(Self.pageSize),
            otherPage: 0,
            otherSize: Int32(Self.pageSize),
            filterOptions: EligibleFilterOptions(includeExpired: false, checkBudgetAvailability: true, includePreview: true)
        )
        return base.forSectionPage(
            section: section,
            nextPage: Int32(nextPage),
            currentMyPage: Int32(currentMyPage),
            currentOtherPage: Int32(currentOtherPage)
        )
    }

    /// Gọi Find Eligible Campaigns (suspend Kotlin).
    private func findEligible(section: EligibleSection?, nextPage: Int, currentMyPage: Int, currentOtherPage: Int) async throws -> EligibleOffersResult? {
        let request = makeEligibleInput(section: section, nextPage: nextPage, currentMyPage: currentMyPage, currentOtherPage: currentOtherPage)
        let useCase = findEligibleUseCase
        return try await useCase.invoke(request: request)
    }

    /// Fetch trang 0 (my + other) qua Eligible API, reset state.
    private func fetchFirstPage() {
        // Reset: hủy fetch cũ (kể cả load-more đang chạy) + bump generation để bỏ response cũ.
        firstPageTask?.cancel()
        myPageTask?.cancel()
        otherPageTask?.cancel()
        fetchGeneration += 1
        let token = fetchGeneration
        isLoadingSubject.send(true)
        firstPageTask = Task { @MainActor [weak self] in
            guard let self = self else { return }
            let model: EligibleOffersResult?
            do {
                model = try await self.findEligible(section: nil, nextPage: 0, currentMyPage: 0, currentOtherPage: 0)
            } catch {
                if token == self.fetchGeneration { self.isLoadingSubject.send(false) }
                return
            }
            // Có reset mới hơn → bỏ qua kết quả này (tránh đè data mới bằng data cũ).
            guard token == self.fetchGeneration else { return }
            guard let model else { self.isLoadingSubject.send(false); return }
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
        let token = fetchGeneration
        // Chỉ load-more nhóm myOffers (sectionCode=my_offers) → otherOffers trả null, bỏ qua.
        myPageTask?.cancel()
        myPageTask = Task { @MainActor [weak self] in
            guard let self = self else { return }
            let model: EligibleOffersResult?
            do {
                model = try await self.findEligible(section: EligibleSection.myOffers, nextPage: nextPage, currentMyPage: st0.myPage, currentOtherPage: st0.otherPage)
            } catch {
                if token == self.fetchGeneration { self.isFetchingMy.send(false) }
                return
            }
            // Reset xen giữa → bỏ append (data cũ không được nối vào list đã reset).
            guard token == self.fetchGeneration else { return }
            guard let model else { self.isFetchingMy.send(false); return }
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
        let token = fetchGeneration
        // Chỉ load-more nhóm otherOffers (sectionCode=other_offers) → myOffers trả null, bỏ qua.
        otherPageTask?.cancel()
        otherPageTask = Task { @MainActor [weak self] in
            guard let self = self else { return }
            let model: EligibleOffersResult?
            do {
                model = try await self.findEligible(section: EligibleSection.otherOffers, nextPage: nextPage, currentMyPage: st0.myPage, currentOtherPage: st0.otherPage)
            } catch {
                if token == self.fetchGeneration { self.isFetchingOther.send(false) }
                return
            }
            // Reset xen giữa → bỏ append.
            guard token == self.fetchGeneration else { return }
            guard let model else { self.isFetchingOther.send(false); return }
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
            router.routeToDetail(promotion: promotion)
        }
    }
}
