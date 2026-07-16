//
//  PromotionSDK.swift
//  PromotionSDK
//
//  Public API surface. Only UIKit and Foundation types are used here so that
//  the consuming app's compiler never triggers cross-module deserialization of
//  PromotionLogic/PromotionUI/RxSwift when building against this framework.
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
    // framework — tránh app host phải nạp PromotionLogic/RxSwift chỉ để suy ra class layout.

    private static var _impl: NSObject?
    private static var impl: PromotionSDKImpl? { _impl as? PromotionSDKImpl }

    /// Callback host nhận sự kiện — truyền qua `PromotionSDKOptions`. Giữ tới `release` (đối ứng
    /// `private var callback` bên Android).
    private static var callback: PromotionSDKCallback?

    // MARK: - Init

    /// Khởi tạo SDK. Đối ứng `PromotionSDK.initialize(context, options)` bên Android.
    ///
    /// Gọi lại `initialize` = dựng lại đồ thị DI với session mới (vd refresh token → truyền session
    /// mới). Instance headless (`api`) dựng mới mỗi lần đọc nên luôn dùng đồ thị mới nhất.
    ///
    /// - Parameter options: session (customerId/token/baseUrl/language/environment), danh mục dịch vụ,
    ///   theme (bỏ trống = khôi phục theme đã lưu), và callback nhận sự kiện.
    public static func initialize(options: PromotionSDKOptions) {
        let impl = PromotionSDKImpl(options: options)
        _impl = impl
        callback = options.callback
        // Host truyền theme → áp + lưu. Không truyền → khôi phục theme đã lưu lần trước. Host cấu hình
        // một lần; lần sau chỉ cần initialize lại, theme tự sống lại. Đối ứng PromotionSDK.initialize bên Android.
        impl.restoreOrApplyTheme(options.theme)
        wireCallbacks(impl)
    }

    /// Giải phóng SDK. Đối ứng `PromotionSDK.release()` bên Android. Gọi khi chưa init là vô hại.
    /// **Không** xoá theme đã lưu — nó sống qua release/init.
    public static func release() {
        impl?.teardown()
        _impl = nil
        callback = nil
    }

    /// `true` sau `initialize` và trước `release`. Đối ứng `PromotionSDK.isInitialized()` bên Android.
    public static func isInitialized() -> Bool { impl != nil }

    /// Callback đã truyền lúc `initialize` (`nil` nếu chưa init / không truyền). Đối ứng `getCallback()` Android.
    public static func getCallback() -> PromotionSDKCallback? { callback }

    // MARK: - Headless API

    /// Bề mặt headless cho host tự dựng UI (lấy voucher, validate, tạo redemption). Phải gọi
    /// `initialize` trước. Không phải use case — xem `PromotionSDKApi`. Đối ứng `PromotionSDK.api` Android.
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
    public static func updateContext(
        orderId: String? = nil,
        orderValue: String? = nil,
        serviceCode: String? = nil,
        metaData: String? = nil
    ) {
        guard let impl else {
            preconditionFailure("PromotionSDK.initialize() phải được gọi trước khi updateContext().")
        }
        impl.updateContext(orderId: orderId, orderValue: orderValue, serviceCode: serviceCode, metaData: metaData)
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
    // SDK **không** phơi API hỏi cờ ra ngoài. Host không cần biết cờ nào đang bật: mọi điểm vào đều tự
    // gác (`openMyPromotion`, `openPromotionDetail`, widget), và khi bị chặn thì SDK hiện popup
    // PRM_MOB_021 rồi báo host qua `onAvailabilityChanged(enabled:)`.
    //
    // Logic quyết định nằm ở `PromotionFeatureGate` trong `promotionLogic`, dùng chung với Android.

    // MARK: - Screens

    /// Show the "My Promotions" list screen.
    /// Nếu viewController có navigationController → push. Ngược lại → present modal.
    /// Cờ `VOUCHER_LIST` TẮT → hiện popup lỗi PRM_MOB_021 trên `viewController` + báo host
    /// qua `onAvailabilityChanged(enabled:)`.
    public static func openMyPromotion(from viewController: UIViewController) {
        guard let impl else { return }
        impl.canOpenVoucherList { enabled in
            guard enabled else {
                impl.showFeatureDisabledDialog(on: viewController)
                callback?.onAvailabilityChanged(enabled: false)
                return
            }
            let nav = viewController.navigationController ?? (viewController as? UINavigationController)
            let vc = MyPromotionBuilder.build(
                with: .init(customerId: impl.customerId, token: impl.token),
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
    /// Cờ `VOUCHER_DETAIL` TẮT → hiện popup lỗi PRM_MOB_021 trên `viewController` + báo host
    /// qua `onAvailabilityChanged(enabled:)`.
    public static func openPromotionDetail(voucherId: String, from viewController: UIViewController) {
        guard let impl else { return }
        let nav = viewController.navigationController ?? (viewController as? UINavigationController)
        impl.openPromotionDetail(voucherId: voucherId, on: viewController, navigator: nav)
    }

    /// Create the promotion widget view. Attach it to your layout; it manages its own data loading.
    public static func createEndowView(from viewController: UIViewController) -> UIView {
        guard let impl else { return UIView() }
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
