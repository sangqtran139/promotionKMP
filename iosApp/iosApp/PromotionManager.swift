//
//  PromotionManager.swift
//  PromotionSDKDemo
//
//  MẪU THAM KHẢO — Wrapper / Anti-Corruption Layer bọc PRMSDK SDK.
//
//  Ý tưởng (giống cách quản lý SDK bên Android): TOÀN BỘ phần app chỉ nói chuyện với
//  `PromotionManager` (qua protocol `PromotionServing`), KHÔNG gọi SDK rải rác. Khi
//  upgrade/đổi SDK, chỉ sửa đúng file này.
//
//  Gom vào 1 chỗ các ràng buộc dễ sai của SDK:
//   - SDK là singleton tĩnh (PRMSDK.initialize / updateContext / release) — như Android.
//   - Token bị "chụp" lúc initialize → refresh token = initialize lại với session mới.
//   - Order/dịch vụ cập nhật qua updateContext (không re-init).
//   - `callback` gói trong options → manager là callback, map type SDK sang model APP rồi phát ra.
//
//  LƯU Ý: file này import SDK nên CỐ TÌNH GIỮ NHỎ (không nhét UI nặng) — xem ghi chú ở
//  ThemePreviewViewController về swift-frontend `deserializeClass`.
//

import UIKit
import PromotionSDK

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
    let expireDate: String?
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

/// Ưu đãi đủ điều kiện áp cho đơn (kết quả headless findEligibleOffers).
struct EligibleOffer {
    let id: String
    let name: String
    let objectType: String
    let usable: Bool
    let estimatedDiscount: String?
    let expireDate: String?
    let ineligibleReason: String?
}

/// Hai nhóm ưu đãi đủ điều kiện: "của tôi" (voucher đã sở hữu) và "khác" (campaign công khai).
struct EligibleOffers {
    let mine: [EligibleOffer]
    let others: [EligibleOffer]
    let mineIsLastPage: Bool
    let othersIsLastPage: Bool
}

/// Chi tiết một voucher (kết quả headless fetchVoucherDetail).
struct VoucherDetail {
    let id: String
    let merchantName: String
    let title: String
    let description: String
    let guideline: String
    let startDate: String?
    let expireDate: String?
    let bannerURL: String?
    let logoURL: String?
    let statusLabel: String?
}

// MARK: - Cổng app-facing (app phụ thuộc protocol này, dễ mock/test, dễ thay SDK)

protocol PromotionServing: AnyObject {
    /// Gọi sau khi login thành công. `availableServices` do host cung cấp (cho bottom sheet "Chọn dịch vụ").
    func start(customerId: String, token: String?, availableServices: [AvailableService])
    /// Gọi khi access token được refresh — manager tự tạo lại instance với token mới.
    func updateToken(_ token: String?)
    /// Cập nhật context đơn hàng / dịch vụ mỗi khi vào màn có voucher (không re-init). Đối ứng Android.
    func updateContext(orderId: String?, orderValue: String?, serviceCode: String?, metaData: String?)
    /// Gọi khi logout.
    func stop()

    /// Mở màn "Ưu đãi của tôi / Chi tiết" (không cần order).
    func openMyPromotions(from viewController: UIViewController)
    /// Mở thẳng màn chi tiết một ưu đãi, không qua danh sách. Host dùng khi đã biết `voucherId`
    /// (vd: bấm vào push notification, hoặc deeplink từ banner ngoài SDK).
    func openPromotionDetail(voucherId: String, from viewController: UIViewController)
    /// Tạo widget cho màn thanh toán (truyền order tại đây, không re-init).
    func makeCheckoutWidget(from viewController: UIViewController, order: OrderContext) -> UIView

    // --- Headless API (không UI) ---
    /// Lấy danh sách voucher của khách (Search Customer Vouchers).
    func fetchVouchers(keyword: String?, serviceCode: String?, tab: String?,
                       page: Int,
                       completion: @escaping (Result<VoucherPage, Error>) -> Void)
    /// Lấy ưu đãi đủ điều kiện cho đơn (voucher đã sở hữu + campaign công khai).
    func findEligibleOffers(order: OrderContext, tab: String?, myPage: Int, otherPage: Int,
                            completion: @escaping (Result<EligibleOffers, Error>) -> Void)
    /// Lấy chi tiết một voucher theo id (dùng khi host tự dựng màn chi tiết).
    func fetchVoucherDetail(voucherId: String, serviceCode: String?,
                            completion: @escaping (Result<VoucherDetail, Error>) -> Void)
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
    /// Feature flag báo tắt → host nên ẩn điểm vào ưu đãi.
    var onAvailabilityChanged: ((_ enabled: Bool) -> Void)? { get set }
    /// Màn hình SDK đóng (user back).
    var onClosed: (() -> Void)? { get set }
}

