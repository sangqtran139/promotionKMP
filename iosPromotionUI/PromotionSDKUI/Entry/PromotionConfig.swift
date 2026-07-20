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
/// Để cập nhật đơn hàng / dịch vụ mỗi khi vào màn, dùng `PromotionSDK.updateContext`.
public struct PromotionSessionConfig {
    public let customerId: String
    public let accessToken: String
    public let baseUrl: String
    public let language: String
    public let environment: PromotionEnvironment

    public init(
        customerId: String,
        accessToken: String,
        baseUrl: String,
        language: String = "vi-VN",
        environment: PromotionEnvironment = .prod
    ) {
        self.customerId = customerId
        self.accessToken = accessToken
        self.baseUrl = baseUrl
        self.language = language
        self.environment = environment
    }
}

/// Một dịch vụ khả dụng mà voucher có thể áp dụng.
///
/// `serviceCode` phải khớp `productId` trong `applicableProducts` của voucher thì dịch vụ mới hiện
/// ở bottom sheet "Chọn dịch vụ".
public struct PromotionAvailableService {
    public let serviceCode: String
    public let serviceName: String
    public let serviceType: String
    public let iconUrl: String

    public init(serviceCode: String, serviceName: String, serviceType: String = "", iconUrl: String = "") {
        self.serviceCode = serviceCode
        self.serviceName = serviceName
        self.serviceType = serviceType
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
        PromotionSDKConfig(
            baseUrl: session.baseUrl.isEmpty ? "https://staging1.viettelmoney.vn/" : session.baseUrl,
            requestContextProvider: context,
            environment: session.environment.toCore(),
            availableServices: availableServices.map {
                AvailableService(
                    serviceCode: $0.serviceCode,
                    serviceName: $0.serviceName,
                    serviceType: $0.serviceType,
                    iconUrl: $0.iconUrl
                )
            },
            isDebug: isDebug
        )
    }
}

/// Giữ toàn bộ context mà SDK cần — tĩnh (session) + động (đơn hàng / dịch vụ).
/// `PromotionSDK.updateContext` ghi trực tiếp vào đây; instance được tạo mới mỗi `PromotionSDK.initialize`.
///
/// Đối ứng `PromotionMutableContext` bên Android; thay cho `HostRequestContextProvider` cũ. Lõi Kotlin
/// đọc lại các giá trị này ở **mỗi** request qua `PromotionRequestContextProvider`.
final class PromotionMutableContext: NSObject, PromotionRequestContextProvider {

    let session: PromotionSessionConfig

    var orderId: String?
    var orderValue: String?
    var serviceCode: String?
    var metaData: String?

    init(session: PromotionSessionConfig) {
        self.session = session
        super.init()
    }

    func getCustomerId() -> String? { session.customerId }

    /// Lõi tự thêm tiền tố `Bearer ` nếu chuỗi chưa có.
    func getAccessToken() -> String? { session.accessToken }

    func getLanguage() -> String? { session.language }
    func getOrderId() -> String? { orderId }
    func getOrderValue() -> String? { orderValue }
    func getService() -> String? { serviceCode }
    func getMetaData() -> String? { metaData }
}
