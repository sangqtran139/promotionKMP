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

/// Nguồn access token của host — **cách duy nhất** token đi vào SDK. Đối ứng
/// `PromotionTokenSource` bên Android (cùng hai hàm, cùng thứ tự).
///
/// SDK không giữ bản sao token nào. Mọi request đều hỏi lại `currentToken()`; ăn 401 thì hỏi
/// `refreshToken(_:)` đúng một lần rồi thử lại; vẫn không được thì hỏng luôn.
///
/// ```swift
/// final class MyTokenSource: PromotionTokenSource {
///     func currentToken() -> String? { auth.accessToken }
/// }
///
/// PromotionSDK.initialize(tokenSource: MyTokenSource(), baseUrl: baseUrl)
/// ```
///
/// **Vòng đời — bẫy dễ dính nhất:** `PromotionSDK` là singleton, nó giữ object này tới tận
/// `release()`. Cho một `UIViewController` conform protocol này rồi truyền `self` là giữ màn hình đó
/// vĩnh viễn, và token đóng băng ở giá trị cuối. Trỏ vào kho token **cấp app** (singleton), đừng trỏ
/// vào màn hình.
public protocol PromotionTokenSource: AnyObject {

    /// Token hiện hành của host. SDK gọi ở **mỗi** request — chính vì vậy host không cần báo gì cho
    /// SDK khi token đổi: cơ chế nào của app ghi vào kho lúc nào cũng được, lượt gọi kế tiếp tự dùng
    /// giá trị mới.
    ///
    /// **Hợp đồng — bắt buộc đọc:** hàm này nằm trên đường dựng header của mọi request, nên nó phải
    /// là một phép **đọc biến trong bộ nhớ**: không I/O, không chặn, không gọi mạng. Lõi Kotlin gọi
    /// nó từ **thread nền**, nên nó cũng không được `@MainActor`; ô nhớ nguồn phải thread-safe
    /// (`NSLock`, serial queue…). Đọc Keychain ở đây là tự cắm một lần chặn vào từng lượt gọi API.
    ///
    /// Trả `nil`/rỗng → SDK gửi request **không kèm** header `Authorization`.
    func currentToken() -> String?

    /// SDK ăn **HTTP 401** → xin host lấy token mới. Host gọi `onResult` với `true` khi kho token đã
    /// có token mới, `false` khi chịu. SDK thử lại request hỏng **đúng một lần** nếu `true`; `false`
    /// thì để lỗi `TOKEN_EXPIRED` nổi lên và bắn `PromotionSDKCallback.onExpireToken()`.
    ///
    /// **Mặc định là `false`** (xem extension dưới) — host không cài đặt thì 401 hỏng ngay, không
    /// chờ. Đúng hành vi cho app không có cách lấy token mới theo yêu cầu.
    ///
    /// `Bool` chứ không phải token mới là cố ý: token vào SDK theo **một** đường duy nhất là
    /// `currentToken()`. Host ghi token mới vào kho của mình **rồi** báo `true` — không có đường thứ
    /// hai để nhầm.
    ///
    /// **Ràng buộc:**
    /// - Gọi `onResult` **đúng một lần**, kể cả khi hỏng. Không gọi thì SDK chờ tối đa 15 giây rồi
    ///   coi như `false`.
    /// - Được gọi từ thread nền; trả kết quả từ thread nào cũng được.
    /// - Một thời điểm SDK chỉ gọi **một** lượt dù bao nhiêu request cùng ăn 401. Nhưng cơ chế của
    ///   host vẫn nên single-flight: hai màn mở cách nhau vài giây vẫn có thể chạm vào hai lần.
    ///
    /// ```swift
    /// func refreshToken(_ onResult: @escaping (Bool) -> Void) {
    ///     auth.refresh { newToken in
    ///         if let newToken { auth.accessToken = newToken }   // ghi vào kho TRƯỚC
    ///         onResult(newToken != nil)
    ///     }
    /// }
    /// ```
    func refreshToken(_ onResult: @escaping (Bool) -> Void)
}

public extension PromotionTokenSource {
    /// Mặc định: chịu ngay. Swift không thừa hưởng default của interface Kotlin, nên bản mặc định
    /// này phải khai ở đây — đối ứng `= onResult(false)` trên interface bên Android.
    func refreshToken(_ onResult: @escaping (Bool) -> Void) { onResult(false) }
}

/// Thông tin phiên và cấu hình kết nối — truyền 1 lần lúc `PromotionSDK.initialize`.
/// Để cập nhật đơn hàng / dịch vụ mỗi khi vào màn, dùng `PromotionSDK.updateOrderInfo`.
///
/// Không có field `accessToken`: token là thứ **thay đổi theo thời gian**, nên nó vào SDK dưới dạng
/// một nguồn (`tokenSource`) chứ không phải một chuỗi chụp sẵn.
public struct PromotionSessionConfig {
    public let tokenSource: PromotionTokenSource
    public let baseUrl: String
    public let language: String
    public let environment: PromotionEnvironment

    public init(
        tokenSource: PromotionTokenSource,
        baseUrl: String,
        language: String = "vi-VN",
        environment: PromotionEnvironment = .prod
    ) {
        self.tokenSource = tokenSource
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
    /// Danh mục dịch vụ host cấu hình — giữ ở đây để dựng lại core config
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

    /// Dựng core config từ chính context này (khi không có `PromotionSDKOptions` trong tay).
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

    /// Hai hàm dưới đây chỉ **chuyển tiếp** sang `PromotionTokenSource` của host — không cache,
    /// không fallback, không nhánh. Đó là điểm của thiết kế: token có đúng một nguồn, nên không tồn
    /// tại trạng thái nào của SDK để lệch với host.
    ///
    /// Lõi gọi `getAccessToken()` ở **mỗi** request qua `defaultRequest { }`, và tự thêm tiền tố
    /// `Bearer ` nếu chuỗi chưa có. Đối ứng `PromotionMutableContext.getAccessToken()` bên Android.
    func getAccessToken() -> String? { session.tokenSource.currentToken() }

    /// Đối ứng `PromotionMutableContext.refreshAccessToken(onResult:)` bên Android.
    func refreshAccessToken(onResult: @escaping (KotlinBoolean) -> Void) {
        session.tokenSource.refreshToken { didRefresh in onResult(KotlinBoolean(bool: didRefresh)) }
    }

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
