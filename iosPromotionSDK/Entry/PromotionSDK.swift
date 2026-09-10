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
//  `initialize(options:)`, cập nhật đơn hàng/dịch vụ qua `updateOrderInfo(...)`.
//

import UIKit

/// `@MainActor` trên **cả class**, không rải trên từng hàm.
///
/// Hợp đồng thật của bề mặt này vốn đã là "gọi trên main thread" — nó là API UI:
/// `openMyPromotion(from: UIViewController)`, `createOfferWidget(from:)`, `configure(theme:)`. Trước
/// đây hợp đồng đó chỉ nằm trong doc comment, nên host gọi từ thread nền sẽ **crash lúc chạy** ở
/// tầng UIKit thay vì được compiler chỉ đúng chỗ sai.
///
/// Nó cũng đóng luôn phần global mutable state: bốn `private static var` bên dưới nay được
/// MainActor cô lập, thay vì là biến toàn cục ai đọc ghi lúc nào cũng được — thứ Swift 6 sẽ chặn.
///
/// **Thêm bây giờ, không phải sau go-live.** Sau go-live thì đây là source-breaking với mọi host:
/// mỗi chỗ gọi từ ngữ cảnh không phải main sẽ thành lỗi compile hoặc phải bọc `await MainActor.run`.
/// Hôm nay chi phí bằng không vì chưa host nào tích hợp.
@MainActor
public final class PromotionSDK {

    // Chặn khởi tạo instance từ ngoài — API hoàn toàn tĩnh (giống `object` của Android).
    private init() {}

    // MARK: - Private storage
    //
    // _impl là NSObject để type nội bộ (PromotionSDKImpl) không xuất hiện trong module interface của
    // framework — tránh app host phải nạp PromotionLogic/module nội bộ chỉ để suy ra class layout.

    private static var _impl: NSObject?
    private static var impl: PromotionSDKImpl? { _impl as? PromotionSDKImpl }

    /// Đồ thị đang sống. Gọi trước `initialize` → **không** crash: ghi log nêu đúng hàm bị gọi sớm
    /// rồi trả `nil` để nơi gọi `guard … else { return }`. Đối ứng `requireInitialized` bên Android.
    private static func requireImpl(_ caller: String) -> PromotionSDKImpl? {
        guard let impl else {
            PRMLog.integrationError(
                "[PromotionSDK] \(caller) bị gọi trước initialize() — bỏ qua. "
                + "Hãy gọi PromotionSDK.initialize(options:) trước."
            )
            return nil
        }
        return impl
    }

    /// Callback host nhận sự kiện — truyền qua `PromotionSDKOptions`. Giữ tới `release` (đối ứng
    /// `private var callback` bên Android).
    private static var callback: PromotionSDKCallback?

    /// Cấu hình **tĩnh** — đặt ở lần `initialize` đầu (hoặc lần đầu sau `release()`) rồi dùng lại cho
    /// mọi lần init sau. `release()` **không** xoá: đây là cấu hình tích hợp của host, không phải dữ
    /// liệu phiên. Đối ứng `StaticConfig` bên Android.
    private struct StaticConfig {
        let baseUrl: String
        let language: String
        let environment: PromotionEnvironment
    }
    private static var staticConfig: StaticConfig?


    // MARK: - Init

    /// Khởi tạo SDK. Đối ứng `PromotionSDK.initialize(context, options)` bên Android.
    ///
    /// **Lúc nào vào app cũng gọi `initialize` lại** — không còn `updateSession`/`updateToken`.
    ///
    /// `baseUrl` / `environment` / `language` / `theme` là cấu hình **tĩnh**: đặt ở lần đầu (hoặc
    /// lần đầu sau `release()`) rồi dùng lại, các lần init sau **không cần khởi tạo nữa**. Host chỉ
    /// cần đưa `tokenSource`. Muốn đổi thật → `release()` rồi init lại.
    ///
    /// - Parameter options: session (token/baseUrl/language/environment), danh mục dịch vụ,
    ///   theme (bỏ trống = khôi phục theme đã lưu), và callback nhận sự kiện.
    public static func initialize(options: PromotionSDKOptions) {
        // Đã có cấu hình tĩnh → dùng lại, chỉ nhận `tokenSource` mới từ host.
        let incoming: PromotionSessionConfig
        if let cfg = staticConfig {
            incoming = PromotionSessionConfig(
                tokenSource: options.session.tokenSource,
                baseUrl: cfg.baseUrl, language: cfg.language, environment: cfg.environment,
            )
        } else {
            incoming = options.session
        }
        if isInitialized() { release() }
        staticConfig = StaticConfig(baseUrl: incoming.baseUrl, language: incoming.language,
                                    environment: incoming.environment)
        // `language` quyết định chữ trên UI, không chỉ header của Ktor. Đặt TRƯỚC khi dựng `impl`:
        // `restoreOrApplyTheme` và các builder bên dưới đọc `PromotionUIStrings` ngay trong lượt này.
        // Đối ứng `PRMLocale.configure` bên Android.
        PRMLocalization.configure(languageCode: incoming.language)
        let impl = PromotionSDKImpl(options: PromotionSDKOptions(
            session: incoming,
            availableServices: options.availableServices,
            theme: options.theme,
            callback: options.callback,
        ))
        _impl = impl
        callback = options.callback
        // Host truyền theme → áp + lưu. Không truyền → khôi phục theme đã lưu lần trước. Host cấu hình
        // một lần; lần sau chỉ cần initialize lại, theme tự sống lại. Đối ứng PromotionSDK.initialize bên Android.
        impl.restoreOrApplyTheme(options.theme)
        wireCallbacks(impl)
    }

