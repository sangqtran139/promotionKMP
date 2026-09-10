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
        /// Câu server trả khi vừa từ chối ưu đãi ở lượt "Áp dụng" — **một-lần**, VC hiện popup rồi
        /// `ConsumeApplyMessage`. Đối ứng `ChoosePromotionFragment.showApplyMessageIfAny` bên Android.
        var applyMessage: String?
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

    /// Seed pre-select rồi **gọi `findEligible`** — đối ứng `ChoosePromotionFragment.observeData`.
    ///
    /// Cờ gác nay ở STORE (`SeedOnce`), không phải `didStart` của VM: Android không có cờ tương ứng
    /// nên `observeData()` bắn lại mỗi lần view dựng lại và ghi đè tick của user. Một cờ dùng chung
    /// thì hai bên không thể lệch.
    ///
    /// Không còn truyền danh sách preload của widget: vào màn là dữ liệu phải mới (ngân sách có thể
    /// đã hết, voucher có thể vừa bị dùng ở thiết bị khác).
    func loadInitialIfNeeded() {
        dispatch(ChoosePromotionIntentSeedOnce(preSelectedIds: data.preSelectedVoucherIds))
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

    /// Ưu đãi user đang chọn, để bấm "Áp dụng" trả về widget — validate & áp do `OfferWidgetStore` lo.
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
            canApply: canApply(),
            applyMessage: applyMessage
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
                // Dải cảnh báo bám luật riêng ở store, KHÔNG phải `!isUsable`: ca hết hạn không có dải.
                showsIneligibleWarning: offer.showsIneligibleWarning(),
                // Không dùng được → giấu luôn nút "Chi tiết", đối ứng Android
                // `lnDetail.isVisible = canUse`.
                //
                // Với ca HẾT HẠN / `usable = false` thì `MyPromotionCell` đã tự bỏ nút (hai nhánh
                // `.expired`/`.ineligible` gán `derivedButtonTitle = nil`), nhưng ca bị
                // `validateStackableDiscounts` TỪ CHỐI thì không: server vẫn đánh `usable = true` nên
                // `displayState()` ra `.usable` và nút ở lại — card mờ mà vẫn bấm được vào "Chi tiết",
                // trong khi Android đã ẩn. `PromotionCardView` cũng chỉ chặn tap lên CARD
                // (`toggleCheckbox` có `guard !model.isDisabled`), không chặn tap lên nút.
                buttonTitle: offer.isUsable ? PromotionUIStrings.detail : nil,
                // Không chọn được (hết hạn HOẶC chưa đủ điều kiện) → giấu luôn ô tick, không chỉ làm
                // mờ. Đối ứng Android `ChoosePromotionMainAdapter`: `cbUseVoucher.visibility = INVISIBLE`.
                // Trước đây luôn `true` nên ô tick vẫn lòi ra sau lớp phủ mờ trong khi Android đã ẩn.
                // `isHidden` ở đây tương đương INVISIBLE bên Android: checkbox không nằm trong
                // UIStackView nên constraint (merchant kết thúc trước checkbox 8px) vẫn giữ nguyên,
                // card không co lại.
                showsCheckbox: offer.isUsable,
                isChecked: state.isSelected(id: offer.source.id),
                // Hết hạn có chuỗi riêng. Không truyền thì cell rơi vào nhánh mặc định
                // `unmatchedRules.first ?? .ineligible` → hiện "Không đủ điều kiện", sai nghĩa.
                //
                // Ưu đãi bị `validateStackableDiscounts` từ chối (`offer.isRejected`) KHÔNG cần nhánh
                // riêng ở đây: card chỉ mờ đi, không nhãn nào cả — lý do đã hiện ở popup. Nó tự đúng
                // vì `MyPromotionCell` suy nhãn theo `voucher.displayState()`, tức theo `usable` của
                // SERVER, mà server vẫn đánh ưu đãi này `usable = true`. Đối ứng vế `!isRejected` mà
                // Android phải thêm vào adapter (badge bên đó bật theo `!isEnabled` nên không tự đúng).
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
