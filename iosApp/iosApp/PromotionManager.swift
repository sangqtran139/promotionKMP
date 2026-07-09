//
//  PromotionManager.swift
//  VDSPromotionDemoApp
//
//  MẪU THAM KHẢO — Wrapper / Anti-Corruption Layer bọc PromotionSDK SDK.
//
//  Ý tưởng (giống cách quản lý SDK bên Android): TOÀN BỘ phần app chỉ nói chuyện với
//  `PromotionManager` (qua protocol `PromotionServing`), KHÔNG gọi SDK rải rác. Khi
//  upgrade/đổi SDK, chỉ sửa đúng file này.
//
//  Gom vào 1 chỗ các ràng buộc dễ sai của SDK:
//   - Chỉ 1 instance sống tại 1 thời điểm.
//   - Token bị "chụp" lúc init → refresh token = tạo lại instance.
//   - Order truyền lúc mở widget thanh toán (không re-init).
//   - `delegate` là 1-1 → manager sở hữu, map type SDK sang model APP rồi phát ra.
//
//  LƯU Ý: file này import SDK nên CỐ TÌNH GIỮ NHỎ (không nhét UI nặng) — xem ghi chú ở
//  ThemePreviewViewController về swift-frontend `deserializeClass`.
//

import UIKit
import PromotionSDKUI

// MARK: - Model của APP (không dùng type SDK ở tầng app → anti-corruption)

/// Thông tin đơn hàng khi mở widget thanh toán.
struct OrderContext {
    let id: String
    /// Chuỗi số nguyên, đơn vị VNĐ. VD "500000".
    let value: String
}

/// Dịch vụ user chọn trong bottom sheet "Chọn dịch vụ".
struct ServiceSelection {
    let voucherId: String
    let code: String
    let name: String
    let iconUrl: String
}

/// Dịch vụ khả dụng host cấu hình (dùng cho bottom sheet "Chọn dịch vụ").
struct AvailableService {
    let code: String
    let name: String
    let type: String
    let iconUrl: String
    init(code: String, name: String, type: String = "", iconUrl: String = "") {
        self.code = code; self.name = name; self.type = type; self.iconUrl = iconUrl
    }
}

/// Voucher rút gọn (kết quả headless getVouchers).
struct VoucherSummary {
    let id: String
    let merchantName: String
    let title: String
    let imageURL: String?
    let expireDate: Date?
    let isUsed: Bool
    let statusLabel: String?
}

/// Danh sách voucher "của tôi" (Search Customer Vouchers chỉ trả voucher đã sở hữu).
struct VoucherPage {
    let mine: [VoucherSummary]
    let mineIsLastPage: Bool
}

/// Kết quả validate voucher theo đơn.
struct ValidationSummary {
    let isValid: Bool
    let totalDiscount: String
    let finalAmount: String
}

// MARK: - Cổng app-facing (app phụ thuộc protocol này, dễ mock/test, dễ thay SDK)

protocol PromotionServing: AnyObject {
    /// Gọi sau khi login thành công. `availableServices` do host cung cấp (cho bottom sheet "Chọn dịch vụ").
    func start(customerId: String, token: String?, availableServices: [AvailableService])
    /// Gọi khi access token được refresh — manager tự tạo lại instance với token mới.
    func updateToken(_ token: String?)
    /// Gọi khi logout.
    func stop()

    /// Mở màn "Ưu đãi của tôi / Chi tiết" (không cần order).
    func openMyPromotions(from viewController: UIViewController)
    /// Tạo widget cho màn thanh toán (truyền order tại đây, không re-init).
    func makeCheckoutWidget(from viewController: UIViewController, order: OrderContext) -> UIView

