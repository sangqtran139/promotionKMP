//
//  ChoosePromotionViewModel.swift
//  PromotionSDK
//
//  Bọc ChoosePromotionStore — phần dùng chung (giữ store, observe state, effect) nằm ở
//  `PRMStoreViewModel`. Load/paging/search/selection và rule "Xem thêm" nằm ở store; ở đây chỉ còn
//  việc dựng sections cho VC render. Bên Android việc dựng list này nằm trong
//  `ChoosePromotionFragment.rebuildList`.
//

import Foundation
import UIKit
@_implementationOnly import PRMFoundation   // UIImage.sdk(_:) cho ảnh checkbox của cell
@_implementationOnly import PRMKotlinBridge

final class ChoosePromotionViewModel:
    PRMScreenViewModel<ChoosePromotionRouter, ChoosePromotionStore> {

    enum SectionType: String {
        case myPromotions = "myPromotions"
        case otherPromotions = "otherPromotions"
    }

    enum SeeMoreState { case none, expand, collapse }

    struct PromotionSection {
        let type: SectionType
        let title: String
        let items: [MyPromotionCellViewModel]
        let totalCount: Int
        let seeMoreState: SeeMoreState
    }

    /// **View model đã dựng sẵn** cho table: sections + trạng thái nút. KHÔNG phải state của store
    /// — state gốc là `ChoosePromotionState` (dùng chung với Android). Bên Android việc dựng section
    /// này nằm thẳng trong `ChoosePromotionFragment.rebuildList`; iOS gom vào VM cho VC mỏng.
    struct Display {
        var sections: [PromotionSection] = []
        var isLoading = false
        /// Thanh "Đã chọn N voucher" — chỉ hiện ở chế độ multi-select và đang có item được chọn.
        /// Đối ứng `ChoosePromotionFragment.updateApplyButtonState` bên Android.
        var showsSelectedCount = false
        var selectedCount = 0
    }

    let data: ChoosePromotionBuilder.DataModel

    /// State hiện tại + kênh phát. Gán `onState` là **nhận ngay** state hiện tại — mô phỏng đúng
    /// hành vi replay của `StateFlow` bên Android.
    /// Kênh phát **view model đã dựng**. State thô + effect do [PRMStoreViewModel] lo; ở đây thêm
    /// một tầng dựng section vì table cần cấu trúc sẵn.
    private(set) var display = Display() {
        didSet { onDisplay?(display) }
    }
    var onDisplay: ((Display) -> Void)? {
        didSet { onDisplay?(display) }
    }

    // Selection (`selectedIds`) + mở/thu gọn (`myExpanded`) nay do store quản — VC chỉ render.
    private var didStart = false

    init(router: ChoosePromotionRouter,
         data: ChoosePromotionBuilder.DataModel,
         findEligibleUseCase: FindEligibleCampaignsUseCase = FindEligibleCampaignsUseCase()) {
        self.data = data
        super.init(router: router,
                   store: ChoosePromotionStore(findEligibleCampaignsUseCase: findEligibleUseCase))
        // Base phát state thô; màn này còn phải dựng section nên bắc thêm một nhịp sang `display`.
        onState = { [weak self] state in self?.render(state) }
    }

    /// Seed pre-select rồi preload/fetch — đối ứng `ChoosePromotionFragment.observeData`
    /// (SetPreSelected → Preload). `didStart` chặn chạy lại khi màn được bind lại.
    func loadInitialIfNeeded() {
        guard !didStart else { return }
        didStart = true
        dispatch(ChoosePromotionIntentSetPreSelected(ids: data.preSelectedVoucherIds))
        dispatch(ChoosePromotionIntentPreload(
            myOffers: data.preloadedMy,
            otherOffers: data.preloadedOther,
            myIsLastPage: data.myIsLastPage,
            otherIsLastPage: data.otherIsLastPage
        ))
    }

    /// Xoá trắng → `ClearKeyword` (reload ngay, không chờ debounce) — khớp Fragment Android.
    func query(_ keyword: String) {
        if keyword.isEmpty {
            dispatch(ChoosePromotionIntentClearKeyword.shared)
        } else {
            dispatch(ChoosePromotionIntentQueryChanged(keyword: keyword))
        }
    }

    /// Điều hướng — không nằm ở store.
    func openDetail(voucherId: String) {
        guard let promotion = Self.allLoaded(store.currentState()).first(where: { $0.id == voucherId })
        else { return }
        router.routeToDetail(promotion: promotion)
    }

    // ─── State → View ─────────────────────────────────────────────────────────
    private func render(_ state: ChoosePromotionState) {
        display = state.toDisplay()
    }

    /// Ưu đãi user đang chọn, để bấm "Áp dụng" trả về widget — validate & áp do `EndowStore` lo.
    /// Là **hàm gọi lúc bấm** chứ không phải effect: chỉ đọc selection hiện tại, không có gì bất
    /// đồng bộ để chờ. Đối ứng `ChoosePromotionViewModel.selectedOffers()` bên Android.
    func selectedOffers() -> [EligibleOffer] {
        let state = store.currentState()
        return Self.allLoaded(state).filter { state.selectedIds.contains($0.id) }
    }

    fileprivate static func allLoaded(_ state: ChoosePromotionState) -> [EligibleOffer] {
        state.myOffers.map { $0.source } + state.otherOffers.map { $0.source }
    }
}

