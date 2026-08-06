//
//  PromotionSDK.swift
//  PromotionSDK
//
//  Public API surface. Only UIKit and Foundation types are used here so that
//  the consuming app's compiler never triggers cross-module deserialization of
//  PromotionLogic/PRMPromotionUI/RxSwift when building against this framework.
//
//  Singleton tĩnh — đối ứng 1:1 `object PromotionSDK` bên Android. Toàn bộ điểm vào là hàm/thuộc
//  tính `static`; SDK chỉ giữ **một** đồ thị DI sống tại một thời điểm. Cấu hình một lần qua
//  `initialize(options:)`, cập nhật đơn hàng/dịch vụ qua `updateContext(...)`.
//

import UIKit

public final class PromotionSDK {

    // Chặn khởi tạo instance từ ngoài — API hoàn toàn tĩnh (giống `object` của Android).
    private init() {}

    // MARK: - Private storage
    //
    // _impl là NSObject để type nội bộ (PromotionSDKImpl) không xuất hiện trong module interface của
    // framework — tránh app host phải nạp PromotionLogic/module nội bộ chỉ để suy ra class layout.

    private static var _impl: NSObject?
    private static var impl: PromotionSDKImpl? { _impl as? PromotionSDKImpl }

    /// Đồ thị đang sống, kèm **cảnh báo rõ ràng** khi host gọi trước `initialize`.
    ///
    /// Trước đây mỗi điểm vào tự xử lý một kiểu: điều hướng thì `guard … else { return }` (im lặng —
    /// host thấy "bấm không ăn"), còn `updateContext` thì `preconditionFailure` (**crash app host**).
    /// Nay thống nhất: **không** crash, log một dòng nêu đúng hàm bị gọi sớm. Đối ứng Android — chỉ
    /// khác ở chỗ Kotlin ném `IllegalStateException` (host bắt được), Swift `fatalError` thì không,
    /// nên iOS chọn log thay vì kết liễu tiến trình của host.
    private static func requireImpl(_ caller: String) -> PromotionSDKImpl? {
        guard let impl else {
            NSLog("[PromotionSDK] %@ bị gọi trước initialize() — bỏ qua. Hãy gọi PromotionSDK.initialize(options:) trước.", caller)
            return nil
        }
        return impl
    }

    /// Callback host nhận sự kiện — truyền qua `PromotionSDKOptions`. Giữ tới `release` (đối ứng
    /// `private var callback` bên Android).
    private static var callback: PromotionSDKCallback?

    /// Cấu hình **cố định**, chốt ở lần `initialize` **đầu tiên** (baseUrl/environment/language). Các
    /// lần `initialize` sau (host init lại mỗi khi login) chỉ áp field **động** (token/
    /// availableServices); host lỡ truyền field cố định khác → SDK cảnh báo và **bỏ qua**. Muốn đổi
    /// thật → `release()` rồi init lại. Đối ứng `fixedConfig` bên Android.
    private struct FixedConfig {
        let baseUrl: String
        let language: String
        let environment: PromotionEnvironment
    }
    private static var fixedConfig: FixedConfig?

    // MARK: - Init

