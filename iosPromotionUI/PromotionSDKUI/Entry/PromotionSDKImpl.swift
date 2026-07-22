//
//  PromotionSDKImpl.swift
//  PromotionSDK
//
//  Internal implementation box — keeps all PromotionLogic/PRMPromotionUI types out of
//  PromotionSDK's public class layout and module interface so the consuming app's compiler
//  never needs to load those modules for class metadata generation.
//

import UIKit
@_implementationOnly import PRMPromotionUI
@_implementationOnly import PRMKotlinBridge
@_implementationOnly import PRMDesignKit

// Internal callbacks used by PromotionSDK to communicate back.
typealias OnApplyVoucher = (String) -> Void  // voucherId
typealias OnClearVoucher = () -> Void

final class PromotionSDKImpl: NSObject {

    var onApplyVoucher: OnApplyVoucher?
    var onClearVoucher: OnClearVoucher?
    var onUpdateWidgetCount: ((Int) -> Void)?
    var onClose: (() -> Void)?
    /// Báo host trạng thái bật/tắt SDK (feature flag Unleash) khi đã biết chắc.
    var onAvailabilityUpdate: ((Bool) -> Void)?

    /// Nguồn context duy nhất: session tĩnh + order/dịch vụ động. `updateContext` ghi vào đây,
    /// lõi Kotlin đọc lại ở **mỗi** request. Thay cho `HostRequestContextProvider` + các field rời cũ.
    let context: PromotionMutableContext

    var customerId: String { context.session.customerId }
    var token: String? { context.session.accessToken }

    // Định tuyến qua context để `updateContext` (host cập nhật khi mở widget thanh toán) và luồng
    // build request dùng chung một nguồn — không phải re-init SDK, giữ 1 phiên từ lúc login.
    var orderId: String? {
        get { context.orderId }
        set { context.orderId = newValue }
    }
    var orderValue: String? {
        get { context.orderValue }
        set { context.orderValue = newValue }
    }
    /// Dòng đơn hàng host truyền vào — lưu trong `context` (provider) để store dùng chung đọc được.
    var orderItems: [PromotionOrderItem] {
        get { context.orderItems }
        set { context.orderItems = newValue }
    }
    /// ViewModel widget checkout — bọc `EndowStore` (findEligible + validate&apply + widget-state),
    /// đối ứng Android `PRMEndowViewModel`. Trước đây iOS không có VM cho `PRMEndowView`; nghiệp vụ
    /// (kể cả `loadVouchers`/`setState` tay) dồn ở đây — nay `PromotionSDKImpl` chỉ observe + render.
    private let endowVM = EndowViewModel()

    weak var activeWidget: PRMEndowView?

    // Theo dõi transition để phát callback host đúng một lần (mirror Android `PRMEndowView.notifyHost`):
    // count đổi → onUpdateWidgetCount; chuyển sang APPLIED → onApplyVoucher.
    private var lastNotifiedCount: Int = -1
    private var lastNotifiedApplied = false

    init(options: PromotionSDKOptions) {
        self.context = PromotionMutableContext(session: options.session)
        // Khởi tạo lõi Kotlin qua map public→core (đối ứng `options.toCoreConfig` bên Android).
        // `isDebug`: bản DEBUG in toàn bộ request/response của Ktor ra console để đối chiếu schema thật
        // của server với DTO; bản Release tắt hẳn (không log token) — xem `isDebugBuild`.
        // `availableServices` đã nằm trong config (toCoreConfig) → `ServiceSelectorBuilder` đọc lại
        // từ `PromotionContainer.requireConfig()`, không cần holder Swift riêng (parity Android).
        PromotionContainer.shared.initialize(
            config: options.toCoreConfig(context: context, isDebug: PromotionSDKImpl.isDebugBuild)
        )
        super.init()
        // Nạp cờ tính năng từ server. `refresh()` không ném lỗi: hỏng thì giữ cache (fail-open).
        Task { try? await PromotionFeatureGate.shared.refresh() }
    }

