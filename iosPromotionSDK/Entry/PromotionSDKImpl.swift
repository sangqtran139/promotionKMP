//
//  PromotionSDKImpl.swift
//  PromotionSDK
//
//  Internal implementation box — keeps all PromotionLogic/PRMPromotionUI types out of
//  PromotionSDK's public class layout and module interface so the consuming app's compiler
//  never needs to load those modules for class metadata generation.
//

import UIKit
// Không còn `import PRMPromotionUI`: `PRMEndowView` — thứ duy nhất file này lấy từ đó — nay nằm
// trong chính module (`PromotionSDKUI/Endow/`). Entry giờ chỉ chạm hai package: Kotlin bridge và
// design kit.
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

    /// Nguồn context duy nhất: session tĩnh + order/dịch vụ động. `updateOrderInfo` ghi vào đây,
    /// lõi Kotlin đọc lại ở **mỗi** request. Thay cho `HostRequestContextProvider` + các field rời cũ.
    /// `var` để `updateToken` thay context (session mới) mà vẫn giữ order/dịch vụ đang ghi.
    private(set) var context: PromotionMutableContext

    var token: String? { context.session.accessToken }

    // Định tuyến qua context để `updateOrderInfo` (host cập nhật khi mở widget thanh toán) và luồng
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
    /// đối ứng Android `PRMEndowViewModel`. `PromotionSDKImpl` chỉ observe + render.
    private let endowVM = EndowViewModel()

    weak var activeWidget: PRMEndowView?

    // Theo dõi transition để phát callback host đúng một lần (mirror Android `PRMEndowView.notifyHost`):
    // count đổi → onUpdateWidgetCount; chuyển sang APPLIED → onApplyVoucher.
    /// Quyết định "khi nào bắn callback host" — rule dùng chung ở `promotionLogic`, có test.
    /// Trước đây là `lastNotifiedCount` + `lastNotifiedApplied` rải trong `render`, còn Android có
    /// bản riêng ba biến: cùng hợp đồng public mà hai cách tính.
    private let hostNotifier = EndowHostNotifier()

    init(options: PromotionSDKOptions) {
        self.context = PromotionMutableContext(session: options.session, availableServices: options.availableServices)
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
        initialFlagLoad = Task { try? await PromotionFeatureGate.shared.refresh() }
    }

    /// Task nạp cờ lần đầu, giữ lại để `PromotionSDK.initialize` báo host **sau khi** nó xong.
    /// Không notify thẳng trong `init` được: `wireCallbacks` chạy **sau** khi `init` trả về, nên
    /// `onAvailabilityUpdate` lúc đó còn `nil` và callback đầu tiên sẽ rơi mất.
    private var initialFlagLoad: Task<Void, Never>?

    /// Chờ lần nạp cờ đầu tiên xong rồi báo host công tắc tổng — đây là lúc đầu tiên biết chắc
    /// SDK có được bật hay không. Đối ứng `notifyAvailability()` bên Android.
    func notifyAvailabilityAfterInitialLoad() {
        let load = initialFlagLoad
        Task { @MainActor [weak self] in
            if let load { await load.value }
            self?.onAvailabilityUpdate?(PromotionSDKImpl.isSdkEnabled())
        }
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

    /// Facade headless: đọc token thẳng từ `PromotionRequestContextProvider` của lõi
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

    /// Ghi context động — gọi từ `PromotionSDK.updateOrderInfo`. Overwrite cả 4 trường (nil/rỗng = xoá),
    /// đối ứng `PromotionSDK.updateOrderInfo` bên Android (ghi thẳng vào `PromotionMutableContext`).
    /// Đơn chỉ hỗ trợ **một** dòng sản phẩm nên nhận field phẳng rồi tự bọc thành `[PromotionOrderItem]`
    /// 1 phần tử.
    func updateOrderInfo(orderId: String, productId: String, orderValue: String?, metaData: String?,
                         skuId: String?, productName: String?, productCategory: String?,
                         quantity: Int?, unitPrice: String?) {
        context.orderId = orderId
        context.orderValue = orderValue
        context.metaData = metaData
        context.orderItems = [PromotionOrderItem(
            skuId: skuId ?? "",
            productId: productId,
            productName: productName,
            productCategory: productCategory,
            quantity: quantity ?? 1,
            unitPrice: unitPrice ?? "0"
        )]
    }

    /// Đăng nhập user mới sau khi đã init một lần: đổi token (+ availableServices động),
    /// giữ field cố định đã khoá. Context đơn hàng reset. Đối ứng `PromotionSDK.updateSession` bên Android.
    func updateSession(accessToken: String, availableServices: [PromotionAvailableService]?) {
        let old = context.session
        let newSession = PromotionSessionConfig(
            accessToken: accessToken, baseUrl: old.baseUrl,
            language: old.language, environment: old.environment,
        )
        applySession(newSession, availableServices: availableServices ?? context.availableServices, keepOrderContext: false)
    }

    /// Refresh token giữa phiên (cùng customer, không đổi login) — giữ nguyên context đơn hàng đang
    /// ghi (dùng khi token hết hạn giữa checkout). Đối ứng `PromotionSDK.updateToken` bên Android.
    func updateToken(_ accessToken: String) {
        let old = context.session
        let newSession = PromotionSessionConfig(
            accessToken: accessToken, baseUrl: old.baseUrl,
            language: old.language, environment: old.environment,
        )
        applySession(newSession, availableServices: context.availableServices, keepOrderContext: true)
    }

    /// Dựng lại đồ thị DI với [newSession] + [availableServices]. `keepOrderContext=true` (refresh
    /// token) thì bơm lại order/dịch vụ đang ghi; false (login mới) thì để rỗng.
    /// **Clear trước là bắt buộc** (HttpClient token cũ nằm trong singleton lõi).
    func applySession(_ newSession: PromotionSessionConfig, availableServices: [PromotionAvailableService], keepOrderContext: Bool) {
        let newContext = PromotionMutableContext(session: newSession, availableServices: availableServices)
        if keepOrderContext {
            newContext.orderId = context.orderId
            newContext.orderValue = context.orderValue
            newContext.serviceCode = context.serviceCode
            newContext.metaData = context.metaData
            newContext.orderItems = context.orderItems
        }
        context = newContext

        PromotionContainer.shared.clear()
        PromotionContainer.shared.initialize(config: newContext.toCoreConfig(isDebug: PromotionSDKImpl.isDebugBuild))
        // Callback đã được nối từ lần initialize đầu nên báo thẳng, không cần đợi như lúc init.
        Task { @MainActor [weak self] in
            try? await PromotionFeatureGate.shared.refresh()
            self?.onAvailabilityUpdate?(PromotionSDKImpl.isSdkEnabled())
        }
        // callback + theme giữ nguyên — không đụng.
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

    // MARK: - Feature flag — ranh giới lõi ↔ DTO public
    //
    // `static` chứ không phải instance method: bề mặt feature flag của `PromotionSDK` phải trả lời
    // được **cả khi chưa initialize** (fail-open → bật hết), lúc đó chưa có `impl` nào để hỏi.
    // Đối ứng `PromotionFeatureMapper.kt` bên Android.

    /// Tên cờ thật của lõi Kotlin cho một `PromotionFeature`.
    static func flagName(for feature: PromotionFeature) -> String {
        switch feature {
        case .all: return PromotionFeatureFlag.shared.ENABLE_ALL
        case .voucherList: return PromotionFeatureFlag.shared.VOUCHER_LIST
        case .voucherDetail: return PromotionFeatureFlag.shared.VOUCHER_DETAIL
        case .voucherSelection: return PromotionFeatureFlag.shared.VOUCHER_SELECTION
        case .voucherApply: return PromotionFeatureFlag.shared.VOUCHER_APPLY
        case .voucherRedeem: return PromotionFeatureFlag.shared.VOUCHER_REDEEM
        }
    }

    /// Hai điều kiện phải **song song đúng**: công tắc tổng `isSdkEnabled` bật **và** cờ riêng của
    /// tính năng bật. SDK tắt ⇒ mọi tính năng tắt, không có ngoại lệ.
    ///
    /// `&&` này không đổi kết quả — `PromotionFeatureFlags.isEnabled` của lõi đã tự áp `ENABLE_ALL`
    /// bằng `if (!enableAll) return false`. Viết ra để luật hiện lên ngay tại bề mặt public.
    /// Fail-open giữ nguyên: chưa `initialize()` thì cả hai vế đều `true`.
    static func isFeatureEnabled(_ feature: PromotionFeature) -> Bool {
        isSdkEnabled() && PromotionFeatureGate.shared.isEnabled(flagName: flagName(for: feature))
    }

    static func isSdkEnabled() -> Bool {
        PromotionFeatureGate.shared.isSdkEnabled()
    }

    /// Đọc từng cờ qua `PromotionFeatureGate.isEnabled` chứ **không** đọc thẳng field của
    /// `PromotionFeatureFlags`: công tắc tổng `ENABLE_ALL` chỉ được áp bên trong `isEnabled`
    /// (`if (!enableAll) return false`), field thô thì không. Đọc thẳng field sẽ trả
    /// `voucherList = true` ngay cả khi công tắc tổng đang tắt — host ẩn nhầm/hiện nhầm.
    static func featureFlagsSnapshot() -> PromotionFeatureFlagsSnapshot {
        PromotionFeatureFlagsSnapshot(
            all: isFeatureEnabled(.all),
            voucherList: isFeatureEnabled(.voucherList),
            voucherDetail: isFeatureEnabled(.voucherDetail),
            voucherSelection: isFeatureEnabled(.voucherSelection),
            voucherApply: isFeatureEnabled(.voucherApply),
            voucherRedeem: isFeatureEnabled(.voucherRedeem)
        )
    }

    /// Nạp lại cờ từ server rồi trả snapshot mới trên **main thread**. `refresh()` không ném lỗi
    /// nghiệp vụ; `try?` chỉ để nuốt `CancellationException` đã khai báo cho Swift.
    static func refreshFeatureFlags(_ completion: @escaping (PromotionFeatureFlagsSnapshot) -> Void) {
        Task {
            try? await PromotionFeatureGate.shared.refresh()
            let flags = featureFlagsSnapshot()
            await MainActor.run { completion(flags) }
        }
    }

    /// Báo lỗi nghiệp vụ khi tính năng đang TẮT (PRM_MOB_021) — dùng cho các thao tác UI (bấm mở màn).
    /// Popup **LUÔN hiện** (`PRMConfirmationDialog`); đồng nhất Android
    /// (`PRMBaseConfirmDialog.showFeatureDisabled`).
    func showFeatureDisabledToast(on viewController: UIViewController) {
        let message = PromotionSDKError.featureDisabled.errorDescription
            ?? "Tính năng ưu đãi hiện đang tạm thời không khả dụng. Vui lòng thử lại sau."
        PRMConfirmationDialog.showError(message, in: viewController.view)
    }

    /// Mở màn chi tiết ưu đãi theo `voucherId`. Màn tự fetch chi tiết đầy đủ; trong lúc chờ hiện shimmer.
    /// Gate bởi cờ `VOUCHER_DETAIL` (đã gate ngầm bởi master): TẮT → toast lỗi PRM_MOB_021 + báo host.
    /// Có navigationController → push, ngược lại → present modal.
    ///
    /// `returnVoucherOnApply` đi thẳng xuống `PromotionDetailBuilder.DataModel` — không còn enum
    /// trung gian ở cả hai nền tảng: nơi mở màn có thể là màn bất kỳ của host, nên cờ đặt tên theo
    /// *hành vi* ("tôi tự nhận voucher" / "để SDK điều hướng chọn dịch vụ") thay vì theo *màn gọi*.
    func openPromotionDetail(
        voucherId: String,
        on viewController: UIViewController,
        navigator: UINavigationController?,
        returnVoucherOnApply: Bool = true,
        hostHandlesDismiss: Bool = false,
        onVoucherApplied: ((PromotionVoucherDetail) -> Void)? = nil,
        onFeatureDisabled: (() -> Void)? = nil
    ) {
        canOpenVoucherDetail { [weak self] enabled in
            guard let self else { return }
            guard enabled else {
                // Host có đăng ký thì trả cho host, không thì SDK tự hiện popup.
                if let onFeatureDisabled {
                    onFeatureDisabled()
                } else {
                    self.showFeatureDisabledToast(on: viewController)
                }
                return
            }
            let nav = navigator ?? viewController.navigationController ?? (viewController as? UINavigationController)
            // Seed tối thiểu từ voucherId — card trống + shimmer cho tới khi fetch detail xong.
            let seed = PRMPromotionCardSeed(voucherId: voucherId)
            let vc = PromotionDetailBuilder.build(
                with: .init(
                    promotion: seed,
                    returnVoucherOnApply: returnVoucherOnApply,
                    hostHandlesDismiss: hostHandlesDismiss,
                    // Map domain -> DTO ở đây, ranh giới public (đối ứng `PromotionSDK` bên Android).
                    onVoucherApplied: onVoucherApplied.map { host in
                        { detail in host(PromotionSDKApi.toVoucherDetail(detail)) }
                    }
                ),
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
            // Qua notifier: `applyFlag` chạy HAI lần (cache rồi server) nên gọi thẳng là host nhận
            // callback trùng khi cờ không đổi. Android vốn đã chặn bằng `lastNotifiedAvailability`;
            // nay cả hai bên dùng chung một rule.
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

        // Cờ nào, đọc thế nào — do store quyết (dùng chung Android), native chỉ hỏi.
        // Gọi ngay với cache hiện có (chưa có cache → bật lạc quan).
        applyFlag(endowVM.availabilityFromCache())
        // Rồi làm mới từ server và gọi lại nếu giá trị đổi.
        Task { @MainActor in
            applyFlag(await endowVM.refreshAvailability())
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

    /// Bấm "Thanh toán" của host — uỷ thẳng xuống `EndowStore.confirmRedemption` (dùng chung Android).
    func confirmRedemption(onSuccess: @escaping () -> Void, onError: @escaping (PromotionSDKError) -> Void) {
        // Không còn `[weak self]`: closure không đụng tới `self` nữa kể từ khi bỏ popup PRM_MOB_021.
        endowVM.confirmRedemption { result in
            if let failure = result as? EndowConfirmResultFailure {
                // KHÔNG hiện gì, kể cả `PRM_MOB_021` (cờ `VOUCHER_REDEEM` tắt).
                //
                // Khác hẳn các điểm gác khác (`openMyPromotion`, `openPromotionDetail`): ở đó user vừa
                // bấm để MỞ một tính năng, không nói gì thì màn hình đứng im vô lý. Còn đây là giữa
                // luồng THANH TOÁN của host — SDK chen một thông báo của mình vào là cướp quyền điều
                // khiển, trong khi host mới là bên biết phải dừng hay đi tiếp và hiện gì.
                //
                // Cờ tắt vẫn KHÔNG gọi API (`EndowStore.confirmRedemption` chặn trước) và vẫn trả đúng
                // mã lỗi ra đây — chỉ bỏ phần hiển thị. Đối ứng `PRMEndowView.confirmRedemption` Android.
                //
                // Trả **kiểu công khai**, không phải mã thô: host `switch` là xong, không phải so
                // chuỗi. Đối ứng `PRMEndowView.onError` bên Android.
                onError(PromotionSDKError.from(failure.errorCode))
            } else {
                onSuccess()
            }
        }
    }

    /// Map `EndowState` (store) → widget-state — **reactive**, mirror Android `PRMEndowView.renderState`.
    ///
    /// Quyết định 4 trạng thái lấy THẲNG từ `EndowState.widgetState` (rule dùng chung ở
    /// `promotionLogic`, 8 test phủ ở `EndowStoreTest`). Trước đây hàm này chép lại nguyên 4 nhánh
    /// bằng Swift — sửa rule bên Kotlin thì Android đổi theo còn iOS lặng lẽ giữ hành vi cũ.
    ///
    /// Ở đây chỉ còn phần **thật sự của native**: format chuỗi tiền và phát callback host theo
    /// transition.
    private func render(_ state: EndowState, on view: PRMEndowView) {
        // Widget không đi qua `PRMStoreViewModel.emitErrorIfNeeded` (đọc thẳng state, `autoConsumesError`
        // tắt) nên phải tự bắt TOKEN_EXPIRED ở đây — 4 màn Store khác đã có base lo hộ. Đối ứng
        // `PRMEndowView.renderState` bên Android (xử lý TRƯỚC guard `hasLoadedInitial`, vì `loadInitial()`
        // hỏng cũng set `hasLoadedInitial = true` kèm `errorCode`).
        if let errorCode = state.errorCode {
            if errorCode == PromotionErrorCodes.shared.TOKEN_EXPIRED {
                PromotionSDK.getCallback()?.onExpireToken()
            }
            endowVM.consumeError()
        }

        // Giữ trạng thái loading (shimmer) tới khi nạp xong — mirror Android `renderState` (return sớm).
        guard state.hasLoadedInitial else { return }

        // Hiển thị TẤT CẢ ưu đãi đã áp (mờ ở state .unavailable) — khớp Android
        // (`ApplyPromotionAdapter` submit cả list, dim item invalid), nhất quán với nhánh .applied.
        let titles = { state.appliedDiscounts.map(Self.appliedTitle) }

        switch state.widgetState {
        case .unavailable: view.setState(.unavailable(voucherTitles: titles()))
        case .applied:     view.setState(.applied(voucherTitles: titles()))
        case .notApplied:  view.setState(.notApplied(count: Int(state.totalVoucherCount)))
        case .empty:       view.setState(.empty)
        // Kotlin thêm case mới mà iOS chưa cập nhật → về EMPTY thay vì không compile được.
        // Cùng cách xử lý với `ChooseSeeMoreState.toSeeMoreState()`.
        default:           view.setState(.empty)
        }

        emit(hostNotifier.onState(state: state))
    }

    /// Map `EndowHostEvent` dùng chung → callback public của iOS.
    ///
    /// Thứ tự do notifier quyết định: **count trước, applied sau**. Bản cũ ở đây bắn ngược lại
    /// (applied trong `switch`, count sau cùng) — host nào dựa vào thứ tự đó sẽ thấy đổi.
    private func emit(_ events: [EndowHostEvent]) {
        // Cast `as?` cho khớp cách file này vẫn xử lý sealed interface của Kotlin
        // (`result as? EndowConfirmResultFailure`, `effect as? PRMEffectShowError`).
        for event in events {
            if let applied = event as? EndowHostEventVoucherApplied {
                onApplyVoucher?(applied.voucherId)
            }
        }
    }

    /// Format số tiền giảm (chuỗi số thô) -> "Giảm x.xxxđ".
    /// Chữ trên chip của widget: `tags[0]` (nhãn server) → số tiền giảm.
    ///
    /// Trước đây bên này **chỉ** format số tiền — không có nhánh `tags` mà Android có — nên nhãn
    /// server gửi kèm bị bỏ qua hoàn toàn. Nay khớp đúng Android.
    ///
    /// Đối ứng `ApplyPromotionAdapter.bind` bên Android — sửa bên nào thì sửa cả bên kia.
    private static func appliedTitle(_ discount: EndowAppliedDiscount) -> String {
        if let tag = discount.tags.first, !tag.isEmpty { return tag }
        return formatDiscount(discount.calculatedDiscount)
    }

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
        // (`forEndowView` lấy `myVouchers`/`otherVouchers` + cờ phân trang từ state).
        // `currentState` (đồng bộ) chứ không phải `state`: đọc ngay lúc user bấm, phải là bản mới nhất.
        let endowState = endowVM.currentState
        let vc = ChoosePromotionBuilder.build(
            with: .init(
                orderItems: context.getOrderItems(),
                preloadedMy: endowState.myOffers,
                preloadedOther: endowState.otherOffers,
                myIsLastPage: endowState.myIsLastPage,
                otherIsLastPage: endowState.otherIsLastPage,
                preSelectedVoucherIds: endowState.appliedDiscounts.map { $0.objectId }
            ),
            navigator: nav
        )
        vc.onApplyVoucher = { [weak self, weak host, weak vc] (promotions: [EligibleOffer], onSettled: @escaping () -> Void) in
            // `onSettled` mở khoá nút "Áp dụng" (chống spam trong lúc chờ validate) → phải gọi ở MỌI
            // đường ra, kể cả nhánh guard này, nếu không nút khoá vĩnh viễn.
            guard let self, !promotions.isEmpty else {
                onSettled()
                return
            }

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
                onSettled()
                if let errorCode = state.errorCode {
                    // Validate hỏng → **popup** (không phải toast): user vừa bấm "Áp dụng" và đang
                    // chờ kết quả, toast trôi mất thì tưởng đã áp xong. Popup buộc phải bấm "Đóng".
                    // Màn "Chọn ưu đãi" **ở lại** để user chọn lại hoặc thoát chủ động.
                    // Android tạm thời vẫn dùng toast (`ChoosePromotionFragment`).
                    //
                    // Mã lỗi → chuỗi hiển thị dùng chung mọi màn, đối ứng `mapPromotionError`.
                    if let vc = vc {
                        PRMConfirmationDialog.showError(
                            PromotionUIStrings.errorMessage(errorCode),
                            in: vc.view
                        )
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
