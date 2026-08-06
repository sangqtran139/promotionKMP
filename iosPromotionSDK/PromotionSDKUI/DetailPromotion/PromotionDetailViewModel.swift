//
//  PromotionDetailViewModel.swift
//  PromotionSDK
//
//  Lớp bọc mỏng quanh PromotionDetailStore (tầng UI-logic dùng chung ở promotionLogic).
//  ĐỒNG NHẤT với `PromotionDetailViewModel` bên Android — cùng `store` / `bindStore` /
//  `handleAction` / `render` / `handleError`, cùng thứ tự. Fetch + quyết định nút nằm ở store;
//  VM chỉ FORMAT hiển thị (card/HTML/ngày) — phần rendering vốn là của native.
//
//  KHÔNG seed từ màn danh sách: mọi thứ hiển thị đều đến từ `getCustomerVoucherDetail`; trong lúc
//  chờ là `UiState.initial` + shimmer. `data.promotion` chỉ dùng để lấy `voucherId` cần fetch.
//
//  KHÔNG dùng Combine: store đã phơi callback (`watchState`), nên VM cũng phơi callback
//  (`onState` / `onEffect`) — đối ứng 1-1 `uiState: StateFlow` / `uiEffect: Flow` bên Android.
//

import Foundation
@_implementationOnly import PRMDesignKit
@_implementationOnly import PRMKotlinBridge

final class PromotionDetailViewModel: PRMBaseViewModel<PromotionDetailRouter> {

    /// Nội dung 1 tab, đã bọc thành **trang HTML hoàn chỉnh** bằng `wrapPromotionHtml` dùng chung với
    /// Android — VC chỉ việc nạp thẳng vào `WKWebView`. Rỗng = trang trống.
    struct ContentDisplay {
        let html: String

        static let empty = ContentDisplay(html: "")
    }

    /// Nội dung cả 2 tab, render đồng thời vào 2 trang vuốt được.
    struct TabContents {
        let detail: ContentDisplay
        let guide: ContentDisplay

        static let empty = TabContents(detail: .empty, guide: .empty)
    }

    /// Bề mặt view (đã format) — đối ứng `PromotionDetailUiState` bên Android.
    struct UiState {
        var card: VoucherCardViewModel
        var banner: String?
        var tabContents: TabContents
        var applyTitle: String
        var isApplyEnabled: Bool
        var isApplyVisible: Bool
        var isLoading: Bool

        /// Trước khi có detail — trống hoàn toàn, shimmer che ở VC.
        static let initial = UiState(
            card: VoucherCardViewModel(title: "", description: "", logoURL: nil, date: ""),
            banner: nil,
            tabContents: .empty,
            applyTitle: "",
            isApplyEnabled: false,
            isApplyVisible: false,
            isLoading: true
        )
    }

    /// Đối ứng `PromotionDetailAction` bên Android — chỉ những gì màn thật sự phát.
    enum Action {
        case loadDetail
        case openServiceSelector
        case serviceSelected(ServiceSelectorItem)
    }

    /// Sự kiện một-lần — đối ứng `PromotionDetailEffect` bên Android.
    enum Effect {
        case showError(String)
        /// Mở "Chọn dịch vụ". Luật **1 dịch vụ → chọn thẳng, không mở sheet** (TLNV MOB_002 control
        /// #5) nằm trong `ServiceSelectorBottomSheet.present`, dùng chung cho cả 3 màn.
        case showServiceSelector([ServiceSelectorItem])
    }

    /// Id voucher dùng cho callback "Áp dụng".
    let voucherId: String
    let data: PromotionDetailBuilder.DataModel

    /// State hiện tại + kênh phát. Gán `onState` là **nhận ngay** state hiện tại — mô phỏng đúng
    /// hành vi replay của `StateFlow` bên Android.
    private(set) var uiState = UiState.initial {
        didSet { onState?(uiState) }
    }
    var onState: ((UiState) -> Void)? {
        didSet { onState?(uiState) }
    }
    /// Một-lần, KHÔNG replay (giống effect bên Android).
    var onEffect: ((Effect) -> Void)?

