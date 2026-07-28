//
//  PromotionUIStrings.swift
//  PromotionSDK
//
//  Gom chuỗi hiển thị tiếng Việt của tầng UI vào MỘT nơi — parity với `strings.xml` bên Android
//  (store/promotionLogic cố ý không giữ chuỗi; native lo hiển thị). Trước đây literal nằm rải trong
//  cell/ViewModel. Bước sau có thể chuyển sang `Localizable.strings` mà không đổi call-site.
//

import Foundation

enum PromotionUIStrings {
    static let useNow = "Sử dụng ngay"
    static let use = "Sử dụng"
    /// Nút màn Chi tiết khi vào từ luồng thanh toán (TLNV MOB_002 control #5) — `prm_apply` bên Android.
    static let apply = "Áp dụng"
    /// TẠM THỜI: phản hồi sau khi chọn dịch vụ, trong lúc chưa có đích điều hướng thật.
    /// Đối ứng `prm_service_selected` bên Android.
    static func serviceSelected(_ name: String) -> String { "Đã chọn dịch vụ: \(name)" }
    static let used = "Đã sử dụng"
    static let expired = "Đã hết hạn"
    static let ineligible = "Không đủ điều kiện"
    static let detail = "Chi tiết"
    static let myPromotions = "Ưu đãi của tôi"
    static let otherPromotions = "Ưu đãi khác"
    /// Hai tab màn chi tiết — đối ứng `prm_tab_detail_info` / `prm_tab_usage_guide` bên Android.
    static let tabDetailInfo = "Thông tin chi tiết"
    static let tabUsageGuide = "Hướng dẫn sử dụng"
    /// Empty-view màn "Ưu đãi của tôi" — đối ứng `prm_deal_hot` / `prm_hot_deal_description` bên Android.
    static let emptyPromotionsTitle = "Ngàn deal HOT chờ bạn"
    static let emptyPromotionsDescription = "Lấp đầy kho quà với thật nhiều ưu đãi hấp dẫn bạn nhé!"

    /// "HSD: 20/05/2026"
    static func expiryDate(_ value: String) -> String { "HSD: \(value)" }
    /// API không trả HSD (nil/rỗng) = voucher không có hạn dùng — đối ứng `prm_expiry_never`.
    static let expiryNever = "HSD: Không hết hạn"
    /// "HSD còn 3 ngày" — số ngày do store (promotionLogic) quyết định.
    static func remainingDays(_ days: Int) -> String { "HSD còn \(days) ngày" }
    /// "Giảm 100.000đ"
    static func discount(_ value: String) -> String { "Giảm \(value)đ" }
    /// "Hạn sử dụng 20/05/2026"
    static func expiryDateLong(_ value: String) -> String { "Hạn sử dụng \(value)" }
    /// "Đã chọn 2 voucher" — đối ứng `prm_selected_voucher_count` bên Android.
    static func selectedVoucherCount(_ count: Int) -> String { "Đã chọn \(count) voucher" }

    /// Map mã lỗi (raw từ store) → chuỗi hiển thị — **dùng chung mọi màn**, KHỚP wording Android
    /// (`R.string.prm_*`). Mã là chữ THƯỜNG khớp `ErrorCodes` của lõi (trước đây iOS so `"MISSING_CUSTOMER_ID"`
    /// chữ HOA → không bao giờ khớp, luôn rơi default).
    static func errorMessage(_ code: String) -> String {
        switch code {
        case "missing_customer_id": return "Không tìm thấy thông tin khách hàng"
        case "no_result", "error_detail_unavailable": return "Không tìm thấy kết quả phù hợp"
        default: return "Đã có lỗi xảy ra. Vui lòng thử lại sau"
        }
    }
}