// MARK: - Manager: chỗ DUY NHẤT chạm PRMSDK SDK

final class PromotionManager: NSObject, PromotionServing {

    /// Singleton — ép đúng ràng buộc "1 instance SDK sống tại 1 thời điểm".
    /// (Có DI container thì inject `PromotionServing` thay cho singleton cũng được.)
    static let shared = PromotionManager()
    private override init() {}

    // Sự kiện (fan-out từ callback 1-1 của SDK ra nhiều listener của app)
    var onVoucherApplied: ((String) -> Void)?
    var onVoucherCleared: (() -> Void)?
    var onVoucherCountChanged: ((Int) -> Void)?
    var onServiceSelected: ((ServiceSelection) -> Void)?
    var onAvailabilityChanged: ((Bool) -> Void)?
    var onClosed: (() -> Void)?

    /// Adapter riêng conform `PRMSDKCallback` — tách khỏi manager để tên method callback
    /// (`onVoucherCleared()`, `onClosed()`…) không đụng các closure sự kiện cùng tên ở trên.
    private lazy var sdkCallback = PromotionCallbackAdapter(owner: self)

    private var customerId: String?
    private var availableServices: [AvailableService] = []   // host truyền xuống qua start(...)

    /// SDK đã sẵn sàng chưa (đã `initialize`, chưa `release`). Công cụ Playground dùng để gác.
    var isReady: Bool { PRMSDK.isInitialized() }

    // Theme cấu hình tập trung 1 chỗ (nil = mặc định SDK).
    private static let theme: PRMSDKTheme? = nil   // hoặc PRMSDKTheme(button: ...)
    // Base URL Promotion BFF — host cấu hình. Đối ứng `PRMSessionConfig.baseUrl` bên Android.
    private static let baseUrl = "http://125.235.38.229:8080/"

    // MARK: Lifecycle

    func start(customerId: String, token: String?, availableServices: [AvailableService]) {
        self.customerId = customerId
        self.availableServices = availableServices
        rebuild(token: token)
    }

    func updateToken(_ token: String?) {
        rebuild(token: token)   // token bị chụp lúc init → refresh = tạo lại instance
    }

    func updateContext(orderId: String?, orderValue: String?, serviceCode: String?, metaData: String?) {
        PRMSDK.updateContext(orderId: orderId, orderValue: orderValue, serviceCode: serviceCode, metaData: metaData)
    }

    func stop() {
        PRMSDK.release()
        customerId = nil
        availableServices = []
    }

    private func rebuild(token: String?) {
        guard let customerId else { return }
        // Đối ứng Android: PRMSDK.initialize(options) một lần; refresh token = initialize lại
        // với session mới. callback + theme + danh mục dịch vụ gói trong options.
        // Release trước khi init lại để đóng HttpClient cũ + clear DI (initialize KHÔNG tự release) —
        // khớp Android `rebuild`. `release()` gọi khi chưa init là vô hại.
        if PRMSDK.isInitialized() { PRMSDK.release() }
        PRMSDK.initialize(
            options: PRMSDKOptions(
                session: PRMSessionConfig(
                    customerId: customerId,
                    accessToken: token ?? "",
                    baseUrl: Self.baseUrl,
                    language: "vi-VN",
                    environment: .prod
                ),
                availableServices: availableServices.map {
                    PRMAvailableService(serviceCode: $0.code, serviceName: $0.name, serviceType: $0.type, iconUrl: $0.iconUrl)
                },
                theme: Self.theme,
                callback: sdkCallback
            )
        )
    }

    // MARK: Điều hướng / UI

    func openMyPromotions(from viewController: UIViewController) {
        PRMSDK.openMyPromotion(from: viewController)
    }

    func openPromotionDetail(voucherId: String, from viewController: UIViewController) {
        PRMSDK.openPromotionDetail(voucherId: voucherId, from: viewController)
    }

    func makeCheckoutWidget(from viewController: UIViewController, order: OrderContext) -> UIView {
        // Order truyền tại đây → cập nhật context rồi dựng widget, không re-init SDK. Đối ứng Android:
        // PRMSDK.updateContext(...) trước khi mở màn có voucher.
        PRMSDK.updateContext(orderId: order.id, orderValue: order.value)
        return PRMSDK.createEndowView(from: viewController)
    }

    // MARK: Headless

    func fetchVouchers(keyword: String? = nil, serviceCode: String? = nil, tab: String? = nil,
                       page: Int = 0,
                       completion: @escaping (Result<VoucherPage, Error>) -> Void) {
        guard PRMSDK.isInitialized() else { return completion(.failure(Self.notReady)) }
        PRMSDK.api.getVouchers(keyword: keyword, serviceCode: serviceCode, tab: tab,
                                 page: page) { result in
            switch result {
            case .success(let r):
                completion(.success(VoucherPage(
                    mine: r.vouchers.map(Self.map),
                    mineIsLastPage: r.isLastPage
                )))
            case .failure(let error): completion(.failure(error))
            }
        }
    }

