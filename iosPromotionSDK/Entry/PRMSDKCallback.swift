//
//  PRMSDKCallback.swift
//  PRMSDK
//
//  Callback sự kiện SDK — set qua `PRMSDKOptions.callback`.
//  Tên type / method / tham số **trùng chữ** với `PRMSDKCallback` bên Android
//  (xem docs/InitParity.md §3). SDK là singleton nên **không** truyền `sdk` vào method.
//

import Foundation

/// Dữ liệu trả về host khi user chọn 1 dịch vụ trong bottom sheet "Chọn dịch vụ".
/// Đối ứng `PRMServiceSelection` bên Android.
public struct PRMServiceSelection {
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

public protocol PRMSDKCallback: AnyObject {
    /// Gọi khi user chọn và bấm "Áp dụng" ưu đãi thành công.
    func onVoucherApplied(voucherId: String)

    /// Gọi khi user bấm "Hủy" để bỏ chọn ưu đãi trên widget.
    func onVoucherCleared()

    /// Gọi khi widget load xong và biết tổng số voucher khả dụng.
    func onVoucherCountChanged(count: Int)

    /// Gọi khi user chọn 1 dịch vụ trong bottom sheet "Chọn dịch vụ" (từ màn Ưu đãi của tôi / Tìm kiếm / Chi tiết).
    func onServiceSelected(selection: PRMServiceSelection)

    /// Gọi khi biết chắc trạng thái bật/tắt SDK qua feature flag (Unleash).
    /// `enabled == false` → host nên ẩn toàn bộ điểm vào ưu đãi (entry point, widget).
    func onAvailabilityChanged(enabled: Bool)

    /// Gọi khi màn hình SDK bị đóng (user back).
    func onClosed()
}

public extension PRMSDKCallback {
    func onVoucherApplied(voucherId: String) {}
    func onVoucherCleared() {}
    func onVoucherCountChanged(count: Int) {}
    func onServiceSelected(selection: PRMServiceSelection) {}
    func onAvailabilityChanged(enabled: Bool) {}
    func onClosed() {}
}