    // --- Headless API (không UI) ---
    /// Lấy danh sách voucher của khách (Search Customer Vouchers).
    func fetchVouchers(keyword: String?, serviceCode: String?, tab: String?,
                       myPage: Int,
                       completion: @escaping (Result<VoucherPage, Error>) -> Void)
    /// Kiểm tra voucher còn hợp lệ với đơn hàng (nên gọi trước createRedemption).
    func validate(order: OrderContext, voucherIds: [String],
                  completion: @escaping (Result<ValidationSummary, Error>) -> Void)
    /// Tạo phiên thanh toán sau khi user áp dụng voucher (trả về sessionId).
    func createRedemption(order: OrderContext, voucherId: String, completion: @escaping (Result<String, Error>) -> Void)

    // Sự kiện phát cho app (fan-out từ delegate 1-1 của SDK).
    var onVoucherApplied: ((_ voucherId: String) -> Void)? { get set }
    var onVoucherCleared: (() -> Void)? { get set }
    var onVoucherCountChanged: ((_ count: Int) -> Void)? { get set }
    var onServiceSelected: ((ServiceSelection) -> Void)? { get set }
}

// MARK: - Manager: chỗ DUY NHẤT chạm PromotionSDK SDK

final class PromotionManager: NSObject, PromotionServing {

    /// Singleton — ép đúng ràng buộc "1 instance SDK sống tại 1 thời điểm".
    /// (Có DI container thì inject `PromotionServing` thay cho singleton cũng được.)
    static let shared = PromotionManager()
    private override init() {}

    // Sự kiện
    var onVoucherApplied: ((String) -> Void)?
    var onVoucherCleared: (() -> Void)?
    var onVoucherCountChanged: ((Int) -> Void)?
    var onServiceSelected: ((ServiceSelection) -> Void)?

    private var sdk: PromotionSDK?
    private var customerId: String?
    private var availableServices: [AvailableService] = []   // host truyền xuống qua start(...)

    /// Escape hatch CHỈ cho công cụ demo (API/Theme Playground) cần instance SDK thật.
    /// App thật KHÔNG nên dùng — mọi thao tác nên đi qua các hàm của manager.
    var rawSDK: PromotionSDK? { sdk }

    // Theme cấu hình tập trung 1 chỗ (nil = mặc định SDK).
    private static let theme: PromotionSDKTheme? = nil   // hoặc PromotionSDKTheme(button: ...)

    // MARK: Lifecycle

    func start(customerId: String, token: String?, availableServices: [AvailableService]) {
        self.customerId = customerId
        self.availableServices = availableServices
        rebuild(token: token)
    }

    func updateToken(_ token: String?) {
        rebuild(token: token)   // token bị chụp lúc init → refresh = tạo lại instance
    }

    func stop() {
        sdk = nil
        customerId = nil
        availableServices = []
    }

    private func rebuild(token: String?) {
        guard let customerId else { return }
        let instance = PromotionSDK(
            customerId: customerId,
            token: token,
            availableServices: availableServices.map {
                PromotionAvailableService(serviceCode: $0.code, serviceName: $0.name, serviceType: $0.type, iconUrl: $0.iconUrl)
            }
        )
        instance.delegate = self
        if let theme = Self.theme { instance.configure(theme: theme) }   // set theme TRƯỚC khi tạo view
        sdk = instance
    }

    // MARK: Điều hướng / UI

    func openMyPromotions(from viewController: UIViewController) {
        sdk?.openMyPromotion(from: viewController)
    }

    func makeCheckoutWidget(from viewController: UIViewController, order: OrderContext) -> UIView {
        // Order truyền tại đây → không cần re-init SDK.
        sdk?.createEndowView(from: viewController, orderId: order.id, orderValue: order.value) ?? UIView()
    }

    // MARK: Headless

    func fetchVouchers(keyword: String? = nil, serviceCode: String? = nil, tab: String? = nil,
                       myPage: Int = 0,
                       completion: @escaping (Result<VoucherPage, Error>) -> Void) {
        guard let sdk else { return completion(.failure(Self.notReady)) }
        sdk.useCases.getVouchers(keyword: keyword, serviceCode: serviceCode, tab: tab,
                                 myPage: myPage) { result in
            switch result {
            case .success(let r):
                completion(.success(VoucherPage(
                    mine: r.myVouchers.map(Self.map),
                    mineIsLastPage: r.myIsLastPage
                )))
            case .failure(let error): completion(.failure(error))
            }
        }
    }