    // MARK: - Theming

    /// Lúc khởi tạo: host truyền theme → áp + lưu; không truyền → khôi phục theme đã lưu.
    /// Phải gọi **sau** `PromotionContainer.initialize` (đã chạy trong init) để `preferences` sẵn sàng.
    func restoreOrApplyTheme(_ theme: PromotionSDKTheme?) {
        if let theme {
            applyTheme(theme)
            PromotionThemeStore.save(theme)
        } else if let saved = PromotionThemeStore.load() {
            applyTheme(saved)
        }
    }

    /// `configure(theme:)`: áp + persist. `nil` = reset và xoá theme đã lưu.
    func applyAndPersistTheme(_ theme: PromotionSDKTheme?) {
        applyTheme(theme)
        if let theme {
            PromotionThemeStore.save(theme)
        } else {
            PromotionThemeStore.clear()
        }
    }

    /// Map public theme (UIColor/CGFloat) → PRMDesignKit registry config. Truyền nil để reset.
    func applyTheme(_ theme: PromotionSDKTheme?) {
        guard let theme else {
            PRMThemeRegistry.shared.clear()
            return
        }
        let config = PRMThemeConfig(
            button: theme.buttonToken.map {
                PRMButtonThemeToken(
                    backgroundColor: $0.backgroundColor,
                    textColor: $0.textColor,
                    shadowColor: $0.shadowColor,
                    cornerRadius: $0.cornerRadius
                )
            },
            searchBar: theme.searchBarToken.map {
                PRMSearchBarThemeToken(
                    borderColor: $0.borderColor,
                    hintTextColor: $0.hintTextColor,
                    textColor: $0.textColor,
                    iconColor: $0.iconColor,
                    cornerRadius: $0.cornerRadius
                )
            },
            listItem: theme.listItemToken.map {
                PRMListItemThemeToken(
                    linkTextColor: $0.linkTextColor,
                    usedBadgeTextColor: $0.usedBadgeTextColor,
                    usedBadgeBackgroundColor: $0.usedBadgeBackgroundColor,
                    radioSelectedColor: $0.radioButtonSelectedStrokeColor,
                    radioUnselectedColor: $0.radioButtonStrokeColor
                )
            },
            tabChip: theme.tabChipToken.map {
                PRMTabChipThemeToken(
                    activeBackgroundColor: $0.activeBackgroundColor,
                    inactiveBackgroundColor: $0.inactiveBackgroundColor,
                    activeTextColor: $0.activeTextColor,
                    inactiveTextColor: $0.inactiveTextColor,
                    cornerRadius: $0.cornerRadius
                )
            },
            tabUnderline: theme.tabUnderlineToken.map {
                PRMTabUnderlineThemeToken(
                    indicatorColor: $0.indicatorColor,
                    activeTextColor: $0.activeTextColor,
                    inactiveTextColor: $0.inactiveTextColor,
                    backgroundColor: $0.backgroundColor
                )
            },
            discountBadge: theme.discountBadgeToken.map {
                PRMDiscountBadgeThemeToken(
                    availableTextColor: $0.availableTextColor,
                    unavailableTextColor: $0.unavailableTextColor,
                    availableBackgroundColor: $0.availableBackgroundColor,
                    unavailableBackgroundColor: $0.unavailableBackgroundColor,
                    actionTextColor: $0.actionTextColor
                )
            }
        )
        PRMThemeRegistry.shared.configure(config)
    }