    // ─── Store ────────────────────────────────────────────────────────────────
    private let store: PromotionDetailStore
    private var storeCancellable: PromotionCancellable?
    private var didStart = false
    /// Dịch vụ/sản phẩm voucher áp dụng được — chỉ có sau khi detail về.
    private var applicableProducts: [ApplicableProduct] = []
    /// Chi tiết mới nhất từ store — nguồn cho `notifyVoucherApplied()`. Nil khi chưa/không có data.
    private var currentDetail: VoucherDetail?

    init(router: PromotionDetailRouter,
         data: PromotionDetailBuilder.DataModel,
         getDetailUseCase: GetCustomerVoucherDetailUseCase = GetCustomerVoucherDetailUseCase()) {
        self.data = data
        self.store = PromotionDetailStore(getCustomerVoucherDetailUseCase: getDetailUseCase)
        self.voucherId = data.promotion.id
        super.init(router: router)
        bindStore()   // đối ứng `init { bindStore() }` bên Android
    }

    deinit {
        storeCancellable?.cancel()
        store.clear()
    }

    // ─── Nhãn nút + hành vi khi bấm (TLNV MOB_002 control #5) ──────────────────
    /// Bật → nút là "Áp dụng", bấm thì trả voucher về nơi đã mở màn (màn "Chọn ưu đãi" nội bộ, hoặc
    /// màn host qua `PromotionSDK.openPromotionDetail(...)`).
    var returnVoucherOnApply: Bool { data.returnVoucherOnApply }

    /// `true` → VC **không** `routeToParent()` sau khi báo; host tự đóng trong `onVoucherApplied`.
    var hostHandlesDismiss: Bool { data.hostHandlesDismiss }

    /// Báo nơi đã mở màn rằng voucher này được chọn. VC tự `routeToParent()` sau đó (đối ứng Android:
    /// `setFragmentResult` + `onVoucherApplied` + `onBackFragment`).
    /// `currentDetail` chỉ có sau khi API trả; nút "Áp dụng" bị khoá trước đó nên bình thường không
    /// nil. Nil thì bỏ callback — không bịa object rỗng cho host (đối ứng Android).
    func notifyVoucherApplied() {
        guard let currentDetail else { return }
        data.onVoucherApplied?(currentDetail)
    }

    // ─── Store observation (đối ứng Android.bindStore) ──────────────────────────
    private func bindStore() {
        storeCancellable = observeStore(watch: { [store] in store.watchState(onEach: $0) }) { [weak self] state in
            guard let self = self else { return }
            self.render(state)
            self.handleError(state)
        }
    }

    // ─── Intent forwarding (đối ứng Android.handleAction) ───────────────────────
    func handleAction(_ action: Action) {
        switch action {
        case .loadDetail:
            // VC gọi một lần lúc bind; `didStart` chặn fetch lặp khi màn được bind lại.
            guard !didStart else { return }
            didStart = true
            store.dispatch(intent: PromotionDetailIntentLoadDetail(voucherId: voucherId))
        case .openServiceSelector:
            openServiceSelector()
        case .serviceSelected:
            break   // TODO: điều hướng màn dịch vụ khi có đích đến
        }
    }

    // ─── State → View ─────────────────────────────────────────────────────────
    /// Chỉ hiển thị khi API detail trả về. Không có detail → về **khung rỗng** (shimmer che lúc đang
    /// tải; hết tải thì card/tab trống, nút ẩn) — đối ứng `PromotionDetailFragment.bindEmptyContent()`
    /// bên Android. Trước đây chỉ cập nhật `isLoading` nên nếu lần load sau trả `detail == nil`
    /// (API lỗi / voucher biến mất) màn **vẫn giữ nội dung voucher cũ**, lệch với Android.
    private func render(_ state: PromotionDetailState) {
        guard let detail = state.detail else {
            applicableProducts = []
            currentDetail = nil
            var empty = UiState.initial
            empty.isLoading = state.isLoading
            uiState = empty
            return
        }
        applicableProducts = detail.applicableProducts
        currentDetail = detail
        // Nhãn nút theo nơi mở màn (TLNV MOB_002 control #5) — chuỗi SDK, không dùng nhãn server.
        uiState = state.toUiState(
            detail: detail,
            applyTitle: returnVoucherOnApply ? PromotionUIStrings.apply : PromotionUIStrings.useNow
        )
    }