    /// Khởi tạo **tối giản** — đủ cho phần lớn host: chỉ nguồn token + baseUrl.
    /// `availableServices`/`theme`/`callback` là tuỳ chọn; cần cấu hình sâu hơn thì dùng overload
    /// nhận `PromotionSDKOptions`. Đối ứng overload phẳng `initialize(...)` bên Android.
    ///
    /// - Parameter tokenSource: nguồn token — xem `PromotionTokenSource`. SDK đọc lại token ở **mỗi**
    ///   request nên host không phải báo gì khi token đổi. Trỏ vào kho token **cấp app**, không phải
    ///   vào màn hình đang gọi hàm này.
    public static func initialize(
        tokenSource: PromotionTokenSource,
        baseUrl: String,
        environment: PromotionEnvironment = .prod,
        language: String = "vi-VN",
        availableServices: [PromotionAvailableService] = [],
        theme: PromotionSDKTheme? = nil,
        callback: PromotionSDKCallback? = nil
    ) {
        initialize(options: PromotionSDKOptions(
            session: PromotionSessionConfig(
                tokenSource: tokenSource, baseUrl: baseUrl,
                language: language, environment: environment,
            ),
            availableServices: availableServices,
            theme: theme,
            callback: callback,
        ))
    }


    /// Giải phóng SDK. Đối ứng `PromotionSDK.release()` bên Android. Gọi khi chưa init là vô hại.
    /// **Không** xoá theme đã lưu — nó sống qua release/init.
    public static func release() {
        impl?.teardown()
        _impl = nil
        callback = nil
        // Xoá sạch **dữ liệu phiên**. GIỮ `staticConfig` và theme đã lưu: đó là cấu hình tích hợp
        // của host, không phải dữ liệu người dùng. Đối ứng `PromotionSDK.release()` bên Android.
    }

    /// `true` sau `initialize` và trước `release`. Đối ứng `PromotionSDK.isInitialized()` bên Android.
    public static func isInitialized() -> Bool { impl != nil }

    /// Callback đã truyền lúc `initialize` (`nil` nếu chưa init / không truyền). Đối ứng `getCallback()` Android.
    public static func getCallback() -> PromotionSDKCallback? { callback }

    /// Version của SDK đang chạy, vd `"1.0.0"` — đọc được **trước** `initialize`.
    ///
    /// Với support và telemetry, "app đang chạy SDK bản nào" là câu hỏi đầu tiên; nó phải trả lời
    /// được bằng một dòng code. Trước đây host phải tự biết class nào để `Bundle(for:)` — kiến thức
    /// nội bộ của SDK, và là **lệch parity**: Android đã có `BuildConfig.SDK_VERSION`.
    ///
    /// Nguồn: `MARKETING_VERSION` của target `PRM` → `CFBundleShortVersionString` trong Info.plist
    /// của framework (`GENERATE_INFOPLIST_FILE = YES`). Xem `docs/release/VersioningPolicy.md`; số
    /// này giữ trùng `SDK_VERSION` bên Android.
    ///
    /// `Bundle(for:)` chứ không phải `Bundle.main`: `.main` là bundle của **app host**, trả về
    /// version của app chứ không phải của SDK.
    public static let sdkVersion: String = {
        let bundle = Bundle(for: PromotionSDKImpl.self)
        return bundle.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "unknown"
    }()

    // MARK: - Headless API

