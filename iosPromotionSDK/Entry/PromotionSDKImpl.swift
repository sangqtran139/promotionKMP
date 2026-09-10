//
//  PromotionSDKImpl.swift
//  PromotionSDK
//
//  Internal implementation box — keeps all PromotionLogic/PRMPromotionUI types out of
//  PromotionSDK's public class layout and module interface so the consuming app's compiler
//  never needs to load those modules for class metadata generation.
//

import UIKit
// Không còn `import PRMPromotionUI`: `PRMOfferWidget` — thứ duy nhất file này lấy từ đó — nay nằm
// trong chính module (`PromotionKit/OfferWidget/`). Entry giờ chỉ chạm hai package: Kotlin bridge và
// design kit.
@_implementationOnly import PRMKotlinBridge
@_implementationOnly import PRMDesignKit

// Internal callbacks used by PromotionSDK to communicate back.
typealias OnApplyVoucher = (String) -> Void  // voucherId

/// `@MainActor` theo `PromotionSDK` (bề mặt public) và `PRMStoreViewModel`.
///
/// Toàn bộ class này là tầng UI: nó giữ widget đang hiển thị (`activeWidget`), điều hướng
/// (`push`/`present`/`pop`), dựng view controller, và gọi callback về host. Không có phần nào chạy
/// nền — phần nền nằm ở lõi Kotlin, và mọi lối vào từ đó đã hop main sẵn.
@MainActor
final class PromotionSDKImpl: NSObject {

    /// Cầu nối duy nhất từ SDK ra callback của host — `PromotionSDK.wireCallbacks` gán nó.
    ///
    /// Bốn closure từng đứng cạnh đây đã **xoá**, vì `wireCallbacks` chưa bao giờ gán cái nào: hai
    /// cái có chỗ gọi nhưng không ai nghe (huỷ áp voucher, cờ khả dụng), hai cái không gán cũng
    /// không gọi (đếm voucher của widget, đóng màn). Cả bốn ứng với những sự kiện mà
    /// `PromotionSDKCallback` **cố ý không có** — xem `docs/common/InitParity.md` §3 mục "Đã loại".
    /// Giữ lại chỉ tạo cảm giác SDK có 5 kênh ra host trong khi chỉ có một.
    var onApplyVoucher: OnApplyVoucher?

    /// Nguồn context duy nhất: session tĩnh + order/dịch vụ động. `updateOrderInfo` ghi vào đây,
    /// lõi Kotlin đọc lại ở **mỗi** request. Thay cho `HostRequestContextProvider` + các field rời cũ.
    /// `let`: context sống suốt vòng đời một phiên. Không còn đường nào thay nó — token đổi thì
    /// kho của host đổi, SDK đọc lại qua `PromotionTokenSource` chứ không dựng lại gì.
    let context: PromotionMutableContext

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
    /// ViewModel widget checkout — bọc `OfferWidgetStore` (findEligible + validate&apply + widget-state),
    /// đối ứng Android `PRMOfferWidgetViewModel`. `PromotionSDKImpl` chỉ observe + render.
    private let offerWidgetVM = OfferWidgetViewModel()

    weak var activeWidget: PRMOfferWidget?

    // Theo dõi transition để phát callback host đúng một lần (mirror Android `PRMOfferWidget.notifyHost`):
    // chuyển sang APPLIED → onApplyVoucher. (Nhánh "count đổi" đã bỏ cùng closure của nó.)
    /// Quyết định "khi nào bắn callback host" — rule dùng chung ở `promotionLogic`, có test.
    /// Trước đây là `lastNotifiedCount` + `lastNotifiedApplied` rải trong `render`, còn Android có
    /// bản riêng ba biến: cùng hợp đồng public mà hai cách tính.
    private let hostNotifier = OfferWidgetHostNotifier()

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