    /// Khởi tạo SDK. Đối ứng `PromotionSDK.initialize(context, options)` bên Android.
    ///
    /// **Host chỉ cần gọi `initialize` — kể cả khi login lại.** Lần đầu chốt phần **cố định**
    /// (`baseUrl` / `environment` / `language` / `theme`). Các lần sau (đăng nhập user mới) chỉ cần
    /// truyền lại field **động** (`accessToken` / `availableServices`); SDK **bỏ qua**
    /// mọi thay đổi ở field cố định (có cảnh báo log). Muốn đổi cấu hình cố định thật → `release()` rồi
    /// init lại.
    ///
    /// - Parameter options: session (token/baseUrl/language/environment), danh mục dịch vụ,
    ///   theme (bỏ trống = khôi phục theme đã lưu), và callback nhận sự kiện.
    public static func initialize(options: PromotionSDKOptions) {
        let incoming = options.session
        if isInitialized(), let locked = fixedConfig, let impl = impl {
            // Login lại: field cố định đã khoá. Cảnh báo nếu host truyền khác, rồi giữ nguyên bản khoá.
            if locked.baseUrl != incoming.baseUrl
                || locked.environment != incoming.environment
                || locked.language != incoming.language {
                NSLog("[PromotionSDK] initialize() được gọi lại với field cố định khác (baseUrl/environment/language). Các field này chốt ở lần initialize() đầu và bị bỏ qua. Gọi release() trước nếu muốn đổi.")
            }
            if let cb = options.callback { callback = cb }
            // Chỉ áp field động; ép field cố định về bản đã khoá. Context đơn hàng reset (phiên mới).
            impl.applySession(
                PromotionSessionConfig(
                    accessToken: incoming.accessToken,
                    baseUrl: locked.baseUrl, language: locked.language, environment: locked.environment,
                ),
                availableServices: options.availableServices,
                keepOrderContext: false
            )
            return
        }

        // Lần đầu (hoặc sau release): chốt field cố định + dựng đồ thị DI đầy đủ.
        if isInitialized() { release() }
        let impl = PromotionSDKImpl(options: options)
        _impl = impl
        callback = options.callback
        fixedConfig = FixedConfig(baseUrl: incoming.baseUrl, language: incoming.language, environment: incoming.environment)
        // Host truyền theme → áp + lưu. Không truyền → khôi phục theme đã lưu lần trước. Host cấu hình
        // một lần; lần sau chỉ cần initialize lại, theme tự sống lại. Đối ứng PromotionSDK.initialize bên Android.
        impl.restoreOrApplyTheme(options.theme)
        wireCallbacks(impl)
        // Phải gọi **sau** wireCallbacks: cờ nạp xong mới báo host, và lúc đó callback đã được nối.
        impl.notifyAvailabilityAfterInitialLoad()
    }

    /// Khởi tạo **tối giản** — đủ cho phần lớn host: chỉ token + baseUrl.
    /// `availableServices`/`theme`/`callback` là tuỳ chọn; cần cấu hình sâu hơn thì dùng overload
    /// nhận `PromotionSDKOptions`. Đối ứng overload phẳng `initialize(...)` bên Android.
    public static func initialize(
        accessToken: String,
        baseUrl: String,
        environment: PromotionEnvironment = .prod,
        language: String = "vi-VN",
        availableServices: [PromotionAvailableService] = [],
        theme: PromotionSDKTheme? = nil,
        callback: PromotionSDKCallback? = nil
    ) {
        initialize(options: PromotionSDKOptions(
            session: PromotionSessionConfig(
                accessToken: accessToken, baseUrl: baseUrl,
                language: language, environment: environment,
            ),
            availableServices: availableServices,
            theme: theme,
            callback: callback,
        ))
    }

    /// **Đăng nhập user mới** sau khi đã `initialize` một lần — chỉ truyền field **động**
    /// (`accessToken` + `availableServices` + `callback`); SDK **giữ nguyên** field cố định đã khoá
    /// (baseUrl / environment / language / theme). Lối chính cho host: `initialize` **một
    /// lần** lúc mở app, mỗi lần login sau chỉ gọi `updateSession`. Context đơn hàng reset (phiên mới).
    ///
    /// - Parameter availableServices: danh mục dịch vụ cho phiên mới; `nil` = giữ danh mục hiện tại.
    /// - Parameter callback: nơi nhận sự kiện cho phiên mới; `nil` = **giữ nguyên** callback hiện tại
    ///   (giống `availableServices`). Không có đường "gỡ callback" ở đây — muốn gỡ thì `release()`.
    ///   Dùng khi host thay object nghe sự kiện theo user đang đăng nhập, thay vì gọi lại `initialize`.
    /// Đối ứng `updateSession(...)` bên Android.
    public static func updateSession(accessToken: String,
                                     availableServices: [PromotionAvailableService]? = nil,
                                     callback: PromotionSDKCallback? = nil) {
        guard let impl = requireImpl("updateSession(accessToken:availableServices:callback:)") else { return }
        // Gán TRƯỚC updateSession: nạp lại cờ tính năng ở cuối `applySession` sẽ bắn
        // `onAvailabilityChanged` của phiên mới — phải về callback mới, không phải callback của user cũ.
        if let callback { Self.callback = callback }
        impl.updateSession(accessToken: accessToken, availableServices: availableServices)
    }

