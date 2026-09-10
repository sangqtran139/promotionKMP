//
//  PromotionDetailViewModel.swift
//  PromotionSDK
//
//  Bọc PromotionDetailStore — phần dùng chung (giữ store, observe state, effect) nằm ở
//  `PRMStoreViewModel`. Ở đây chỉ còn thứ riêng của màn: FORMAT hiển thị (card/HTML/ngày), vốn là
//  việc của native. Bên Android việc format này nằm thẳng trong `PromotionDetailFragment`.
//
//  KHÔNG seed từ màn danh sách: mọi thứ hiển thị đều đến từ `getCustomerVoucherDetail`; trong lúc
//  chờ là `Display.initial` + shimmer. `data.promotion` chỉ dùng để lấy `voucherId` cần fetch.
//

import Foundation
@_implementationOnly import PRMDesignKit
@_implementationOnly import PRMKotlinBridge

final class PromotionDetailViewModel:
    PRMScreenViewModel<PromotionDetailRouter, PromotionDetailStore> {

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

    /// **View model đã format** cho VC: card, HTML đã bọc, nhãn nút. KHÔNG phải state của store —
    /// state gốc là `PromotionDetailState` (dùng chung với Android). Bên Android việc format này nằm
    /// thẳng trong `PromotionDetailFragment.bindDetailContent`; iOS gom vào VM cho VC mỏng, nên nó
    /// còn tồn tại ở đây chứ không bị bỏ như `UiState` bên kia.
    struct Display {
        var card: VoucherCardViewModel
        var banner: String?
        var tabContents: TabContents
        var applyTitle: String
        var isApplyEnabled: Bool
        var isApplyVisible: Bool
        var isLoading: Bool

        /// Trước khi có detail — trống hoàn toàn, shimmer che ở VC.
        static let initial = Display(
            card: VoucherCardViewModel(title: "", description: "", logoURL: nil, date: ""),
            banner: nil,
            tabContents: .empty,
            applyTitle: "",
            isApplyEnabled: false,
            isApplyVisible: false,
            isLoading: true
        )
    }

    /// Id voucher dùng cho callback "Áp dụng".
    let voucherId: String
    let data: PromotionDetailBuilder.DataModel

    /// Kênh phát **view model đã format**. State thô + effect do [PRMStoreViewModel] lo; ở đây chỉ
    /// thêm một tầng format vì màn này phải dựng HTML/card/ngày trước khi VC nạp được.
    private(set) var display = Display.initial {
        didSet { onDisplay?(display) }
    }
    var onDisplay: ((Display) -> Void)? {
        didSet { onDisplay?(display) }
    }

    private var didStart = false
    /// Dịch vụ/sản phẩm voucher áp dụng được — chỉ có sau khi detail về.
    private var applicableProducts: [ApplicableProduct] = []
    /// Chi tiết mới nhất từ store — nguồn cho `notifyVoucherApplied()`. Nil khi chưa/không có data.
    private var currentDetail: VoucherDetail?

    init(router: PromotionDetailRouter,
         data: PromotionDetailBuilder.DataModel,
         getDetailUseCase: GetCustomerVoucherDetailUseCase = GetCustomerVoucherDetailUseCase()) {
        self.data = data
        self.voucherId = data.promotion.id
        super.init(router: router,
                   store: PromotionDetailStore(getCustomerVoucherDetailUseCase: getDetailUseCase))
        // Base phát state thô; màn này còn phải format nên bắc thêm một nhịp sang `display`.
        onState = { [weak self] state in self?.render(state) }
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

    // ─── Intent ───────────────────────────────────────────────────────────────
    /// VC gọi một lần lúc bind; `didStart` chặn fetch lặp khi màn được bind lại.
    func loadDetailIfNeeded() {
        guard !didStart else { return }
        didStart = true
        dispatch(PromotionDetailIntentLoadDetail(voucherId: voucherId))
    }

    // ─── State → View ─────────────────────────────────────────────────────────
    /// Chỉ hiển thị khi API detail trả về. Không có detail → về **khung rỗng** (shimmer che lúc đang
    /// tải; hết tải thì card/tab trống, nút ẩn) — đối ứng `PromotionDetailFragment.bindEmptyContent()`
    /// bên Android. `detail == nil` ở lần load sau (API lỗi / voucher biến mất) cũng về khung rỗng,
    /// không giữ lại nội dung voucher cũ.
    private func render(_ state: PromotionDetailState) {
        guard let detail = state.detail else {
            applicableProducts = []
            currentDetail = nil
            var empty = Display.initial
            empty.isLoading = state.isLoading
            display = empty
            return
        }
        applicableProducts = detail.applicableProducts
        currentDetail = detail
        // Nhãn nút theo nơi mở màn (TLNV MOB_002 control #5) — chuỗi SDK, không dùng nhãn server.
        display = state.toDisplay(
            detail: detail,
            applyTitle: returnVoucherOnApply ? PromotionUIStrings.apply : PromotionUIStrings.useNow
        )
    }

    // ─── Bottom sheet "Chọn dịch vụ" (render native; lọc dùng chung ở promotionLogic) ──
    /// Đối ứng `PromotionDetailViewModel.serviceOptions()` bên Android. Luật **1 dịch vụ → chọn
    /// thẳng, không mở sheet** (TLNV MOB_002 control #5) nằm trong `ServiceSelectorBottomSheet.present`.
    func serviceOptions() -> [ServiceSelectorItem] {
        ServiceSelectorBuilder.items(forApplicableProducts: applicableProducts)
    }

}

// ─── Map state dùng chung (store) → model UI iOS ──────────────────────────────

/// Chiếu state dùng chung ([PromotionDetailState]) → **view model đã format** (`Display`).
///
/// Nhận [detail] đã unwrap: iOS format sẵn card/HTML/ngày nên chỉ dựng được khi đã có detail —
/// chưa có thì VM giữ `Display.initial`.
private extension PromotionDetailState {
    func toDisplay(detail: VoucherDetail, applyTitle: String) -> PromotionDetailViewModel.Display {
        PromotionDetailViewModel.Display(
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

    /// Tiền tố "HSD:" — khớp Android (`prm_expiry_short_format`) và khớp màn danh sách iOS
    /// (`MyPromotionCell` dùng `expiryDate`). Không parse được ngày → chuỗi rỗng, card tự ẩn dòng.
    /// API không trả HSD (nil/rỗng) → "HSD: Không hết hạn"; có chuỗi mà parse hỏng → rỗng (ẩn dòng).
    /// Đối ứng `PromotionDetailFragment.bindDetailContent` bên Android.
    static func dateString(_ raw: String?) -> String {
        if let date = PRMPromotionDate.parse(raw) {
            return PromotionUIStrings.expiryDate(PRMPromotionDate.display(date))
        }
        return PRMPromotionDate.isMissing(raw) ? PromotionUIStrings.expiryNever : ""
    }
}
