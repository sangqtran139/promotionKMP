//
//  PromotionDetailViewModel.swift
//  PromotionSDK
//
//  Lớp bọc mỏng quanh PromotionDetailStore (tầng UI-logic dùng chung ở promotionLogic).
//  ĐỒNG NHẤT với `PromotionDetailViewModel` bên Android — cùng `store` / `bindStore` / `render` /
//  `handleError` + forward intent cùng thứ tự (xem `MyPromotionViewModel`). Fetch + quyết định nút
//  nằm ở store; VM chỉ FORMAT hiển thị (card/HTML/ngày) — phần rendering vốn là của native.
//
//  KHÔNG seed từ màn danh sách: mọi thứ hiển thị đều đến từ `getCustomerVoucherDetail`; trong lúc
//  chờ thì `Display.empty` + shimmer. `data.promotion` chỉ dùng để lấy `voucherId` cần fetch.
//

import Foundation
import Combine
@_implementationOnly import PRMKotlinBridge

final class PromotionDetailViewModel: PRMBaseViewModel<PromotionDetailRouter>, PRMViewModelType {

    struct Input {}

    /// Nội dung hiển thị theo tab cùng cờ cho biết text có phải HTML không.
    struct ContentDisplay {
        let text: String
        let isHTML: Bool
    }

    /// Nội dung cả 2 tab, render đồng thời vào 2 trang vuốt được.
    struct TabContents {
        let detail: ContentDisplay
        let guide: ContentDisplay
    }

    struct Output {
        let voucherCardViewModel: AnyPublisher<VoucherCardViewModel, Never>
        let bannerImageName: AnyPublisher<String?, Never>
        let tabContents: AnyPublisher<TabContents, Never>
        let applyButtonTitle: AnyPublisher<String, Never>
        let isApplyEnabled: AnyPublisher<Bool, Never>
        let isApplyVisible: AnyPublisher<Bool, Never>
        let isLoading: AnyPublisher<Bool, Never>
        /// Phát **mã lỗi** (raw) — VC map code → chuỗi (đồng nhất Android/MyPromotion).
        let errorCode: AnyPublisher<String, Never>
    }

    /// Gói dữ liệu hiển thị — chỉ dựng từ detail mà API trả về (không seed từ màn danh sách).
    private struct Display {
        let card: VoucherCardViewModel
        let banner: String?
        let detailContent: String
        let guideContent: String
        let applyTitle: String
        let applyEnabled: Bool
        let applyVisible: Bool

        /// Trạng thái trước khi có detail — trống hoàn toàn, shimmer che ở VC.
        static let empty = Display(
            card: VoucherCardViewModel(title: "", description: "", logoURL: nil, date: ""),
            banner: nil,
            detailContent: "",
            guideContent: "",
            applyTitle: "",
            applyEnabled: false,
            applyVisible: false
        )
    }

    /// Id voucher dùng cho callback "Áp dụng".
    let voucherId: String
    let data: PromotionDetailBuilder.DataModel

    // ─── Store ────────────────────────────────────────────────────────────────
    private let store: PromotionDetailStore
    /// Bề mặt view (đã format) — trống cho tới khi store phát detail (shimmer che trong lúc chờ).
    private let displaySubject = CurrentValueSubject<Display, Never>(.empty)
    private let isLoadingSubject = CurrentValueSubject<Bool, Never>(true)
    private let errorSubject = PassthroughSubject<String, Never>()
    private var storeCancellable: PromotionCancellable?
    private var didStart = false
    /// Dịch vụ/sản phẩm voucher áp dụng được — chỉ có sau khi detail về.
    private var applicableProducts: [ApplicableProduct] = []

    init(router: PromotionDetailRouter,
         data: PromotionDetailBuilder.DataModel,
         getDetailUseCase: GetCustomerVoucherDetailUseCase = GetCustomerVoucherDetailUseCase()) {
        self.data = data
        self.store = PromotionDetailStore(getCustomerVoucherDetailUseCase: getDetailUseCase)
        self.voucherId = data.promotion.id
        super.init(router: router)
    }

    deinit {
        storeCancellable?.cancel()
        store.clear()
    }