    func findEligibleOffers(order: OrderContext, tab: String?, myPage: Int, otherPage: Int,
                            completion: @escaping (Result<EligibleOffers, Error>) -> Void) {
        guard PRMSDK.isInitialized() else { return completion(.failure(Self.notReady)) }
        PRMSDK.api.findEligible(orderId: order.id, orderValue: order.value,
                                      tabCode: tab, myPage: myPage, otherPage: otherPage) { result in
            switch result {
            case .success(let r):
                completion(.success(EligibleOffers(
                    mine: r.myOffers.map(Self.map),
                    others: r.otherOffers.map(Self.map),
                    mineIsLastPage: r.myIsLastPage,
                    othersIsLastPage: r.otherIsLastPage
                )))
            case .failure(let error): completion(.failure(error))
            }
        }
    }

    func fetchVoucherDetail(voucherId: String, serviceCode: String?,
                            completion: @escaping (Result<VoucherDetail, Error>) -> Void) {
        guard PRMSDK.isInitialized() else { return completion(.failure(Self.notReady)) }
        PRMSDK.api.getVoucherDetail(voucherId: voucherId, serviceCode: serviceCode) { result in
            switch result {
            case .success(let r): completion(.success(Self.map(r)))
            case .failure(let error): completion(.failure(error))
            }
        }
    }

    func validate(order: OrderContext, voucherIds: [String],
                  completion: @escaping (Result<ValidationSummary, Error>) -> Void) {
        guard PRMSDK.isInitialized() else { return completion(.failure(Self.notReady)) }
        PRMSDK.api.validateDiscounts(orderId: order.id, orderValue: order.value, voucherIds: voucherIds) { result in
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
        guard PRMSDK.isInitialized() else { return completion(.failure(Self.notReady)) }
        PRMSDK.api.createRedemption(orderId: order.id, orderValue: order.value, voucherIds: [voucherId]) { result in
            switch result {
            case .success(let r): completion(.success(r.sessionId))   // map type SDK → String cho app
            case .failure(let error): completion(.failure(error))
            }
        }
    }

    // MARK: Mapping SDK → app model (anti-corruption)

    private static let notReady = NSError(domain: "PromotionManager", code: -1,
                                          userInfo: [NSLocalizedDescriptionKey: "SDK chưa khởi tạo (chưa login?)"])

    private nonisolated static func map(_ v: PRMVoucher) -> VoucherSummary {
        VoucherSummary(id: v.id, merchantName: v.merchantName, title: v.title, imageURL: v.imageURL,
                       expireDate: v.expireDate, isUsed: v.isUsed, statusLabel: v.displayStatusLabel)
    }

    private nonisolated static func map(_ o: PRMEligibleOffer) -> EligibleOffer {
        EligibleOffer(id: o.id, name: o.name, objectType: o.objectType, usable: o.usable,
                      estimatedDiscount: o.estimatedDiscount, expireDate: o.expireDate,
                      ineligibleReason: o.ineligibleReason)
    }

    private nonisolated static func map(_ d: PRMVoucherDetail) -> VoucherDetail {
        VoucherDetail(id: d.id, merchantName: d.merchantName, title: d.title, description: d.description,
                      guideline: d.guideline, startDate: d.startDate, expireDate: d.expireDate,
                      bannerURL: d.bannerURL, logoURL: d.logoURL, statusLabel: d.displayStatusLabel)
    }
}

// MARK: - Adapter callback SDK (1-1) → map sang model app rồi phát ra ngoài
//
// Tách khỏi PromotionManager để tên method của `PRMSDKCallback` (`onVoucherCleared()`,
// `onClosed()`…) không đụng các closure sự kiện cùng tên trên manager. Giữ `weak owner` — manager
// sở hữu adapter (strong lazy), nên vòng đời khớp nhau.

private final class PromotionCallbackAdapter: PRMSDKCallback {

    private weak var owner: PromotionManager?

    init(owner: PromotionManager) { self.owner = owner }

    func onVoucherApplied(voucherId: String) {
        owner?.onVoucherApplied?(voucherId)
    }

    func onVoucherCleared() {
        owner?.onVoucherCleared?()
    }

    func onVoucherCountChanged(count: Int) {
        owner?.onVoucherCountChanged?(count)
    }

    func onServiceSelected(selection: PRMServiceSelection) {
        owner?.onServiceSelected?(ServiceSelection(
            voucherId: selection.voucherId,
            code: selection.serviceCode,
            name: selection.serviceName,
            iconUrl: selection.iconUrl
        ))
    }

    func onAvailabilityChanged(enabled: Bool) {
        owner?.onAvailabilityChanged?(enabled)
    }

    func onClosed() {
        owner?.onClosed?()
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
