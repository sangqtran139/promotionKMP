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

    /// Gọi khi user chọn 1 dịch vụ trong bottom sheet "Chọn dịch vụ" (từ màn Ưu đãi của tôi / Tìm kiếm / Chi tiết).
    func onServiceSelected(selection: PromotionServiceSelection)

    // Ba doc comment từng đứng xen giữa các method ở đây — cho "đếm voucher của widget", "trạng
    // thái bật/tắt SDK theo feature flag", và "màn SDK bị đóng". Method của chúng đã bỏ, doc thì ở
    // lại: đọc file này thì tưởng protocol có 6 sự kiện, và `PromotionSDK.swift` còn hứa với host ở
    // ba chỗ nữa. Quyết định loại ba sự kiện đó ghi ở `docs/common/InitParity.md` §3 mục "Đã loại".
    // Host cần cờ tính năng thì dùng `PromotionSDK.refreshFeatureFlags`.

    /// Gọi khi 1 API bên trong màn SDK (Ưu đãi của tôi / Tìm kiếm / Chi tiết / Chọn ưu đãi / widget
    /// Endow) trả về HTTP 401 và **không cứu được** — tức `PromotionTokenSource.refreshToken(_:)` đã
    /// báo `false`, hoặc host không cài đặt nó.
    ///
    /// Nghĩa là phiên đã chết thật: host nên điều hướng user về màn đăng nhập. Host **không** cần đẩy
    /// token mới vào SDK — SDK tự đọc lại qua `PromotionTokenSource.currentToken()` ở mỗi request.
    /// Đối ứng `onExpireToken()` bên Android — xem docs/common/InitParity.md §3.
    func onExpireToken()
}

public extension PromotionSDKCallback {
    func onVoucherApplied(voucherId: String) {}
    func onServiceSelected(selection: PromotionServiceSelection) {}
    func onExpireToken() {}
}