    /// Map config nội bộ trong registry → public theme (đảo ngược applyTheme).
    func currentTheme() -> PromotionSDKTheme? {
        guard let config = PRMThemeRegistry.shared.current else { return nil }
        return PromotionSDKTheme(
            buttonToken: config.button.map {
                ButtonToken(
                    backgroundColor: $0.backgroundColor, textColor: $0.textColor,
                    shadowColor: $0.shadowColor, cornerRadius: $0.cornerRadius
                )
            },
            searchBarToken: config.searchBar.map {
                SearchBarToken(
                    borderColor: $0.borderColor, hintTextColor: $0.hintTextColor,
                    textColor: $0.textColor, iconColor: $0.iconColor, cornerRadius: $0.cornerRadius
                )
            },
            listItemToken: config.listItem.map {
                ListItemToken(
                    linkTextColor: $0.linkTextColor, usedBadgeTextColor: $0.usedBadgeTextColor,
                    usedBadgeBackgroundColor: $0.usedBadgeBackgroundColor,
                    radioButtonStrokeColor: $0.radioUnselectedColor,
                    radioButtonSelectedStrokeColor: $0.radioSelectedColor
                )
            },
            tabChipToken: config.tabChip.map {
                TabChipToken(
                    activeBackgroundColor: $0.activeBackgroundColor,
                    inactiveBackgroundColor: $0.inactiveBackgroundColor,
                    activeTextColor: $0.activeTextColor, inactiveTextColor: $0.inactiveTextColor,
                    cornerRadius: $0.cornerRadius
                )
            },
            tabUnderlineToken: config.tabUnderline.map {
                TabUnderlineToken(
                    indicatorColor: $0.indicatorColor, activeTextColor: $0.activeTextColor,
                    inactiveTextColor: $0.inactiveTextColor, backgroundColor: $0.backgroundColor
                )
            },
            discountBadgeToken: config.discountBadge.map {
                DiscountBadgeToken(
                    availableTextColor: $0.availableTextColor, unavailableTextColor: $0.unavailableTextColor,
                    availableBackgroundColor: $0.availableBackgroundColor,
                    unavailableBackgroundColor: $0.unavailableBackgroundColor,
                    actionTextColor: $0.actionTextColor
                )
            }
        )
    }

    /// Facade headless: đọc customerId/token thẳng từ `PromotionRequestContextProvider` của lõi
    /// (đối xứng Android), gọi thẳng `PromotionUseCases`; không cần truyền context vào đây.
    func makeApi() -> PromotionSDKApi {
        PromotionSDKApi()
    }

    /// Cập nhật order cho luồng widget thanh toán (eligible + validate dùng các giá trị này).
    func updateOrder(orderId: String?, orderValue: String?, orderItems: [PromotionOrderItem]? = nil) {
        if let orderId { self.orderId = orderId }
        if let orderValue { self.orderValue = orderValue }
        if let orderItems { self.orderItems = orderItems }
    }

    /// Ghi context động — gọi từ `PromotionSDK.updateContext`. Overwrite cả 4 trường (nil = xoá),
    /// đối ứng `PromotionSDK.updateContext` bên Android (ghi thẳng vào `PromotionMutableContext`).
    func updateContext(orderId: String?, orderValue: String?, serviceCode: String?, metaData: String?) {
        context.orderId = orderId
        context.orderValue = orderValue
        context.serviceCode = serviceCode
        context.metaData = metaData
    }

    /// Giải phóng đồ thị DI + reset theme trong bộ nhớ. Đối ứng `PromotionSDK.release()` bên Android:
    /// **không** xoá theme đã lưu (nó sống qua release/init), chỉ reset registry đang chạy.
    func teardown() {
        PromotionContainer.shared.clear()
        applyTheme(nil)
    }


    /// Hai điểm gác của tầng UI, uỷ quyền cho `PromotionFeatureGate` của lõi Kotlin (dùng chung với
    /// Android). Cờ đọc từ cache đồng bộ, không gọi mạng; `completion` về main thread để chỗ gọi
    /// push/present được ngay.
    ///
    /// Tên tính năng chỉ tồn tại ở `PromotionFeatureFlag` bên Kotlin — không có enum nào bên Swift.
    func canOpenVoucherList(_ completion: @escaping (Bool) -> Void) {
        let enabled = PromotionFeatureGate.shared.canOpenVoucherList()
        DispatchQueue.main.async { completion(enabled) }
    }