    func validate(order: OrderContext, voucherIds: [String],
                  completion: @escaping (Result<ValidationSummary, Error>) -> Void) {
        guard let sdk else { return completion(.failure(Self.notReady)) }
        sdk.useCases.validateDiscounts(orderId: order.id, orderValue: order.value, voucherIds: voucherIds) { result in
            switch result {
            case .success(let r):
                completion(.success(ValidationSummary(isValid: r.overallValid,
                                                      totalDiscount: r.totalDiscountAmount,
                                                      finalAmount: r.finalAmount)))
            case .failure(let error): completion(.failure(error))
            }
        }
    }

    func createRedemption(order: OrderContext, voucherId: String, completion: @escaping (Result<String, Error>) -> Void) {
        guard let sdk else { return completion(.failure(Self.notReady)) }
        sdk.useCases.createRedemption(orderId: order.id, orderValue: order.value, voucherIds: [voucherId]) { result in
            switch result {
            case .success(let r): completion(.success(r.sessionId))   // map type SDK → String cho app
            case .failure(let error): completion(.failure(error))
            }
        }
    }

    // MARK: Mapping SDK → app model (anti-corruption)

    private static let notReady = NSError(domain: "PromotionManager", code: -1,
                                          userInfo: [NSLocalizedDescriptionKey: "SDK chưa khởi tạo (chưa login?)"])

    private static func map(_ v: PromotionVoucher) -> VoucherSummary {
        VoucherSummary(id: v.id, merchantName: v.merchantName, title: v.title, imageURL: v.imageURL,
                       expireDate: v.expireDate, isUsed: v.isUsed, statusLabel: v.displayStatusLabel)
    }
}

// MARK: - Delegate SDK (1-1) → map sang model app rồi phát ra ngoài

extension PromotionManager: PromotionSDKCallback {

    func vdsPromotion(_ sdk: PromotionSDK, didApplyVoucherId voucherId: String) {
        onVoucherApplied?(voucherId)
    }

    func vdsPromotionDidClearVoucher(_ sdk: PromotionSDK) {
        onVoucherCleared?()
    }

    func vdsPromotion(_ sdk: PromotionSDK, didUpdateVoucherCount count: Int) {
        onVoucherCountChanged?(count)
    }

    func vdsPromotion(_ sdk: PromotionSDK, didSelectService selection: PromotionSDKServiceSelection) {
        onServiceSelected?(ServiceSelection(
            voucherId: selection.voucherId,
            code: selection.serviceCode,
            name: selection.serviceName,
            iconUrl: selection.iconUrl
        ))
    }
}

/*
 CÁCH DÙNG TRONG APP (không đụng type SDK ngoài file này):

 // sau login (availableServices lấy từ host):
 PromotionManager.shared.start(
     customerId: user.id,
     token: auth.accessToken,
     availableServices: host.services.map { .init(code: $0.code, name: $0.name, iconUrl: $0.icon) }
 )
 PromotionManager.shared.onServiceSelected = { sel in /* điều hướng tới màn dịch vụ */ }

 // màn "Ưu đãi của tôi":
 PromotionManager.shared.openMyPromotions(from: self)

 // headless (không UI):
 PromotionManager.shared.fetchVouchers(keyword: "grab") { result in /* result.success = VoucherPage */ }

 // màn thanh toán:
 let widget = PromotionManager.shared.makeCheckoutWidget(from: self, order: .init(id: order.id, value: "500000"))
 PromotionManager.shared.onVoucherApplied = { voucherId in
     let order = OrderContext(id: order.id, value: "500000")
     PromotionManager.shared.validate(order: order, voucherIds: [voucherId]) { _ in
         PromotionManager.shared.createRedemption(order: order, voucherId: voucherId) { result in
             // result.success = sessionId → gọi API thanh toán backend
         }
     }
 }

 // refresh token / logout:
 PromotionManager.shared.updateToken(newToken)
 PromotionManager.shared.stop()
*/