// ─── Map state dùng chung (store) → model UI iOS ──────────────────────────────

/// Chiếu state dùng chung ([ChoosePromotionState]) → **view model đã dựng** (`Display`).
private extension ChoosePromotionState {

    func toDisplay() -> ChoosePromotionViewModel.Display {
        ChoosePromotionViewModel.Display(
            sections: buildSections(),
            isLoading: isLoading,
            showsSelectedCount: isMultiSelection && !selectedIds.isEmpty,
            selectedCount: selectedIds.count
        )
    }

    /// iOS-only UI: dựng sections cho `UITableView` (Android dựng list item ở adapter).
    /// Selection + mở/thu gọn đều đã nằm ở store — hàm này chỉ đọc.
    func buildSections() -> [ChoosePromotionViewModel.PromotionSection] {
        let trimmed = keyword.trimmingCharacters(in: .whitespacesAndNewlines)
        let isSearching = !trimmed.isEmpty
        let selected = selectedIds
        let cell: (ChooseOffer) -> MyPromotionCellViewModel = { offer in
            MyPromotionCellViewModel(
                offer: offer.source,
                isEnabled: offer.isUsable,
                buttonTitle: PromotionUIStrings.detail,
                showsCheckbox: true,
                isChecked: selected.contains(offer.source.id),
                checkedImage: UIImage.sdk("prm_ic_circle_check"),
                uncheckedImage: UIImage.sdk("prm_ic_circle_uncheck"),
                highlightKeyword: isSearching ? trimmed : nil,
                expiringInDays: offer.expiringInDays?.intValue
            )
        }

        var sections: [ChoosePromotionViewModel.PromotionSection] = []
        if !myOffers.isEmpty {
            // Slice + trạng thái nút "Xem thêm/Thu gọn" đều lấy từ **rule dùng chung** ở promotionLogic
            // (`visibleMyOffers()` / `mySeeMoreState()`) — y như Android, không bên nào tự suy lại.
            sections.append(.init(type: .myPromotions, title: PromotionUIStrings.myPromotions,
                                  items: visibleMyOffers().map(cell), totalCount: myOffers.count,
                                  seeMoreState: mySeeMoreState().toSeeMoreState()))
        }
        if !otherOffers.isEmpty {
            sections.append(.init(type: .otherPromotions, title: PromotionUIStrings.otherPromotions,
                                  items: otherOffers.map(cell), totalCount: otherOffers.count,
                                  seeMoreState: .none))
        }
        return sections
    }
}

/// `ChooseSeeMoreState` (rule dùng chung ở promotionLogic) → enum hiển thị của VC.
private extension ChooseSeeMoreState {
    func toSeeMoreState() -> ChoosePromotionViewModel.SeeMoreState {
        switch self {
        case .hidden: return .none
        case .collapse: return .collapse
        case .expand: return .expand
        default: return .expand
        }
    }
}