    func canOpenVoucherDetail(_ completion: @escaping (Bool) -> Void) {
        let enabled = PromotionFeatureGate.shared.canOpenVoucherDetail()
        DispatchQueue.main.async { completion(enabled) }
    }

    /// Popup lỗi nghiệp vụ khi tính năng đang TẮT (PRM_MOB_021) — dùng cho các thao tác UI (bấm mở màn).
    func showFeatureDisabledDialog(on viewController: UIViewController) {
        let message = PromotionSDKError.featureDisabled.errorDescription
            ?? "Tính năng hiện đang tạm thời không khả dụng. Vui lòng thử lại sau."
        PRMConfirmationDialog.showError(message, in: viewController.view)
    }

    /// Mở màn chi tiết ưu đãi theo `voucherId`. Màn tự fetch chi tiết đầy đủ; trong lúc chờ hiện shimmer.
    /// Gate bởi cờ `VOUCHER_DETAIL` (đã gate ngầm bởi master): TẮT → popup lỗi PRM_MOB_021 + báo host.
    /// Có navigationController → push, ngược lại → present modal.
    func openPromotionDetail(voucherId: String, on viewController: UIViewController, navigator: UINavigationController?) {
        canOpenVoucherDetail { [weak self] enabled in
            guard let self else { return }
            guard enabled else {
                self.showFeatureDisabledDialog(on: viewController)
                self.onAvailabilityUpdate?(false)
                return
            }
            let nav = navigator ?? viewController.navigationController ?? (viewController as? UINavigationController)
            // Seed tối thiểu từ voucherId — card trống + shimmer cho tới khi fetch detail xong.
            let seed = PRMPromotionCardSeed(voucherId: voucherId)
            let vc = PromotionDetailBuilder.build(
                with: .init(promotion: seed),
                navigator: nav
            )
            if let nav {
                nav.pushViewController(vc, animated: true)
            } else {
                let wrapper = UINavigationController(rootViewController: vc)
                wrapper.modalPresentationStyle = .fullScreen
                viewController.present(wrapper, animated: true)
            }
        }
    }

    func makeEndowView(presentFrom host: UIViewController, navigator: UINavigationController?) -> UIView {
        self._host = host
        self._navigator = navigator

        // Container gate theo cờ VOUCHER_SELECTION (đã gate ngầm bởi master ENABLE_ALL).
        // FAIL-OPEN: `observe` gọi NGAY (lúc chưa có config → BẬT lạc quan) rồi gọi LẠI khi
        // config thật về. BẬT → dựng widget (idempotent, chỉ 1 lần). TẮT/server báo tắt sau
        // → rút widget + thu height 0. Báo host mỗi lần đổi.
        let container = UIView()
        let collapse = container.heightAnchor.constraint(equalToConstant: 0)
        // Lõi Kotlin không có `observe`. Giữ nguyên ngữ nghĩa: đọc cache NGAY (fail-open → hiện lạc
        // quan), rồi `refresh()` xong gọi LẠI với giá trị thật từ server.
        let applyFlag: (Bool) -> Void = { [weak self, weak container] enabled in
            guard let self, let container else { return }
            self.onAvailabilityUpdate?(enabled)
            guard enabled else {
                // Server báo tắt (kể cả sau khi đã hiện lạc quan) → rút widget lại.
                self.activeWidget?.removeFromSuperview()
                self.activeWidget = nil
                container.isHidden = true
                collapse.isActive = true
                return
            }
            collapse.isActive = false
            container.isHidden = false
            // Đã dựng rồi thì bỏ qua (tránh dựng lại khi `.update` bắn lặp).
            guard self.activeWidget == nil else { return }
            let widget = PRMEndowView()
            widget.delegate = self
            widget.dataSource = self
            self.activeWidget = widget
            widget.translatesAutoresizingMaskIntoConstraints = false
            container.addSubview(widget)
            NSLayoutConstraint.activate([
                widget.topAnchor.constraint(equalTo: container.topAnchor),
                widget.leadingAnchor.constraint(equalTo: container.leadingAnchor),
                widget.trailingAnchor.constraint(equalTo: container.trailingAnchor),
                widget.bottomAnchor.constraint(equalTo: container.bottomAnchor)
            ])
        }

        let gate = PromotionFeatureGate.shared
        // Gọi ngay với cache hiện có (chưa có cache → bật lạc quan).
        applyFlag(gate.canShowVoucherSelection())
        // Rồi làm mới từ server và gọi lại nếu giá trị đổi.
        Task { @MainActor in
            try? await gate.refresh()
            applyFlag(gate.canShowVoucherSelection())
        }
        return container
    }

