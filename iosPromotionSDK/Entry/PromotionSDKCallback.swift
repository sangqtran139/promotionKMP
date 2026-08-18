//
//  PromotionSDKCallback.swift
//  PromotionSDK
//
//  Callback sự kiện SDK — set qua `PromotionSDKOptions.callback`.
//  Tên type / method / tham số **trùng chữ** với `PromotionSDKCallback` bên Android
//  (xem docs/InitParity.md §3). SDK là singleton nên **không** truyền `sdk` vào method.
//

import Foundation

/// Dữ liệu trả về host khi user chọn 1 dịch vụ trong bottom sheet "Chọn dịch vụ".
/// Đối ứng `PromotionServiceSelection` bên Android.
public struct PromotionServiceSelection {
    /// ID voucher đang thao tác (bấm "Dùng"/"Áp dụng").
    public let voucherId: String
    /// Mã dịch vụ được chọn (khớp `productId` host cấu hình / `productId` của voucher).
    public let productId: String
    public let productName: String
    public let skuSourceId: String
    public let iconUrl: String

    public init(voucherId: String, productId: String, productName: String, skuSourceId: String = "", iconUrl: String) {
        self.voucherId = voucherId
        self.productId = productId
        self.productName = productName
        self.skuSourceId = skuSourceId
        self.iconUrl = iconUrl
    }
}

public protocol PromotionSDKCallback: AnyObject {
    /// Gọi khi user chọn và bấm "Áp dụng" ưu đãi thành công.
    func onVoucherApplied(voucherId: String)


    /// Gọi khi widget load xong và biết tổng số voucher khả dụng.

    /// Gọi khi user chọn 1 dịch vụ trong bottom sheet "Chọn dịch vụ" (từ màn Ưu đãi của tôi / Tìm kiếm / Chi tiết).
    func onServiceSelected(selection: PromotionServiceSelection)

    /// Gọi khi biết chắc trạng thái bật/tắt SDK qua feature flag (Unleash).
    /// `enabled == false` → host nên ẩn toàn bộ điểm vào ưu đãi (entry point, widget).

    /// Gọi khi màn hình SDK bị đóng (user back).

    /// Gọi khi 1 API bên trong màn SDK (Ưu đãi của tôi / Tìm kiếm / Chi tiết / Chọn ưu đãi / widget
    /// Endow) trả về HTTP 401/403 — token hết hạn hoặc không hợp lệ. Host nên refresh token rồi gọi
    /// lại `PromotionSDK.updateToken` (hoặc điều hướng user về màn đăng nhập). Đối ứng
    /// `onExpireToken()` bên Android — xem docs/common/InitParity.md §3.
    func onExpireToken()
}

public extension PromotionSDKCallback {
    func onVoucherApplied(voucherId: String) {}
    func onServiceSelected(selection: PromotionServiceSelection) {}
    func onExpireToken() {}
}