    /// Refresh access token **giữa phiên** (cùng customer, không đổi login) — nhẹ hơn `updateSession`:
    /// **giữ nguyên** cả context đơn hàng đang ghi (dùng khi token hết hạn giữa checkout). Đối ứng
    /// `updateToken(_:)` bên Android.
    public static func updateToken(_ accessToken: String) {
        guard let impl = requireImpl("updateToken(_:)") else { return }
        impl.updateToken(accessToken)
    }

    /// Giải phóng SDK. Đối ứng `PromotionSDK.release()` bên Android. Gọi khi chưa init là vô hại.
    /// **Không** xoá theme đã lưu — nó sống qua release/init.
    public static func release() {
        impl?.teardown()
        _impl = nil
        callback = nil
        // Mở khoá cấu hình cố định: initialize() kế tiếp được coi là "lần đầu" và chốt lại từ đầu.
        fixedConfig = nil
    }

    /// `true` sau `initialize` và trước `release`. Đối ứng `PromotionSDK.isInitialized()` bên Android.
    public static func isInitialized() -> Bool { impl != nil }

    /// Callback đã truyền lúc `initialize` (`nil` nếu chưa init / không truyền). Đối ứng `getCallback()` Android.
    public static func getCallback() -> PromotionSDKCallback? { callback }

    // MARK: - Headless API

    /// Bề mặt headless cho host tự dựng UI (lấy voucher, validate, tạo redemption). Phải gọi
    /// `initialize` trước. Không phải use case — xem `PromotionSDKApi`. Đối ứng `PromotionSDK.api` Android.
    ///
    /// Đây là điểm vào **duy nhất** còn dừng chương trình khi chưa `initialize` — kiểu trả về không
    /// optional nên không có giá trị nào an toàn để trả. Đối ứng `check(...)` bên Android. Dùng
    /// `isInitialized()` để gác trước nếu host không chắc thứ tự khởi tạo.
    public static var api: PromotionSDKApi {
        guard let impl else {
            preconditionFailure("PromotionSDK.initialize() phải được gọi trước khi truy cập api.")
        }
        return impl.makeApi()
    }

    // MARK: - Context

    /// Session đã truyền lúc `initialize`. `nil` khi chưa `initialize`. Đối ứng `PromotionSDK.session`.
    public static var session: PromotionSessionConfig? { impl?.context.session }

    /// Giá trị động hiện tại được ghi qua `updateContext`. `nil` khi chưa ghi. Đối ứng Android.
    public static var currentOrderId: String? { impl?.context.orderId }
    public static var currentOrderValue: String? { impl?.context.orderValue }
    public static var currentServiceCode: String? { impl?.context.serviceCode }
    public static var currentMetaData: String? { impl?.context.metaData }

    /// Cập nhật context đơn hàng / dịch vụ — gọi mỗi khi host vào màn có voucher (checkout, dịch vụ…).
    ///
    /// Ghi vào context đang sống; **không** cần `initialize` lại. SDK đọc lại các giá trị này ở **mỗi**
    /// request, nên gọi trước khi mở màn hoặc gọi API là đủ. Đối ứng `PromotionSDK.updateContext` Android.
    ///
    /// - Parameter orderItems: dòng sản phẩm của đơn — cần khi muốn lấy campaign theo SKU; bỏ trống
    ///   thì chỉ nhận campaign cấp đơn. (Luồng widget có thể dùng `createEndowView(orderItems:)`.)
    public static func updateContext(
        orderId: String? = nil,
        orderValue: String? = nil,
        serviceCode: String? = nil,
        metaData: String? = nil,
        orderItems: [PromotionOrderItem] = []
    ) {
        guard let impl = requireImpl("updateContext()") else { return }
        impl.updateContext(orderId: orderId, orderValue: orderValue, serviceCode: serviceCode,
                           metaData: metaData, orderItems: orderItems)
    }

    // MARK: - Theming

    /// Cập nhật theme **và lưu lại** để sống qua các lần mở app. Truyền `nil` để xoá theme đã lưu,
    /// quay về mặc định SDK. Đối ứng `PromotionSDK.configure(theme:)` bên Android.
    ///
    /// Lưu ý: theme nên cấu hình **một lần** lúc khởi tạo. Các view đã render có thể chỉ cập nhật khi
    /// được dựng lại (rebind / đẩy màn mới).
    public static func configure(theme: PromotionSDKTheme?) {
        impl?.applyAndPersistTheme(theme)
    }