    /// Bật log body HTTP của lõi Kotlin.
    ///
    /// **Không** dùng `#if DEBUG`: xcframework luôn được archive ở cấu hình Release, nên cờ đó
    /// vĩnh viễn là `false` bên trong SDK dù app host build Debug. Đọc biến môi trường của tiến
    /// trình để bật được cả trên bản phát hành khi cần chẩn đoán:
    ///
    ///     xcrun simctl launch --console booted <bundle-id> PROMOTION_SDK_DEBUG=1
    ///
    /// Mặc định tắt — log body sẽ in cả `Authorization`.
    private static var isDebugBuild: Bool {
        ProcessInfo.processInfo.environment["PROMOTION_SDK_DEBUG"] == "1"
    }

    // Stored weakly to avoid retain cycles — these are UIKit types (fine in module interface context)
    weak var _host: UIViewController?
    weak var _navigator: UINavigationController?

    /// Map `EndowState` (store) → widget-state — **reactive**, mirror Android `PRMEndowView.renderState`.
    /// Quyết định 4 trạng thái khớp `EndowStore.widgetState`; phát callback host đúng một lần theo transition.
    private func render(_ state: EndowState, on view: PRMEndowView) {
        // Giữ trạng thái loading (shimmer) tới khi nạp xong — mirror Android `renderState` (return sớm).
        guard state.hasLoadedInitial else { return }
        if state.discountUnavailable && !state.appliedDiscounts.isEmpty {
            // Hiển thị số tiền của TẤT CẢ ưu đãi đã áp (mờ ở state .unavailable) — khớp Android
            // (`ApplyPromotionAdapter` submit cả list, dim item invalid), nhất quán với nhánh .applied.
            view.setState(.unavailable(voucherTitles: state.appliedDiscounts.map { Self.formatDiscount($0.calculatedDiscount) }))
            lastNotifiedApplied = false
        } else if !state.appliedDiscounts.isEmpty {
            view.setState(.applied(voucherTitles: state.appliedDiscounts.map { Self.formatDiscount($0.calculatedDiscount) }))
            if !lastNotifiedApplied {
                lastNotifiedApplied = true
                if let firstId = state.appliedDiscounts.first?.objectId { onApplyVoucher?(firstId) }
            }
        } else if state.totalVoucherCount > 0 {
            view.setState(.notApplied(count: Int(state.totalVoucherCount)))
            lastNotifiedApplied = false
        } else {
            view.setState(.empty)
            lastNotifiedApplied = false
        }
        let count = Int(state.totalVoucherCount)
        if count != lastNotifiedCount {
            lastNotifiedCount = count
            onUpdateWidgetCount?(count)
        }
    }

    /// Format số tiền giảm (chuỗi số thô) -> "Giảm x.xxxđ".
    private static func formatDiscount(_ raw: String) -> String {
        let digits = raw.filter { $0.isNumber }
        guard let value = Int(digits) else { return raw }
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.groupingSeparator = "."
        let formatted = formatter.string(from: NSNumber(value: value)) ?? "\(value)"
        return "Giảm \(formatted)đ"
    }

