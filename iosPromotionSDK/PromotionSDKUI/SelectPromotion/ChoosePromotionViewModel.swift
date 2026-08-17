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
        /// Đang nạp lại do kéo-để-tải-lại → chỉ vòng xoay của `UIRefreshControl`, KHÔNG bật shimmer
        /// toàn màn như [isLoading]. Đối ứng `swipeRefreshVoucher.isRefreshing` bên Android.
        var isRefreshing = false
        /// Đang lấy trang kế của nhóm "Ưu đãi khác" (cuộn tới đáy) → spinner ở đáy list. Nhóm "Ưu đãi
        /// của tôi" phân trang bằng nút "Xem thêm" nên không dùng cờ này.
        /// Đối ứng `ChoosePromotionListItem.Loading` bên Android.
        var isLoadingMoreOther = false
        /// Gõ từ khoá mà không ra kết quả → view "không tìm thấy" thay cho list. List rỗng lúc KHÔNG
        /// tìm kiếm thì không tính (đó là "chưa có ưu đãi nào").
        /// Đối ứng `showNoResult` trong `ChoosePromotionFragment.observeData`.
        var showsNoResult = false
        /// Hiện view rỗng: tìm không ra kết quả **hoặc** lượt nạp vừa hỏng (kéo-để-tải-lại lỗi).
        /// Luật ở store (`ChoosePromotionState.showsEmptyView()`) — Android đọc đúng hàm đó.
        var showsEmptyView = false
        /// Thanh "Đã chọn N voucher" — chỉ hiện ở chế độ multi-select và đang có item được chọn.
        /// Đối ứng `ChoosePromotionFragment.updateApplyButtonState` bên Android.
        var showsSelectedCount = false
        var selectedCount = 0
        /// Nút "Áp dụng" bấm được chưa — luật ở store (`canApply()`), đối ứng
        /// `binding.btnApply.isEnabled` bên Android. Trước đây VC tự suy `selectedCount > 0`.
        var canApply = false
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
        // Cờ gác nay ở STORE (`SeedOnce`), không phải `didStart` của VM: Android không có cờ tương
        // ứng nên `observeData()` bắn lại mỗi lần view dựng lại và ghi đè tick của user. Một cờ dùng
        // chung thì hai bên không thể lệch.
        dispatch(ChoosePromotionIntentSeedOnce(
            preSelectedIds: data.preSelectedVoucherIds,
            myOffers: data.preloadedMy,
            otherOffers: data.preloadedOther,
            myIsLastPage: data.myIsLastPage,
            otherIsLastPage: data.otherIsLastPage
        ))
    }

    /// Gõ trắng **không** cần rẽ nhánh sang `ClearKeyword`: store đã xử đúng đường đó trong
    /// `onQueryChanged` (huỷ debounce, nạp lại ngay). Nhánh `if` cũ ở đây là bản chép của
    /// `ChoosePromotionFragment.setupSearch` bên Android — cùng một luật viết hai lần.
    func query(_ keyword: String) {
        dispatch(ChoosePromotionIntentQueryChanged(keyword: keyword))
    }

    /// Điều hướng — không nằm ở store.
    func openDetail(voucherId: String) {
        guard let promotion = store.currentState().allOffers().first(where: { $0.id == voucherId })
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
    /// Luật lọc nằm ở store (`ChoosePromotionState.selectedOffers()`) — Android gọi đúng hàm này.
    func selectedOffers() -> [EligibleOffer] {
        store.currentState().selectedOffers()
    }
}

// ─── Map state dùng chung (store) → model UI iOS ──────────────────────────────

/// Chiếu state dùng chung ([ChoosePromotionState]) → **view model đã dựng** (`Display`).
private extension ChoosePromotionState {

    /// Mọi **quyết định** đều lấy từ store (`showsNoResult()` / `showsSelectedCount()` / `canApply()`)
    /// — Android đọc đúng những hàm đó. Ở đây chỉ còn việc xếp chúng vào struct cho VC.
    func toDisplay() -> ChoosePromotionViewModel.Display {
        ChoosePromotionViewModel.Display(
            sections: buildSections(),
            isLoading: isLoading,
            isRefreshing: isRefreshing,
            isLoadingMoreOther: isLoadingMoreOther,
            showsNoResult: showsNoResult(),
            showsEmptyView: showsEmptyView(),
            showsSelectedCount: showsSelectedCount(),
            selectedCount: selectedIds.count,
            canApply: canApply()
        )
    }

    /// iOS-only UI: dựng sections cho `UITableView` (Android dựng list item ở adapter).
    /// Selection + mở/thu gọn đều đã nằm ở store — hàm này chỉ đọc.
    func buildSections() -> [ChoosePromotionViewModel.PromotionSection] {
        let trimmed = highlightKeyword()
        let isSearching = !trimmed.isEmpty
        let state = self
        let cell: (ChooseOffer) -> MyPromotionCellViewModel = { offer in
            MyPromotionCellViewModel(
                offer: offer.source,
                isEnabled: offer.isUsable,
                buttonTitle: PromotionUIStrings.detail,
                showsCheckbox: true,
                isChecked: state.isSelected(id: offer.source.id),
                // Hết hạn có chuỗi riêng. Không truyền thì cell rơi vào nhánh mặc định
                // `unmatchedRules.first ?? .ineligible` → hiện "Không đủ điều kiện", sai nghĩa.
                stateText: offer.isExpired ? PromotionUIStrings.expired : nil,
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