    /// Danh sách dịch vụ cho bottom sheet "Chọn dịch vụ" (thuần iOS — VC gọi).
    func serviceSelectorItems() -> [ServiceSelectorItem] {
        ServiceSelectorBuilder.items(forApplicableProducts: applicableProducts)
    }

    func transform(input: Input) -> Output {
        bindStore()
        forward(input)
        return buildOutput()
    }

    // ─── Store observation (đối ứng Android.bindStore) ──────────────────────────
    private func bindStore() {
        storeCancellable = observeStore(watch: { self.store.watchState(onEach: $0) }) { [weak self] state in
            guard let self = self else { return }
            self.render(state)
            self.handleError(state)
        }
    }

    // ─── Intent forwarding (đối ứng Android.handleAction) ───────────────────────
    private func forward(_ input: Input) {
        // iOS tự kích load lần đầu (VC gọi transform một lần); Android do Fragment kích LoadDetail.
        if !didStart {
            didStart = true
            store.dispatch(intent: PromotionDetailIntentLoadDetail(voucherId: voucherId))
        }
    }

    // ─── State → View ─────────────────────────────────────────────────────────
    private func render(_ state: PromotionDetailState) {
        isLoadingSubject.send(state.isLoading)
        // Chỉ hiển thị khi API detail trả về; chưa có thì giữ `Display.empty` (shimmer che).
        if let detail = state.detail {
            applicableProducts = detail.applicableProducts
            displaySubject.send(Self.display(from: detail, state: state))
        }
    }

    // ─── Error ──────────────────────────────────────────────────────────────────
    private func handleError(_ state: PromotionDetailState) {
        guard let code = state.errorCode else { return }
        errorSubject.send(code)   // view map code → chuỗi
        store.dispatch(intent: PromotionDetailIntentConsumeError.shared)
    }

    /// Chiếu display → bề mặt view iOS (Output). Đối ứng Android `PromotionDetailState.toUiState()`.
    private func buildOutput() -> Output {
        let display = displaySubject.receive(on: DispatchQueue.main)
        let tabContents = display.map { d -> TabContents in
            TabContents(detail: Self.contentDisplay(d.detailContent), guide: Self.contentDisplay(d.guideContent))
        }
        return Output(
            voucherCardViewModel: display.map { $0.card }.eraseToAnyPublisher(),
            bannerImageName: display.map { $0.banner }.eraseToAnyPublisher(),
            tabContents: tabContents.eraseToAnyPublisher(),
            applyButtonTitle: display.map { $0.applyTitle }.eraseToAnyPublisher(),
            isApplyEnabled: display.map { $0.applyEnabled }.eraseToAnyPublisher(),
            isApplyVisible: display.map { $0.applyVisible }.eraseToAnyPublisher(),
            isLoading: isLoadingSubject.receive(on: DispatchQueue.main).eraseToAnyPublisher(),
            errorCode: errorSubject.receive(on: DispatchQueue.main).eraseToAnyPublisher()
        )
    }

    // ─── Display builders (rendering — native format card/ngày/HTML) ─────────────

    /// Nội dung 1 tab: rỗng → để trống (không text mặc định); có → render HTML.
    private static func contentDisplay(_ raw: String) -> ContentDisplay {
        raw.isEmpty ? ContentDisplay(text: "", isHTML: false) : ContentDisplay(text: raw, isHTML: true)
    }

    /// Card đầy đủ từ detail; nút lấy **quyết định từ store** (actionVisible/Enabled + label server).
    private static func display(from detail: VoucherDetail, state: PromotionDetailState) -> Display {
        Display(
            card: VoucherCardViewModel(
                title: detail.merchantName ?? "",
                description: detail.title ?? "",
                logoURL: detail.logo,
                date: dateString(detail.expirationDate)
            ),
            banner: detail.banner,
            detailContent: detail.description_ ?? "",
            guideContent: detail.guideline ?? "",
            applyTitle: state.actionEnabled ? PromotionUIStrings.useNow : state.actionLabel,
            applyEnabled: state.actionEnabled,
            applyVisible: state.actionVisible
        )
    }

    private static func dateString(_ raw: String?) -> String {
        PRMPromotionDate.parse(raw).map { PromotionUIStrings.expiryDateLong(PRMPromotionDate.display($0)) } ?? ""
    }
}