    /// Theme đang áp (`nil` nếu đang dùng mặc định). Đối ứng `PromotionSDK.currentTheme()` bên Android.
    public static func currentTheme() -> PromotionSDKTheme? {
        impl?.currentTheme()
    }

    // MARK: - Feature flag
    //
    // SDK **vẫn tự gác** mọi điểm vào (`openMyPromotion`, `openPromotionDetail`, widget) — bốn hàm
    // dưới đây **không** thay thế việc đó, chúng chỉ cho host *hỏi trước* để ẩn entry point của mình
    // thay vì để user bấm rồi ăn toast PRM_MOB_021.
    //
    // Tất cả đều **fail-open**: chưa `initialize` hoặc chưa có cache → trả "bật hết". Không hàm nào
    // dừng chương trình, vì cờ hỏng không được phép làm chết màn hình của host.
    //
    // Logic quyết định nằm ở `PromotionFeatureGate` trong `promotionLogic`, dùng chung với Android.

    /// Ảnh chụp toàn bộ cờ, đọc **cache đồng bộ** — không gọi mạng, gọi được từ main thread.
    /// Đã áp sẵn công tắc tổng: `all == false` thì mọi field còn lại đều `false`.
    ///
    /// Cache được nạp ở `initialize` và mỗi lần `refreshFeatureFlags`; muốn chắc chắn mới nhất thì
    /// gọi `refreshFeatureFlags` rồi đọc trong `completion`. Đối ứng `PromotionSDK.featureFlags()` Android.
    public static func featureFlags() -> PromotionFeatureFlagsSnapshot {
        PromotionSDKImpl.featureFlagsSnapshot()
    }

    /// Tra **một** tính năng. Tương đương `featureFlags().isEnabled(feature)` nhưng khỏi dựng snapshot.
    ///
    /// ```swift
    /// myVoucherButton.isHidden = !PromotionSDK.isFeatureEnabled(.voucherList)
    /// ```
    ///
    /// Đối ứng `PromotionSDK.isFeatureEnabled(feature)` bên Android.
    public static func isFeatureEnabled(_ feature: PromotionFeature) -> Bool {
        PromotionSDKImpl.isFeatureEnabled(feature)
    }

    /// Công tắc tổng `PROMOTION.ENABLE_ALL` — `false` thì host nên ẩn **toàn bộ** điểm vào ưu đãi.
    /// Tương đương `isFeatureEnabled(.all)`. Đối ứng `PromotionSDK.isSdkEnabled()` bên Android.
    public static func isSdkEnabled() -> Bool {
        PromotionSDKImpl.isSdkEnabled()
    }

    /// Nạp lại cờ từ server rồi trả snapshot mới. **Không ném**: gọi API hỏng thì giữ nguyên cache
    /// và vẫn gọi `completion` với giá trị đang có (fail-open).
    ///
    /// `completion` chạy trên **main thread** để host set UI được ngay; gọi trước `initialize` cũng
    /// an toàn (trả cờ mặc định bật hết). Sau mỗi lần nạp, SDK báo lại công tắc tổng qua
    /// `PromotionSDKCallback.onAvailabilityChanged(enabled:)`.
    ///
    /// ```swift
    /// PromotionSDK.refreshFeatureFlags { flags in
    ///     self.promotionSection.isHidden = !flags.all
    /// }
    /// ```
    ///
    /// Đối ứng `PromotionSDK.refreshFeatureFlags(onComplete)` bên Android.
    public static func refreshFeatureFlags(completion: ((PromotionFeatureFlagsSnapshot) -> Void)? = nil) {
        PromotionSDKImpl.refreshFeatureFlags { flags in
            callback?.onAvailabilityChanged(enabled: flags.all)
            completion?(flags)
        }
    }

    // MARK: - Screens