    /// Auto-apply voucher `isAutoApplied`: validate qua EndowStore; `render` (observe) lo cập nhật widget
    /// + callback host. Dormant tới khi `findEligible` trả `isAutoApplied` (xem TODO ở attach).
    private func autoApply(_ promotions: [EligibleOffer]) {
        guard !promotions.isEmpty else { return }
        endowVM.validateAndApply(promotions)
    }

    func openChoosePromotion() {
        guard let host = _host else { return }
        let nav = _navigator ?? host.navigationController

        // Preload từ EndowStore (offers đã nạp) để tránh gọi API hai lần — giống Android
        // (`forEndowView` lấy `myVouchers`/`otherVouchers` từ state; lastPage giả định my=false/other=true).
        let endowState = endowVM.state
        let vc = ChoosePromotionBuilder.build(
            with: .init(
                orderItems: context.getOrderItems(),
                preloadedMy: endowState.myOffers,
                preloadedOther: endowState.otherOffers,
                myIsLastPage: false,
                otherIsLastPage: true,
                preSelectedVoucherIds: endowState.appliedDiscounts.map { $0.objectId }
            ),
            navigator: nav
        )
        vc.onApplyVoucher = { [weak self, weak host, weak vc] (promotions: [EligibleOffer]) in
            guard let self, !promotions.isEmpty else { return }

            let pop: () -> Void = {
                if let navCtrl = host?.navigationController {
                    navCtrl.popViewController(animated: true)
                } else {
                    host?.dismiss(animated: true)
                }
            }

            // Bấm "Áp dụng" -> validate qua EndowStore; widget cập nhật qua `render` (observe).
            // Lỗi -> KHÔNG áp; ở lại màn chọn + báo lỗi. Thành công/không-đủ-điều-kiện -> đóng màn.
            self.endowVM.validateAndApply(promotions) { [weak vc] state in
                if state.errorCode != nil {
                    if let vc = vc {
                        PRMConfirmationDialog.showError("Không thể áp dụng ưu đãi lúc này. Vui lòng thử lại.", in: vc.view)
                    }
                    return
                }
                pop()
            }
        }

        if let navCtrl = nav {
            navCtrl.pushViewController(vc, animated: true)
        } else {
            let wrapper = UINavigationController(rootViewController: vc)
            wrapper.modalPresentationStyle = .fullScreen
            host.present(wrapper, animated: true)
        }
    }
}

// MARK: - PRMEndowViewDataSource (internal conformance)

extension PromotionSDKImpl: PRMEndowViewDataSource {
    func selectPromtionViewDidAttachToWindow(_ view: PRMEndowView) {
        // Quan sát EndowStore → render widget **reactive** (mirror Android: `PRMEndowView` collect uiState).
        // Trạng thái áp/không-đủ-điều-kiện persist trong store nên tự khôi phục khi re-attach.
        endowVM.observe { [weak self, weak view] state in
            guard let self, let view else { return }
            self.render(state, on: view)
        }
        endowVM.loadInitial()   // idempotent — store bỏ qua nếu đã nạp
        // TODO(auto-apply): `EligibleOffer` không có `isAutoApplied` (API Find Eligible không trả) →
        // nhánh `autoApply` chưa từng chạy. Android auto-apply từ Search API; cần backend xác nhận.
    }
}

// MARK: - PRMEndowViewDelegate (internal conformance)

extension PromotionSDKImpl: PRMEndowViewDelegate {
    func selectPromtionViewDidTapSelect(_ view: PRMEndowView) {
        switch view.currentState {
        case .applied:
            // Huỷ áp: xoá ở store → `render` (observe) tự đưa widget về NOT_APPLIED/EMPTY.
            endowVM.clearApplied()
            onClearVoucher?()
        default:
            openChoosePromotion()
        }
    }
}
