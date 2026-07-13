

//
//  PromotionSDK.swift
//  PromotionSDK
//
//  Public API surface. Only UIKit and Foundation types are used here so that
//  the consuming app's compiler never triggers cross-module deserialization of
//  PromotionLogic/PromotionUI/RxSwift when building against this framework.
//

import UIKit

public final class PromotionSDK {

    // MARK: - Public

    public weak var delegate: PromotionSDKCallback? {
        didSet { wireCallbacks() }
    }

    /// Bề mặt headless cho host tự dựng UI (lấy voucher, validate, tạo redemption).
    /// Không phải use case — xem [PromotionSDKApi].
    public private(set) lazy var api: PromotionSDKApi = {
        impl.makeApi()
    }()

    // MARK: - Private
    //
    // _impl is stored as NSObject so the concrete internal type (PromotionSDKImpl)
    // never appears in the framework's binary module interface. This prevents
    // the consuming app's compiler from loading PromotionLogic/RxSwift modules
    // to determine PromotionSDK's class layout.

    private let _impl: NSObject
    private var impl: PromotionSDKImpl { _impl as! PromotionSDKImpl }

    // MARK: - Init

    /// - Parameters:
    ///   - orderId/orderValue: thông tin đơn hàng dùng để validate voucher khi user bấm "Áp dụng".
    ///   - theme: tùy biến giao diện (màu/bo góc). Bỏ trống = dùng default SDK. Xem `PromotionSDKTheme`.
    ///   - baseURL: base URL Promotion BFF (vd trỏ prod/UAT). Bỏ trống = dùng URL mặc định theo build.
    ///   - availableServices: danh mục dịch vụ khả dụng — dùng cho bottom sheet "Chọn dịch vụ" khi áp voucher.
    ///   - orderItems: dòng đơn hàng — dùng cho Find Eligible Campaigns (luồng "Chọn ưu đãi"). Bỏ trống = chỉ campaign cấp đơn.
    public init(customerId: String, token: String? = nil, orderId: String? = nil, orderValue: String? = nil, orderItems: [PromotionOrderItem] = [], theme: PromotionSDKTheme? = nil, baseURL: String? = nil, availableServices: [PromotionAvailableService] = []) {
        _impl = PromotionSDKImpl(customerId: customerId, token: token, orderId: orderId, orderValue: orderValue, orderItems: orderItems, baseURL: baseURL, availableServices: availableServices)
        // Truyền theme → áp + lưu. Không truyền → khôi phục theme đã lưu lần trước. Host cấu hình một
        // lần; lần sau chỉ cần khởi tạo lại SDK, theme tự sống lại. Đối ứng PromotionSDK.init bên Android.
        impl.restoreOrApplyTheme(theme)
    }

    // MARK: - Theming

    /// Cập nhật theme **và lưu lại** để sống qua các lần mở app. Truyền `nil` để xoá theme đã lưu,
    /// quay về mặc định SDK.
    ///
    /// Lưu ý: theme nên cấu hình **một lần** lúc khởi tạo. Các view đã render có thể chỉ
    /// cập nhật khi được dựng lại (rebind/đẩy màn mới).
    public func configure(theme: PromotionSDKTheme?) {
        impl.applyAndPersistTheme(theme)
    }

    /// Theme đang áp (nil nếu đang dùng mặc định). Dùng để đọc lại / lưu cấu hình.
    public var currentTheme: PromotionSDKTheme? {
        impl.currentTheme()
    }

    // MARK: - Public API

    // MARK: - Feature flag
    //
    // SDK **không** phơi API hỏi cờ ra ngoài. Host không cần biết cờ nào đang bật: mọi điểm vào
    // đều tự gác (`openMyPromotion`, `openPromotionDetail`, widget), và khi bị chặn thì SDK hiện
    // popup PRM_MOB_021 rồi báo host qua `vdsPromotion(_:didUpdateAvailability:)`.
    //
    // Logic quyết định nằm ở `PromotionFeatureGate` trong `promotionLogic`, dùng chung với Android.
    // Kể cả khi muốn phơi ra, type Kotlin **không thể** xuất hiện ở API public: nó sẽ kéo
    // `PromotionKit` vào `.swiftinterface` của framework, khiến app host không build được
    // (`error: Unable to find module dependency: 'PromotionKit'`).