    /// Bề mặt headless cho host tự dựng UI (lấy voucher, validate, tạo redemption). Phải gọi
    /// `initialize` trước. Không phải use case — xem `PromotionSDKApi`. Đối ứng `PromotionSDK.api` Android.
    ///
    /// Gọi trước `initialize` **không crash**: trả về một bề mặt mà mọi hàm cho
    /// `.failure(.notInitialized)`. Trước đây chỗ này dừng thẳng chương trình — điểm dừng duy nhất
    /// trong cả public API, và là SDK làm crash app của host vì lỗi thứ tự khởi tạo của **host**.
    /// Với một SDK nhúng vào luồng thanh toán thì cái giá đó không đáng; lập luận cũ ("kiểu trả về
    /// không optional nên không có giá trị nào an toàn để trả") bỏ qua mất lựa chọn thứ ba là để
    /// chính các hàm trả lỗi.
    ///
    /// `isInitialized()` vẫn dùng được nếu host muốn gác trước.
    public static var api: PromotionSDKApi {
        impl?.makeApi() ?? PromotionSDKApi.notInitialized
    }

    // MARK: - Context

    /// Cấu hình phiên đã truyền lúc `initialize` (`baseUrl` / `language` / `environment` / nguồn
    /// token). `nil` khi chưa `initialize`.
    ///
    /// Không có token ở đây: token không phải cấu hình mà là giá trị đổi theo thời gian — hỏi
    /// `tokenSource` nếu cần. Đối ứng `PromotionSDK.session` bên Android.
    public static var session: PromotionSessionConfig? { impl?.context.session }

    /// Giá trị động hiện tại được ghi qua `updateOrderInfo`. `nil` khi chưa ghi. Đối ứng Android.
    public static var currentOrderId: String? { impl?.context.orderId }
    public static var currentOrderValue: String? { impl?.context.orderValue }
    public static var currentServiceCode: String? { impl?.context.serviceCode }
    public static var currentMetaData: String? { impl?.context.metaData }

