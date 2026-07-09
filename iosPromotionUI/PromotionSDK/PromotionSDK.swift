

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

    /// Headless API for order integration (validate discounts, create redemptions).
    public private(set) lazy var useCases: PromotionSDKUseCases = {
        impl.makeUseCases()
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
        if let theme {
            impl.applyTheme(theme)
        }
    }

    // MARK: - Theming

    /// Cập nhật theme sau khi đã khởi tạo. Truyền `nil` để reset về default SDK.
    ///
    /// Lưu ý: theme nên cấu hình **một lần** lúc khởi tạo. Các view đã render có thể chỉ
    /// cập nhật khi được dựng lại (rebind/đẩy màn mới).
    public func configure(theme: PromotionSDKTheme?) {
        impl.applyTheme(theme)
    }

    /// Theme đang áp (nil nếu đang dùng mặc định). Dùng để đọc lại / lưu cấu hình.
    public var currentTheme: PromotionSDKTheme? {
        impl.currentTheme()
    }

    // MARK: - Public API

    // MARK: - Feature flag (bật/tắt SDK)

    /// Hỏi SDK có đang được bật hay không (cờ master `PROMOTION.ENABLE_ALL`).
    ///
    /// Cờ tải bất đồng bộ: `completion` gọi lại trên **main thread** ngay khi cờ đã sẵn sàng
    /// (gọi luôn nếu đã tải xong). Trước khi Unleash xác nhận, giá trị là `false` (fail-closed).
    /// Host nên gọi hàm này để quyết định hiện/ẩn UI ưu đãi.
    public func isEnabled(completion: @escaping (Bool) -> Void) {
        impl.isEnabled(completion)
    }

    /// Hỏi một tính năng cụ thể có đang được bật hay không (đã gate ngầm bởi cờ master).
    /// `completion` chạy trên **main thread**; fail-closed khi chưa tải xong.
    public func isEnabled(feature: PromotionSDKFeature, completion: @escaping (Bool) -> Void) {
        impl.isEnabled(feature: feature, completion)
    }

    /// Show the "My Promotions" list screen.
    /// Nếu viewController có navigationController → push. Ngược lại → present modal.
    /// Cờ `VOUCHER_LIST` TẮT → hiện popup lỗi PRM_MOB_021 trên `viewController` + báo host
    /// qua `vdsPromotion(_:didUpdateAvailability:)`.
    public func openMyPromotion(from viewController: UIViewController) {
        impl.isEnabled(feature: .voucherList) { [weak self] enabled in
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