    /// Show the "My Promotions" list screen.
    /// Nếu viewController có navigationController → push. Ngược lại → present modal.
    /// Cờ `VOUCHER_LIST` TẮT → hiện popup lỗi PRM_MOB_021 trên `viewController` + báo host
    /// qua `vdsPromotion(_:didUpdateAvailability:)`.
    public func openMyPromotion(from viewController: UIViewController) {
        impl.canOpenVoucherList { [weak self] enabled in
            guard let self else { return }
            guard enabled else {
                self.impl.showFeatureDisabledDialog(on: viewController)
                self.delegate?.vdsPromotion(self, didUpdateAvailability: false)
                return
            }
            let nav = viewController.navigationController ?? (viewController as? UINavigationController)
            let vc = MyPromotionBuilder.build(
                with: .init(customerId: self.impl.customerId, token: self.impl.token),
                navigator: nav
            )
            vc.onClose = { [weak self] in
                guard let self else { return }
                self.delegate?.vdsPromotionDidClose(self)
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
    /// qua `vdsPromotion(_:didUpdateAvailability:)`.
    public func openPromotionDetail(voucherId: String, from viewController: UIViewController) {
        let nav = viewController.navigationController ?? (viewController as? UINavigationController)
        impl.openPromotionDetail(voucherId: voucherId, on: viewController, navigator: nav)
    }

    /// Create the promotion widget view. Attach it to your layout; it manages its own data loading.
    public func createEndowView(from viewController: UIViewController) -> UIView {
        let nav = viewController.navigationController ?? (viewController as? UINavigationController)
        return impl.makeEndowView(presentFrom: viewController, navigator: nav)
    }

    /// Tạo widget cho luồng thanh toán kèm thông tin đơn hàng.
    ///
    /// Dùng khi giữ **một** instance SDK từ lúc login (chưa biết đơn) rồi bơm `orderId`/`orderValue`
    /// tại màn thanh toán. SDK dùng 2 giá trị này để validate voucher khi user bấm "Áp dụng".
    /// - Note: `orderValue` là chuỗi số nguyên (VNĐ), vd `"500000"`.
    public func createEndowView(from viewController: UIViewController,
                                          orderId: String?,
                                          orderValue: String?) -> UIView {
        impl.updateOrder(orderId: orderId, orderValue: orderValue)
        return createEndowView(from: viewController)
    }

    /// Tạo widget cho luồng thanh toán kèm đơn hàng **và dòng sản phẩm** (`orderItems`).
    ///
    /// Dùng khi cần lấy campaign theo SKU: SDK gọi Find Eligible Campaigns với `orderInfo.items[]`.
    /// - Note: bỏ `orderItems` (dùng overload phía trên) → chỉ nhận campaign cấp đơn.
    public func createEndowView(from viewController: UIViewController,
                                          orderId: String?,
                                          orderValue: String?,
                                          orderItems: [PromotionOrderItem]) -> UIView {
        impl.updateOrder(orderId: orderId, orderValue: orderValue, orderItems: orderItems)
        return createEndowView(from: viewController)
    }

    // MARK: - Private

    private func wireCallbacks() {
        impl.onApplyVoucher = { [weak self] voucherId in
            guard let self else { return }
            self.delegate?.vdsPromotion(self, didApplyVoucherId: voucherId)
        }
        impl.onClearVoucher = { [weak self] in
            guard let self else { return }
            self.delegate?.vdsPromotionDidClearVoucher(self)
        }
        impl.onUpdateWidgetCount = { [weak self] count in
            guard let self else { return }
            self.delegate?.vdsPromotion(self, didUpdateVoucherCount: count)
        }
        impl.onServiceSelected = { [weak self] selection in
            guard let self else { return }
            self.delegate?.vdsPromotion(self, didSelectService: selection)
        }
        impl.onAvailabilityUpdate = { [weak self] enabled in
            guard let self else { return }
            self.delegate?.vdsPromotion(self, didUpdateAvailability: enabled)
        }
    }
}