    // ─── Error ──────────────────────────────────────────────────────────────────
    private func handleError(_ state: PromotionDetailState) {
        guard let code = state.errorCode else { return }
        onEffect?(.showError(code))   // view map code → chuỗi
        store.dispatch(intent: PromotionDetailIntentConsumeError.shared)
    }

    // ─── Bottom sheet "Chọn dịch vụ" (render native; lọc dùng chung ở promotionLogic) ──
    /// Đối ứng `PromotionDetailViewModel.openServiceSelector()` bên Android.
    private func openServiceSelector() {
        let services = ServiceSelectorBuilder.items(forApplicableProducts: applicableProducts)
        onEffect?(.showServiceSelector(services))
    }

}

// ─── Map state dùng chung (store) → model UI iOS ──────────────────────────────

/// Chiếu state dùng chung ([PromotionDetailState]) → **bề mặt view iOS** (`UiState`).
/// Đối ứng 1-1 `private fun PromotionDetailState.toUiState()` bên Android (cũng là hàm mức file).
///
/// Nhận [detail] đã unwrap: khác Android (UiState bên đó mang thẳng `detail` nullable), iOS format
/// sẵn card/HTML/ngày nên chỉ dựng được khi đã có detail — chưa có thì VM giữ `UiState.initial`.
private extension PromotionDetailState {
    func toUiState(detail: VoucherDetail, applyTitle: String) -> PromotionDetailViewModel.UiState {
        PromotionDetailViewModel.UiState(
            card: VoucherCardViewModel(
                title: detail.merchantName ?? "",
                description: detail.title ?? "",
                logoURL: detail.logo,
                date: Self.dateString(detail.expirationDate)
            ),
            banner: detail.banner,
            tabContents: .init(
                detail: Self.contentDisplay(detail.description_ ?? ""),
                guide: Self.contentDisplay(detail.guideline ?? "")
            ),
            // Nhãn **cố định chuỗi SDK**, KHÔNG dùng `actionLabel` (= `displayStatusLabel` của server).
            // Server trả "Sử dụng" cho mọi voucher; màn này dùng chuỗi riêng theo nơi mở màn — VM
            // truyền vào. Card ở màn danh sách thì vẫn theo nhãn server.
            applyTitle: applyTitle,
            isApplyEnabled: actionEnabled,
            isApplyVisible: actionVisible,
            isLoading: isLoading
        )
    }

    // ─── Display builders (rendering — native format card/ngày/HTML) ─────────────

    /// Bọc HTML bằng hàm DÙNG CHUNG ở promotionLogic (Android nạp đúng chuỗi này vào WebView) —
    /// gồm cả chuẩn hoá `width` cố định theo `pt` để nội dung CMS không tràn ngang.
    static func contentDisplay(_ raw: String) -> PromotionDetailViewModel.ContentDisplay {
        .init(html: PromotionHtmlContentKt.wrapPromotionHtml(content: raw))
    }

    /// Tiền tố "HSD:" — **khớp Android** (`prm_expiry_short_format`) và khớp luôn màn danh sách iOS
    /// (`MyPromotionCell` dùng `expiryDate`). Trước đây màn này dùng `expiryDateLong` ("Hạn sử dụng …")
    /// nên là chỗ DUY NHẤT lệch chữ. Không parse được ngày → chuỗi rỗng, card tự ẩn dòng.
    /// API không trả HSD (nil/rỗng) → "HSD: Không hết hạn"; có chuỗi mà parse hỏng → rỗng (ẩn dòng).
    /// Đối ứng `PromotionDetailFragment.bindDetailContent` bên Android.
    static func dateString(_ raw: String?) -> String {
        if let date = PRMPromotionDate.parse(raw) {
            return PromotionUIStrings.expiryDate(PRMPromotionDate.display(date))
        }
        return PRMPromotionDate.isMissing(raw) ? PromotionUIStrings.expiryNever : ""
    }
}
