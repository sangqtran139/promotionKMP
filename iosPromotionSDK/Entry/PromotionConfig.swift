//
//  PromotionConfig.swift
//  PromotionSDK
//
//  Cấu hình phiên & context — mirror `PromotionConfig.kt` bên Android (cùng nội dung, cùng thứ tự):
//  `PromotionSessionConfig`, `PromotionAvailableService`, `PromotionEnvironment`, map public→core,
//  và `PromotionMutableContext` (nội bộ).
//
//  Type public ở đây chỉ dùng String/Foundation nên KHÔNG lộ PRMKotlinBridge ra module interface;
//  phần map + context (nội bộ) mới chạm lõi Kotlin qua `@_implementationOnly`.
//

import Foundation
@_implementationOnly import PRMKotlinBridge

/// Thông tin phiên đăng nhập và cấu hình kết nối — truyền 1 lần lúc `PromotionSDK.initialize`.
/// Để cập nhật đơn hàng / dịch vụ mỗi khi vào màn, dùng `PromotionSDK.updateOrderInfo`.
public struct PromotionSessionConfig {
    public let accessToken: String
    public let baseUrl: String
    public let language: String
    public let environment: PromotionEnvironment

    public init(
        accessToken: String,
        baseUrl: String,
        language: String = "vi-VN",
        environment: PromotionEnvironment = .prod
    ) {
        self.accessToken = accessToken
        self.baseUrl = baseUrl
        self.language = language
        self.environment = environment
    }
}

/// Một dịch vụ khả dụng mà voucher có thể áp dụng.
///
/// `productId` phải khớp `productId` trong `applicableProducts` của voucher thì dịch vụ mới hiện
/// ở bottom sheet "Chọn dịch vụ".
public struct PromotionAvailableService {
    public let productId: String
    public let productName: String
    public let skuSourceId: String
    public let iconUrl: String

    public init(productId: String, productName: String, skuSourceId: String = "", iconUrl: String = "") {
        self.productId = productId
        self.productName = productName
        self.skuSourceId = skuSourceId
        self.iconUrl = iconUrl
    }
}

public enum PromotionEnvironment {
    case prod
    case staging
}

// ─── Public → core ──────────────────────────────────────────────────────────

extension PromotionEnvironment {
    /// Map môi trường public → enum của lõi Kotlin. Đối ứng nhánh `when` trong `toCoreConfig` bên Android.
    func toCore() -> SdkEnvironment {
        switch self {
        case .prod: return SdkEnvironment.prod
        case .staging: return SdkEnvironment.staging
        }
    }
}

extension PromotionSDKOptions {
    /// Public → core. Đối ứng `PromotionSDKOptions.toCoreConfig(contextProvider)` bên Android.
    ///
    /// `availableServices` bơm thẳng vào core config (giống Android) → `ServiceSelectorBuilder` đọc lại
    /// từ lõi qua `PromotionContainer.requireConfig()`. Khác Android (N1): `isDebug` truyền vào (iOS
    /// không có `ApplicationInfo.FLAG_DEBUGGABLE`).
    func toCoreConfig(context: PromotionMutableContext, isDebug: Bool) -> PromotionSDKConfig {
        // `context` giờ đã mang `availableServices` → uỷ thẳng cho `context.toCoreConfig`, một nguồn.
        context.toCoreConfig(isDebug: isDebug)
    }
}

/// Giữ toàn bộ context mà SDK cần — tĩnh (session) + động (đơn hàng / dịch vụ).
/// `PromotionSDK.updateOrderInfo` ghi trực tiếp vào đây; instance được tạo mới mỗi `PromotionSDK.initialize`.
///
/// Đối ứng `PromotionMutableContext` bên Android; thay cho `HostRequestContextProvider` cũ. Lõi Kotlin
/// đọc lại các giá trị này ở **mỗi** request qua `PromotionRequestContextProvider`.
final class PromotionMutableContext: NSObject, PromotionRequestContextProvider {

    let session: PromotionSessionConfig
    /// Danh mục dịch vụ host cấu hình — giữ ở đây để `PromotionSDK.updateToken` dựng lại config
    /// không cần host truyền lại (đối ứng `PromotionMutableContext.availableServices` bên Android).
    let availableServices: [PromotionAvailableService]

    var orderId: String?
    var orderValue: String?
    var serviceCode: String?
    var metaData: String?
    /// Order items (SKU) của đơn hiện tại — lõi đọc qua `getOrderItems()` cho `findEligible`.
    var orderItems: [PromotionOrderItem] = []

    init(session: PromotionSessionConfig, availableServices: [PromotionAvailableService] = []) {
        self.session = session
        self.availableServices = availableServices
        super.init()
    }

    /// Dựng core config từ chính context này (dùng khi `updateToken` không có `PromotionSDKOptions`).
    /// Đối ứng `PromotionMutableContext.toCoreConfig()` bên Android.
    func toCoreConfig(isDebug: Bool) -> PromotionSDKConfig {
        PromotionSDKConfig(
            baseUrl: session.baseUrl,
            requestContextProvider: self,
            environment: session.environment.toCore(),
            availableServices: availableServices.map {
                AvailableService(productId: $0.productId, productName: $0.productName,
                                 skuSourceId: $0.skuSourceId, iconUrl: $0.iconUrl)
            },
            isDebug: isDebug
        )
    }

    /// Lõi tự thêm tiền tố `Bearer ` nếu chuỗi chưa có.
    func getAccessToken() -> String? { session.accessToken }

    func getLanguage() -> String? { session.language }
    func getOrderId() -> String? { orderId }
    func getOrderValue() -> String? { orderValue }
    func getService() -> String? { serviceCode }
    func getMetaData() -> String? { metaData }

    /// Map order items (public) → model lõi Kotlin cho Find Eligible Campaigns — dùng chung với
    /// `ChoosePromotionStore`/`EndowStore`.
    func getOrderItems() -> [EligibleOrderItem] {
        orderItems.map {
            EligibleOrderItem(
                skuSourceId: $0.skuSourceId,
                quantity: Int32($0.quantity),
                unitPrice: $0.unitPrice,
                orderItemId: nil,
                productId: $0.productId,
                productName: $0.productName,
                productCategory: $0.productCategory
            )
        }
    }
}
