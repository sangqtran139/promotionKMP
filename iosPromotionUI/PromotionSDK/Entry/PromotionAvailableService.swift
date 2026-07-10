//
//  PromotionAvailableService.swift
//  PromotionSDK
//
//  Public config: danh mục dịch vụ khả dụng do host cung cấp. Khi user bấm "Áp dụng"
//  trên một voucher, SDK lọc các dịch vụ ở đây theo `applicableProducts` của voucher
//  để hiển thị bottom sheet "Chọn dịch vụ" (parity Android `PromotionSDKConfig.availableServices`).
//

import Foundation

/// Một dịch vụ khả dụng mà voucher có thể áp dụng (host cung cấp lúc khởi tạo SDK).
/// `serviceCode` phải khớp `productId` trong `applicableProducts` của voucher để được hiển thị.
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

/// Holder nội bộ cấu hình phiên SDK (giống `ServiceUrl.promotionBaseURLOverride` cho base URL).
/// Đặt một lần lúc khởi tạo `PromotionSDK`, các màn đọc lại khi cần (single SDK instance).
enum PromotionSessionConfig {
    static var availableServices: [PromotionAvailableService] = []
    /// Callback khi user chọn dịch vụ trong bottom sheet — set 1 lần bởi `PromotionSDKImpl`, các màn
    /// (Ưu đãi của tôi / Tìm kiếm / Chi tiết) gọi khi item được chọn. Map → delegate public ở facade.
    static var onServiceSelected: ((_ voucherId: String, _ service: ServiceSelectorItem) -> Void)?
}
