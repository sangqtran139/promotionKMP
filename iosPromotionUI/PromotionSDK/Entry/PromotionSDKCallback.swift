//
//  PromotionSDKCallback.swift
//  PromotionSDK
//

import Foundation

/// Dữ liệu trả về host khi user chọn 1 dịch vụ trong bottom sheet "Chọn dịch vụ".
public struct PromotionSDKServiceSelection {
    /// ID voucher đang thao tác (bấm "Dùng"/"Áp dụng").
    public let voucherId: String
    /// Mã dịch vụ được chọn (khớp `serviceCode` host cấu hình / `productId` của voucher).
    public let serviceCode: String
    public let serviceName: String
    public let iconUrl: String

    public init(voucherId: String, serviceCode: String, serviceName: String, iconUrl: String) {
        self.voucherId = voucherId
        self.serviceCode = serviceCode
        self.serviceName = serviceName
        self.iconUrl = iconUrl
    }
}

public protocol PromotionSDKCallback: AnyObject {
    /// Gọi khi user chọn và bấm "Áp dụng" ưu đãi thành công.
    func vdsPromotion(_ sdk: PromotionSDK, didApplyVoucherId voucherId: String)

    /// Gọi khi user bấm "Hủy" để bỏ chọn ưu đãi trên widget.
    func vdsPromotionDidClearVoucher(_ sdk: PromotionSDK)

    /// Gọi khi màn hình SDK bị đóng (user back).
    func vdsPromotionDidClose(_ sdk: PromotionSDK)

    /// Gọi khi widget load xong và biết tổng số voucher khả dụng.
    func vdsPromotion(_ sdk: PromotionSDK, didUpdateVoucherCount count: Int)

    /// Gọi khi user chọn 1 dịch vụ trong bottom sheet "Chọn dịch vụ" (từ màn Ưu đãi của tôi / Tìm kiếm / Chi tiết).
    func vdsPromotion(_ sdk: PromotionSDK, didSelectService selection: PromotionSDKServiceSelection)

    /// Gọi khi biết chắc trạng thái bật/tắt SDK qua feature flag (Unleash).
    /// `enabled == false` → host nên ẩn toàn bộ điểm vào ưu đãi (entry point, widget).
    func vdsPromotion(_ sdk: PromotionSDK, didUpdateAvailability enabled: Bool)
}

public extension PromotionSDKCallback {
    func vdsPromotionDidClearVoucher(_ sdk: PromotionSDK) {}
    func vdsPromotionDidClose(_ sdk: PromotionSDK) {}
    func vdsPromotion(_ sdk: PromotionSDK, didUpdateVoucherCount count: Int) {}
    func vdsPromotion(_ sdk: PromotionSDK, didSelectService selection: PromotionSDKServiceSelection) {}
    func vdsPromotion(_ sdk: PromotionSDK, didUpdateAvailability enabled: Bool) {}
}