    /// Show the "My Promotions" list screen.
    /// Nếu viewController có navigationController → push. Ngược lại → present modal.
    /// Cờ `VOUCHER_LIST` TẮT → hiện toast lỗi PRM_MOB_021 trên `viewController` + báo host
    /// qua `onAvailabilityChanged(enabled:)`.
    public static func openMyPromotion(from viewController: UIViewController) {
        guard let impl = requireImpl("openMyPromotion(from:)") else { return }
        impl.canOpenVoucherList { enabled in
            guard enabled else {
                impl.showFeatureDisabledToast(on: viewController)
                callback?.onAvailabilityChanged(enabled: false)
                return
            }
            let nav = viewController.navigationController ?? (viewController as? UINavigationController)
            let vc = MyPromotionBuilder.build(
                with: .init(),
                navigator: nav
            )
            vc.onClose = {
                callback?.onClosed()
            }
            if let nav {
                nav.pushViewController(vc, animated: true)
            } else {
                let wrapper = UINavigationController(rootViewController: vc)
                wrapper.modalPresentationStyle = .fullScreen
                viewController.present(wrapper, animated: true)
            }
        }
    }

    /// Mở màn **chi tiết ưu đãi** theo `voucherId`.
    /// Nếu viewController có navigationController → push. Ngược lại → present modal.
    /// Màn tự gọi API lấy chi tiết đầy đủ; trong lúc chờ hiện shimmer.
    /// Cờ `VOUCHER_DETAIL` TẮT → hiện toast lỗi PRM_MOB_021 trên `viewController` + báo host
    /// qua `onAvailabilityChanged(enabled:)`.
    public static func openPromotionDetail(voucherId: String, from viewController: UIViewController) {
        guard let impl = requireImpl("openPromotionDetail(voucherId:from:)") else { return }
        let nav = viewController.navigationController ?? (viewController as? UINavigationController)
        impl.openPromotionDetail(voucherId: voucherId, on: viewController, navigator: nav)
    }

    /// Create the promotion widget view. Attach it to your layout; it manages its own data loading.
    public static func createEndowView(from viewController: UIViewController) -> UIView {
        // Chưa init → trả view rỗng (không crash): host đã gắn nó vào layout rồi, huỷ tiến trình ở đây
        // là tệ nhất. Log ở `requireImpl` cho biết vì sao widget trống.
        guard let impl = requireImpl("createEndowView(from:)") else { return UIView() }
        let nav = viewController.navigationController ?? (viewController as? UINavigationController)
        return impl.makeEndowView(presentFrom: viewController, navigator: nav)
    }

    /// Tạo widget cho luồng thanh toán kèm thông tin đơn hàng.
    ///
    /// Dùng khi giữ **một** phiên SDK từ lúc login (chưa biết đơn) rồi bơm `orderId`/`orderValue` tại
    /// màn thanh toán. SDK dùng 2 giá trị này để validate voucher khi user bấm "Áp dụng".
    /// Tương đương gọi `updateContext(orderId:orderValue:)` rồi `createEndowView(from:)`.
    /// - Note: `orderValue` là chuỗi số nguyên (VNĐ), vd `"500000"`.
    public static func createEndowView(from viewController: UIViewController,
                                       orderId: String?,
                                       orderValue: String?) -> UIView {
        impl?.updateOrder(orderId: orderId, orderValue: orderValue)
        return createEndowView(from: viewController)
    }

    /// Tạo widget cho luồng thanh toán kèm đơn hàng **và dòng sản phẩm** (`orderItems`).
    ///
    /// Dùng khi cần lấy campaign theo SKU: SDK gọi Find Eligible Campaigns với `orderInfo.items[]`.
    /// - Note: bỏ `orderItems` (dùng overload phía trên) → chỉ nhận campaign cấp đơn.
    public static func createEndowView(from viewController: UIViewController,
                                       orderId: String?,
                                       orderValue: String?,
                                       orderItems: [PromotionOrderItem]) -> UIView {
        impl?.updateOrder(orderId: orderId, orderValue: orderValue, orderItems: orderItems)
        return createEndowView(from: viewController)
    }

    // MARK: - Private

    private static func wireCallbacks(_ impl: PromotionSDKImpl) {
        impl.onApplyVoucher = { voucherId in
            callback?.onVoucherApplied(voucherId: voucherId)
        }
        impl.onClearVoucher = {
            callback?.onVoucherCleared()
        }
        impl.onUpdateWidgetCount = { count in
            callback?.onVoucherCountChanged(count: count)
        }
        impl.onAvailabilityUpdate = { enabled in
            callback?.onAvailabilityChanged(enabled: enabled)
        }
    }
}