    /// Task nạp cờ lần đầu. Giữ tham chiếu để nó không bị huỷ ngay khi `init` trả về.
    ///
    /// Trước đây còn một `notifyAvailabilityAfterInitialLoad()` chờ task này rồi báo host công tắc
    /// tổng, kèm cả comment giải thích rất kỹ vì sao phải gọi **sau** `wireCallbacks` để "callback
    /// đầu tiên không rơi mất". Comment đó nói đúng về một cái bẫy, cho một closure chưa bao giờ
    /// được gán. Đã xoá cả hai — host đọc cờ qua `PromotionSDK.refreshFeatureFlags`.
    private var initialFlagLoad: Task<Void, Never>?

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
                PRMButtonToken(
                    backgroundColor: $0.backgroundColor, textColor: $0.textColor,
                    shadowColor: $0.shadowColor, cornerRadius: $0.cornerRadius
                )
            },
            searchBarToken: config.searchBar.map {
                PRMSearchBarToken(
                    borderColor: $0.borderColor, hintTextColor: $0.hintTextColor,
                    textColor: $0.textColor, iconColor: $0.iconColor, cornerRadius: $0.cornerRadius
                )
            },
            listItemToken: config.listItem.map {
                PRMListItemToken(
                    linkTextColor: $0.linkTextColor, usedBadgeTextColor: $0.usedBadgeTextColor,
                    usedBadgeBackgroundColor: $0.usedBadgeBackgroundColor,
                    radioButtonStrokeColor: $0.radioUnselectedColor,
                    radioButtonSelectedStrokeColor: $0.radioSelectedColor
                )
            },
            tabChipToken: config.tabChip.map {
                PRMTabChipToken(
                    activeBackgroundColor: $0.activeBackgroundColor,
                    inactiveBackgroundColor: $0.inactiveBackgroundColor,
                    activeTextColor: $0.activeTextColor, inactiveTextColor: $0.inactiveTextColor,
                    cornerRadius: $0.cornerRadius
                )
            },
            tabUnderlineToken: config.tabUnderline.map {
                PRMTabUnderlineToken(
                    indicatorColor: $0.indicatorColor, activeTextColor: $0.activeTextColor,
                    inactiveTextColor: $0.inactiveTextColor, backgroundColor: $0.backgroundColor
                )
            },
            discountBadgeToken: config.discountBadge.map {
                PRMDiscountBadgeToken(
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
                         skuSourceId: String?, productName: String?, productCategory: String?,
                         quantity: Int?, unitPrice: String?) {
        context.orderId = orderId
        context.orderValue = orderValue
        context.metaData = metaData
        context.orderItems = [PromotionOrderItem(
            skuSourceId: skuSourceId ?? "",
            productId: productId,
            productName: productName,
            productCategory: productCategory,
            quantity: quantity ?? 1,
            unitPrice: unitPrice ?? "0"
        )]
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
    /// Tên hàm nói **đúng** thứ nó làm. Bản cũ tên `…Toast` trong khi thân hàm gọi
    /// `PRMConfirmationDialog` — một popup có nút "Đóng", chặn thao tác. Toast và dialog khác hẳn
    /// nhau về UX; đọc tên mà tưởng là toast thì sẽ đặt nó vào những chỗ không được phép chặn.
    /// Kèm theo đó `PRMToast` là code chết (0 chỗ dùng) nên đã xoá — không còn toast nào trong SDK.
    func showFeatureDisabledDialog(on viewController: UIViewController) {
        let message = PromotionSDKError.featureDisabled.errorDescription
            ?? PromotionUIStrings.featureDisabled
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
                    self.showFeatureDisabledDialog(on: viewController)
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

    func makeOfferWidget(presentFrom host: UIViewController, navigator: UINavigationController?) -> UIView {
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
            let widget = PRMOfferWidget()
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
        applyFlag(offerWidgetVM.availabilityFromCache())
        // Rồi làm mới từ server và gọi lại nếu giá trị đổi.
        Task { @MainActor in
            applyFlag(await offerWidgetVM.refreshAvailability())
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

    /// Bấm "Thanh toán" của host — uỷ thẳng xuống `OfferWidgetStore.confirmRedemption` (dùng chung Android).
    func confirmRedemption(onSuccess: @escaping () -> Void, onError: @escaping (PromotionSDKError) -> Void) {
        // Không còn `[weak self]`: closure không đụng tới `self` nữa kể từ khi bỏ popup PRM_MOB_021.
        offerWidgetVM.confirmRedemption { result in
            if let failure = result as? OfferWidgetConfirmResultFailure {
                // KHÔNG hiện gì, kể cả `PRM_MOB_021` (cờ `VOUCHER_REDEEM` tắt).
                //
                // Khác hẳn các điểm gác khác (`openMyPromotion`, `openPromotionDetail`): ở đó user vừa
                // bấm để MỞ một tính năng, không nói gì thì màn hình đứng im vô lý. Còn đây là giữa
                // luồng THANH TOÁN của host — SDK chen một thông báo của mình vào là cướp quyền điều
                // khiển, trong khi host mới là bên biết phải dừng hay đi tiếp và hiện gì.
                //
                // Cờ tắt vẫn KHÔNG gọi API (`OfferWidgetStore.confirmRedemption` chặn trước) và vẫn trả đúng
                // mã lỗi ra đây — chỉ bỏ phần hiển thị. Đối ứng `PRMOfferWidget.confirmRedemption` Android.
                //
                // Trả **kiểu công khai**, không phải mã thô: host `switch` là xong, không phải so
                // chuỗi. Đối ứng `PRMOfferWidget.onError` bên Android.
                onError(PromotionSDKError.from(failure.errorCode))
            } else {
                onSuccess()
            }
        }
    }

    /// Map `OfferWidgetState` (store) → widget-state — **reactive**, mirror Android `PRMOfferWidget.renderState`.
    ///
    /// Quyết định 4 trạng thái lấy THẲNG từ `OfferWidgetState.widgetState` (rule dùng chung ở
    /// `promotionLogic`, 8 test phủ ở `OfferWidgetStoreTest`). Trước đây hàm này chép lại nguyên 4 nhánh
    /// bằng Swift — sửa rule bên Kotlin thì Android đổi theo còn iOS lặng lẽ giữ hành vi cũ.
    ///
    /// Ở đây chỉ còn phần **thật sự của native**: format chuỗi tiền và phát callback host theo
    /// transition.
    /// `@MainActor`: hàm này đụng UIKit (`view.setState`) và gọi `PromotionSDK.getCallback()` — bề
    /// mặt public nay MainActor-isolated. Không phải ràng buộc mới, nó vốn chỉ chạy từ `offerWidgetVM.observe`
    /// (base đã hop main); khác biệt là compiler giữ thay cho một dòng comment.
    @MainActor
    private func render(_ state: OfferWidgetState, on view: PRMOfferWidget) {
        // Widget không đi qua `PRMStoreViewModel.emitErrorIfNeeded` (đọc thẳng state, `autoConsumesError`
        // tắt) nên phải tự bắt TOKEN_EXPIRED ở đây — 4 màn Store khác đã có base lo hộ. Đối ứng
        // `PRMOfferWidget.renderState` bên Android (xử lý TRƯỚC guard `hasLoadedInitial`, vì `loadInitial()`
        // hỏng cũng set `hasLoadedInitial = true` kèm `errorCode`).
        if let errorCode = state.errorCode {
            if errorCode == PromotionErrorCodes.shared.TOKEN_EXPIRED {
                PromotionSDK.getCallback()?.onExpireToken()
            }
            offerWidgetVM.consumeError()
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

    /// Map `OfferWidgetHostEvent` dùng chung → callback public của iOS.
    ///
    /// Thứ tự do notifier quyết định: **count trước, applied sau**. Bản cũ ở đây bắn ngược lại
    /// (applied trong `switch`, count sau cùng) — host nào dựa vào thứ tự đó sẽ thấy đổi.
    /// `internal` chứ không `private` **để test được**: thứ tự và số lần gọi callback là một phần
    /// của hợp đồng với host — CHANGELOG từng ghi một lần thứ tự phát bị đổi mà không chữ ký nào
    /// đổi, tức không gì bắt được. Xem `OfferWidgetHostNotifierEmitTests`.
    func emit(_ events: [OfferWidgetHostEvent]) {
        // Cast `as?` cho khớp cách file này vẫn xử lý sealed interface của Kotlin
        // (`result as? OfferWidgetConfirmResultFailure`, `effect as? PRMEffectShowError`).
        for event in events {
            if let applied = event as? OfferWidgetHostEventVoucherApplied {
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
    private static func appliedTitle(_ discount: OfferWidgetAppliedDiscount) -> String {
        if let tag = discount.tags.first, !tag.isEmpty { return tag }
        return formatDiscount(discount.calculatedDiscount)
    }

    private static func formatDiscount(_ raw: String) -> String {
        let digits = raw.filter { $0.isNumber }
        guard let value = Int(digits) else { return raw }
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        // Locale QUYẾT ĐỊNH dấu phân cách, không hardcode `"."`. Trước đây dòng này là
        // `groupingSeparator = "."` — đúng cho vi-VN và sai cho mọi locale khác, tức là tham số
        // `language` không đi tới được chỗ nó phải tới.
        formatter.locale = PRMLocalization.locale
        let formatted = formatter.string(from: NSNumber(value: value)) ?? "\(value)"
        return PromotionUIStrings.discount(formatted)
    }

    /// Auto-apply voucher `isAutoApplied`: validate qua OfferWidgetStore; `render` (observe) lo cập nhật widget
    /// + callback host. Dormant tới khi `findEligible` trả `isAutoApplied` (xem TODO ở attach).
    private func autoApply(_ promotions: [EligibleOffer]) {
        guard !promotions.isEmpty else { return }
        offerWidgetVM.validateAndApply(promotions)
    }

    func openChoosePromotion() {
        guard let host = _host else { return }
        let nav = _navigator ?? host.navigationController

        // Chỉ còn pre-select: màn chọn **tự gọi `findEligible`** mỗi lần mở (`SeedOnce`), không nhận
        // danh sách preload của widget nữa — giống Android (`forOfferWidget`).
        // `currentState` (đồng bộ) chứ không phải `state`: đọc ngay lúc user bấm, phải là bản mới nhất.
        let offerWidgetState = offerWidgetVM.currentState
        let vc = ChoosePromotionBuilder.build(
            with: .init(
                orderItems: context.getOrderItems(),
                // Pre-select TẤT CẢ ưu đãi đang áp (kể cả đang UNAVAILABLE) để user thấy & bỏ chọn
                // được — khớp Android (`offerWidget.discountDetails.map { it.objectId }`).
                preSelectedVoucherIds: offerWidgetState.appliedDiscounts.map { $0.objectId }
            ),
            navigator: nav
        )
        vc.onApplyVoucher = { [weak self, weak host] (promotions: [EligibleOffer], onOutcome: @escaping (OfferWidgetApplyOutcome) -> Void) in
            // `onOutcome` mở khoá nút "Áp dụng" (chống spam trong lúc chờ validate) → phải gọi ở MỌI
            // đường ra, kể cả nhánh guard này, nếu không nút khoá vĩnh viễn. Nhánh này chỉ chạy khi
            // SDK đã bị giải phóng — báo lỗi chứ không im lặng, đối ứng nhánh `viewModel == nil` của
            // `PRMOfferWidget.applySelectedOffers` bên Android.
            guard let self, !promotions.isEmpty else {
                onOutcome(OfferWidgetApplyOutcomeFailed(errorCode: PromotionErrorCodes.shared.GENERAL))
                return
            }

            let pop: () -> Void = {
                if let navCtrl = host?.navigationController {
                    navCtrl.popViewController(animated: true)
                } else {
                    host?.dismiss(animated: true)
                }
            }

            // Bấm "Áp dụng" -> validate qua OfferWidgetStore; widget cập nhật qua `render` (observe).
            //
            // Kết cục giao NGUYÊN VẸN cho VC (`handleApplyOutcome`) — nơi quyết định cập nhật state
            // màn và hiện popup, đối ứng từng dòng với `ChoosePromotionFragment.onApplyClicked`.
            // Ở đây chỉ giữ phần **điều hướng**, thứ VC không tự làm được (nó không giữ `host`).
            //
            // Chỉ `Applied` mới đóng màn. Trước đây nhánh "server từ chối ưu đãi" cũng rơi vào đây
            // (hàm chỉ trả `OfferWidgetState`, nơi gọi chỉ đọc được `errorCode`) nên màn đóng lại như đã áp
            // xong trong khi widget hiện ưu đãi bị gạch ngang.
            self.offerWidgetVM.validateAndApply(promotions) { outcome in
                onOutcome(outcome)
                if outcome is OfferWidgetApplyOutcomeApplied { pop() }
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

// MARK: - PRMOfferWidgetDataSource (internal conformance)

extension PromotionSDKImpl: PRMOfferWidgetDataSource {
    func selectPromotionViewDidAttachToWindow(_ view: PRMOfferWidget) {
        // Quan sát OfferWidgetStore → render widget **reactive** (mirror Android: `PRMOfferWidget` collect uiState).
        // Trạng thái áp/không-đủ-điều-kiện persist trong store nên tự khôi phục khi re-attach.
        offerWidgetVM.observe { [weak self, weak view] state in
            guard let self, let view else { return }
            self.render(state, on: view)
        }
        offerWidgetVM.loadInitial()   // idempotent — store bỏ qua nếu đã nạp
        // TODO(auto-apply): `EligibleOffer` không có `isAutoApplied` (API Find Eligible không trả) →
        // nhánh `autoApply` chưa từng chạy. Android auto-apply từ Search API; cần backend xác nhận.
    }
}

// MARK: - PRMOfferWidgetDelegate (internal conformance)

extension PromotionSDKImpl: PRMOfferWidgetDelegate {
    func selectPromotionViewDidTapSelect(_ view: PRMOfferWidget) {
        switch view.currentState {
        case .applied:
            // Huỷ áp: xoá ở store → `render` (observe) tự đưa widget về NOT_APPLIED/EMPTY.
            // KHÔNG báo host: `PromotionSDKCallback` cố ý không có sự kiện "đã huỷ voucher"
            // (`docs/common/InitParity.md` §3). Dòng gọi closure ở đây từng bắn vào `nil`.
            offerWidgetVM.clearApplied()
        default:
            openChoosePromotion()
        }
    }
}