    /// Cập nhật context đơn hàng / dịch vụ — gọi mỗi khi host vào màn có voucher (checkout, dịch vụ…).
    ///
    /// Ghi vào context đang sống; **không** cần `initialize` lại. SDK đọc lại các giá trị này ở **mỗi**
    /// request, nên gọi trước khi mở màn hoặc gọi API là đủ. Đối ứng `PromotionSDK.updateOrderInfo` Android.
    ///
    /// Đơn hiện chỉ hỗ trợ **một** dòng sản phẩm nên `skuSourceId`/`productName`/`productCategory`/
    /// `quantity`/`unitPrice` được truyền phẳng thay vì `[PromotionOrderItem]`; SDK tự bọc lại thành
    /// mảng 1 phần tử.
    ///
    /// - Parameter orderId: mã đơn hàng — bắt buộc.
    /// - Parameter productId: mã dịch vụ/sản phẩm — bắt buộc, dùng để lấy campaign theo SKU. (Luồng
    ///   widget có thể dùng `createOfferWidget(orderItems:)`.)
    /// - Parameter skuSourceId: mã SKU đối tác — tuỳ chọn. Bỏ trống thì SDK **không** gửi field này
    ///   lên server (không gửi chuỗi rỗng), server chỉ áp rule cấp sản phẩm/đơn.
    /// - Parameter quantity: số lượng (> 0) — mặc định `1` nếu không truyền.
    /// - Parameter unitPrice: đơn giá — mặc định `"0"` nếu không truyền.
    public static func updateOrderInfo(
        orderId: String,
        productId: String,
        orderValue: String? = nil,
        metaData: String? = nil,
        skuSourceId: String? = nil,
        productName: String? = nil,
        productCategory: String? = nil,
        quantity: Int? = nil,
        unitPrice: String? = nil
    ) {
        guard let impl = requireImpl("updateOrderInfo()") else { return }
        impl.updateOrderInfo(orderId: orderId, productId: productId, orderValue: orderValue,
                             metaData: metaData, skuSourceId: skuSourceId,
                             productName: productName, productCategory: productCategory,
                             quantity: quantity, unitPrice: unitPrice)
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
    /// an toàn (trả cờ mặc định bật hết).
    ///
    /// **Đây là đường DUY NHẤT** host biết công tắc tổng — không có callback toàn cục nào cho việc
    /// này. `PromotionSDKCallback` có đúng ba sự kiện (`onVoucherApplied` / `onServiceSelected` /
    /// `onExpireToken`); `onAvailabilityChanged` từng được cân nhắc rồi **loại bỏ có chủ đích**
    /// (xem `docs/common/InitParity.md` §3, mục "Đã loại"). Ba doc comment ở file này từng hứa nó —
    /// hứa một method chưa bao giờ tồn tại.
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
            completion?(flags)
        }
    }

    // MARK: - Screens

    /// Show the "My Promotions" list screen.
    /// Nếu viewController có navigationController → push. Ngược lại → present modal.
    /// Cờ `VOUCHER_LIST` TẮT → gọi `onFeatureDisabled` nếu host truyền, không thì SDK tự hiện
    /// PRM_MOB_021 trên `viewController`.
    /// Cờ tính năng TẮT ở một điểm mở màn: **ưu tiên trả cho host**, host không nhận thì SDK tự lo.
    ///
    /// Nhận closure ngay ở hàm `open…` chứ không dùng callback toàn cục: chỉ có cách đó SDK mới biết
    /// chắc host **có đăng ký hay không**. Đối ứng `PromotionSDK.notifyFeatureDisabled` bên Android.
    private static func notifyFeatureDisabled(_ impl: PromotionSDKImpl,
                                              _ viewController: UIViewController,
                                              _ onFeatureDisabled: (() -> Void)?) {
        if let onFeatureDisabled {
            onFeatureDisabled()
            return
        }
        impl.showFeatureDisabledDialog(on: viewController)
    }

    public static func openMyPromotion(from viewController: UIViewController,
                                       onFeatureDisabled: (() -> Void)? = nil) {
        guard let impl = requireImpl("openMyPromotion(from:)") else { return }
        impl.canOpenVoucherList { enabled in
            guard enabled else {
                notifyFeatureDisabled(impl, viewController, onFeatureDisabled)
                return
            }
            let nav = viewController.navigationController ?? (viewController as? UINavigationController)
            let vc = MyPromotionBuilder.build(
                with: .init(),
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

    /// Mở màn **chi tiết ưu đãi** theo `voucherId`.
    /// Nếu viewController có navigationController → push. Ngược lại → present modal.
    /// Màn tự gọi API lấy chi tiết đầy đủ; trong lúc chờ hiện shimmer.
    /// Cờ `VOUCHER_DETAIL` TẮT → gọi `onFeatureDisabled` nếu host truyền, không thì SDK tự hiện
    /// PRM_MOB_021 trên `viewController`.
    ///
    /// `returnVoucherOnApply` quyết định **nhãn nút và hành vi khi bấm** (TLNV MOB_002 control #5):
    /// - `true` (mặc định): nút "Áp dụng" → trả `voucherId` về `onVoucherApplied`, SDK tự đóng màn.
    /// - `false`: nút "Sử dụng ngay" → SDK mở sheet chọn dịch vụ, kết quả về
    ///   `PromotionSDKCallback.onServiceSelected`.
    ///
    /// Bật là để **màn host nào cũng mang đi tích hợp được**: `onVoucherApplied` gắn với chính lời
    /// gọi này nên data về đúng màn vừa mở, khác `PromotionSDKCallback` là kênh singleton không biết
    /// ai gọi. Đối ứng `PromotionSDK.openPromotionDetail(...)` bên Android.
    ///
    /// `hostHandlesDismiss` (mặc định `false`) → SDK tự pop màn chi tiết sau khi bấm "Áp dụng".
    /// `true` → SDK **để màn đó lại**, host tự pop trong `onVoucherApplied`:
    ///
    /// ```swift
    /// PromotionSDK.openPromotionDetail(
    ///     voucherId: id, from: self, hostHandlesDismiss: true
    /// ) { [weak self] detail in
    ///     self?.navigationController?.popViewController(animated: true)   // host tự đóng
    ///     self?.goToCheckout(detail)
    /// }
    /// ```
    ///
    /// Chỉ có nghĩa khi `returnVoucherOnApply == true` — nhánh "Sử dụng ngay" không pop bao giờ.
    ///
    /// - Parameters:
    ///   - onVoucherApplied: Chỉ dùng khi `returnVoucherOnApply == true`. Nhận **cả object
    ///     `PromotionVoucherDetail`** — cùng thứ `api.getVoucherDetail` trả, nên host không phải gọi
    ///     API lần nữa để lấy tên/mô tả/HSD/ảnh/mã code. Gọi trên main thread, **trước** khi màn pop
    ///     — nhờ vậy `hostHandlesDismiss` mới chạy được. Bỏ trống thì màn vẫn đóng, không ai nhận data.
    public static func openPromotionDetail(
        voucherId: String,
        from viewController: UIViewController,
        returnVoucherOnApply: Bool = true,
        hostHandlesDismiss: Bool = false,
        onVoucherApplied: ((PromotionVoucherDetail) -> Void)? = nil,
        onFeatureDisabled: (() -> Void)? = nil
    ) {
        guard let impl = requireImpl("openPromotionDetail(voucherId:from:)") else { return }
        let nav = viewController.navigationController ?? (viewController as? UINavigationController)
        impl.openPromotionDetail(
            voucherId: voucherId,
            on: viewController,
            navigator: nav,
            returnVoucherOnApply: returnVoucherOnApply,
            hostHandlesDismiss: hostHandlesDismiss,
            onVoucherApplied: onVoucherApplied,
            // Cờ TẮT → trả cho host nếu có; không có thì impl tự hiện popup.
            onFeatureDisabled: onFeatureDisabled
        )
    }

    /// Create the promotion widget view. Attach it to your layout; it manages its own data loading.
    public static func createOfferWidget(from viewController: UIViewController) -> UIView {
        // Chưa init → trả view rỗng (không crash): host đã gắn nó vào layout rồi, huỷ tiến trình ở đây
        // là tệ nhất. Log ở `requireImpl` cho biết vì sao widget trống.
        guard let impl = requireImpl("createOfferWidget(from:)") else { return UIView() }
        let nav = viewController.navigationController ?? (viewController as? UINavigationController)
        return impl.makeOfferWidget(presentFrom: viewController, navigator: nav)
    }

    /// Gọi khi user bấm nút thanh toán của **host**: tạo phiên redemption cho các ưu đãi đang áp
    /// trên widget.
    ///
    /// ```swift
    /// PromotionSDK.confirmRedemption(
    ///     onSuccess: { self.proceedPayment() },
    ///     onError: { code in self.showError(code) }
    /// )
    /// ```
    ///
    /// Không áp ưu đãi nào → `onSuccess` ngay, không gọi mạng. Hết ngân sách giữa chừng → SDK tự
    /// validate lại, widget hiện giá mới, rồi `onError("INSUFFICIENT_BUDGET")`.
    ///
    /// Đối ứng `PRMOfferWidget.confirmRedemption(onSuccess:onError:)` bên Android; nghiệp vụ nằm ở
    /// `OfferWidgetStore.confirmRedemption` nên hai nền tảng chạy một đường. Chưa `initialize` → `onError`.
    public static func confirmRedemption(onSuccess: @escaping () -> Void,
                                         onError: @escaping (PromotionSDKError) -> Void = { _ in }) {
        // Chuỗi mã lỗi viết thẳng: file Entry này KHÔNG import PRMKotlinBridge (type Kotlin lọt vào
        // chữ ký public là app host không build được — xem PublicApi.md).
        guard let impl = requireImpl("confirmRedemption(onSuccess:onError:)") else {
            onError(.networkFailure(code: nil, message: "error_general"))
            return
        }
        impl.confirmRedemption(onSuccess: onSuccess, onError: onError)
    }

    /// Tạo widget cho luồng thanh toán kèm thông tin đơn hàng.
    ///
    /// Dùng khi giữ **một** phiên SDK từ lúc login (chưa biết đơn) rồi bơm `orderId`/`orderValue` tại
    /// màn thanh toán. SDK dùng 2 giá trị này để validate voucher khi user bấm "Áp dụng".
    /// Tương đương gọi `impl.updateOrder(orderId:orderValue:)` (không phải `updateOrderInfo`, hàm đó
    /// yêu cầu thêm `productId` bắt buộc) rồi `createOfferWidget(from:)`.
    /// - Note: `orderValue` là chuỗi số nguyên (VNĐ), vd `"500000"`.
    public static func createOfferWidget(from viewController: UIViewController,
                                       orderId: String?,
                                       orderValue: String?) -> UIView {
        impl?.updateOrder(orderId: orderId, orderValue: orderValue)
        return createOfferWidget(from: viewController)
    }

    /// Tạo widget cho luồng thanh toán kèm đơn hàng **và dòng sản phẩm** (`orderItems`).
    ///
    /// Dùng khi cần lấy campaign theo SKU: SDK gọi Find Eligible Campaigns với `orderInfo.items[]`.
    /// - Note: bỏ `orderItems` (dùng overload phía trên) → chỉ nhận campaign cấp đơn.
    public static func createOfferWidget(from viewController: UIViewController,
                                       orderId: String?,
                                       orderValue: String?,
                                       orderItems: [PromotionOrderItem]) -> UIView {
        impl?.updateOrder(orderId: orderId, orderValue: orderValue, orderItems: orderItems)
        return createOfferWidget(from: viewController)
    }

    // MARK: - Private

    private static func wireCallbacks(_ impl: PromotionSDKImpl) {
        impl.onApplyVoucher = { voucherId in
            callback?.onVoucherApplied(voucherId: voucherId)
        }
    }
}
