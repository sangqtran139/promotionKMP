//
//  PromotionUIStrings.swift
//  PromotionKit
//
//  Gom chuỗi hiển thị của tầng UI vào MỘT nơi — parity với `strings.xml` bên Android
//  (store/promotionLogic cố ý không giữ chuỗi; native lo hiển thị).
//
//  Từ nay thân mỗi hằng là `NSLocalizedString` tra trong bảng của SDK theo ngôn ngữ host chọn —
//  xem `PRMLocalization`. **Call-site không đổi một dòng nào**, đúng như thiết kế ban đầu đã tính.
//
//  Hai điều bắt buộc khi thêm chuỗi mới:
//
//  1. Dùng `static var` (computed), KHÔNG dùng `static let`. `static let` chỉ tính một lần rồi đóng
//     băng — host đổi `language` giữa hai lần `initialize` thì chữ đứng nguyên ở ngôn ngữ đầu tiên,
//     và lỗi đó không hiện ra ở lần chạy nào của dev.
//  2. Thêm cả `vi.lproj` **và** `en.lproj` (`Resources/*.lproj/PromotionKit.strings`). Thiếu bảng
//     nào thì bảng đó rơi về `defaultValue` — vẫn hiện tiếng Việt, không hiện key trần.
//

import Foundation

enum PromotionUIStrings {

    /// Tra một khoá trong bảng chuỗi của SDK.
    ///
    /// `value:` là chuỗi lùi khi khoá không có trong bảng — **luôn** truyền bản tiếng Việt. Không có
    /// nó thì `NSLocalizedString` trả về chính cái khoá (`prm_use_now`), tức là user nhìn thấy tên
    /// biến trên màn hình.
    private static func tr(_ key: String, _ defaultValue: String) -> String {
        NSLocalizedString(
            key,
            tableName: "PromotionKit",
            bundle: PRMLocalization.bundle,
            value: defaultValue,
            comment: ""
        )
    }

    static var useNow: String { tr("prm_use_now", "Sử dụng ngay") }
    static var use: String { tr("prm_use", "Sử dụng") }
    /// Nút màn Chi tiết khi vào từ luồng thanh toán (TLNV MOB_002 control #5) — `prm_apply` bên Android.
    static var apply: String { tr("prm_apply", "Áp dụng") }
    static var used: String { tr("prm_status_used", "Đã sử dụng") }
    static var expired: String { tr("prm_status_expired", "Đã hết hạn") }
    static var ineligible: String { tr("prm_status_ineligible", "Không đủ điều kiện") }
    static var detail: String { tr("prm_detail", "Chi tiết") }
    static var myPromotions: String { tr("prm_my_offer", "Ưu đãi của tôi") }
    static var otherPromotions: String { tr("prm_offer_different", "Ưu đãi khác") }
    /// Hai tab màn chi tiết — đối ứng `prm_tab_detail_info` / `prm_tab_usage_guide` bên Android.
    static var tabDetailInfo: String { tr("prm_tab_detail_info", "Thông tin chi tiết") }
    static var tabUsageGuide: String { tr("prm_tab_usage_guide", "Hướng dẫn sử dụng") }
    /// Empty-view màn "Ưu đãi của tôi" — đối ứng `prm_deal_hot` / `prm_hot_deal_description` bên Android.
    static var emptyPromotionsTitle: String { tr("prm_deal_hot", "Ngàn deal HOT chờ bạn") }
    static var emptyPromotionsDescription: String {
        tr("prm_hot_deal_description", "Lấp đầy kho quà với thật nhiều ưu đãi hấp dẫn bạn nhé!")
    }

    /// "HSD: 20/05/2026"
    static func expiryDate(_ value: String) -> String {
        String(format: tr("prm_expiry_short_format", "HSD: %1$@"), value)
    }
    /// API không trả HSD (nil/rỗng) = voucher không có hạn dùng — đối ứng `prm_expiry_never`.
    static var expiryNever: String { tr("prm_expiry_never", "HSD: Không hết hạn") }
    /// "HSD còn 3 ngày" — số ngày do store (promotionLogic) quyết định.
    static func remainingDays(_ days: Int) -> String {
        String(format: tr("prm_expiry_remaining_days", "HSD còn %1$d ngày"), days)
    }
    /// "Giảm 100.000đ" — [value] là số ĐÃ format theo locale, xem `PromotionSDKImpl.formatDiscount`.
    static func discount(_ value: String) -> String {
        String(format: tr("prm_discount_amount_format", "Giảm %1$@đ"), value)
    }
    /// "Hạn sử dụng 20/05/2026"
    static func expiryDateLong(_ value: String) -> String {
        String(format: tr("prm_expiry_long_format", "Hạn sử dụng %1$@"), value)
    }
    /// "Đã chọn 2 voucher" — đối ứng `prm_selected_voucher_count` bên Android.
    static func selectedVoucherCount(_ count: Int) -> String {
        String(format: tr("prm_selected_voucher_count", "Đã chọn %1$d voucher"), count)
    }

    // MARK: - Widget ưu đãi (`PRMOfferWidget`)

    static var offer: String { tr("prm_offer", "Ưu đãi") }
    static var cancel: String { tr("prm_cancel_voucher", "Huỷ") }
    static var changeVoucher: String { tr("prm_change_voucher", "Chọn lại") }
    static var noOffer: String { tr("prm_no_offer", "Bạn không có mã ưu đãi nào") }
    /// "Bạn có 1 mã ưu đãi" / "Bạn có 3 mã ưu đãi" — Android tách hai khoá (`prm_one_offer` /
    /// `prm_multiple_offer`) vì tiếng Việt không chia số nhiều; giữ đúng hai khoá đó để câu chữ
    /// hai nền tảng trùng khít.
    static func offerCount(_ count: Int) -> String {
        count == 1
            ? tr("prm_one_offer", "Bạn có 1 mã ưu đãi")
            : String(format: tr("prm_multiple_offer", "Bạn có %1$d mã ưu đãi"), count)
    }

    // MARK: - Danh sách

    static var seeMore: String { tr("prm_see_more", "Xem thêm") }
    static var collapse: String { tr("prm_collapse", "Thu gọn") }
    /// "Còn 3 ngày" — nhãn cảnh báo sắp hết hạn trên thẻ, **khác** `remainingDays` (có tiền tố "HSD").
    static func remainingDaysShort(_ days: Int) -> String {
        String(format: tr("prm_remaining_days_short", "Còn %1$d ngày"), days)
    }

    // MARK: - Lỗi hiển thị cho host (`PromotionSDKError.errorDescription`)

    static var featureDisabled: String {
        tr("prm_feature_disabled",
           "Tính năng ưu đãi hiện đang tạm thời không khả dụng. Vui lòng thử lại sau.")
    }
    static var sessionExpired: String {
        tr("prm_error_session_expired", "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.")
    }
    static var timeout: String { tr("prm_error_timeout", "Yêu cầu bị timeout, vui lòng thử lại.") }
    static var parseFailed: String {
        tr("prm_error_parse_failed", "Có lỗi xảy ra với dữ liệu trả về.")
    }
    static var notInitialized: String {
        tr("prm_error_not_initialized", "PromotionSDK chưa được khởi tạo.")
    }
    static var networkError: String {
        tr("prm_error_network", "Không có kết nối mạng. Vui lòng kiểm tra rồi thử lại")
    }
    static var generalError: String { tr("prm_error_general", "Đã có lỗi xảy ra. Vui lòng thử lại sau") }

    /// Nhãn VoiceOver cho các control **chỉ có icon** — nút không có chữ thì VoiceOver đọc tên file
    /// ảnh, hoặc không đọc gì. Đối ứng `android:contentDescription` bên Android, giữ trùng câu chữ.
    ///
    /// Chỉ khai ở đây những control mà nhãn KHÔNG suy ra được từ chữ đang hiển thị: nút có title
    /// (`Áp dụng`, `Sử dụng ngay`…) thì UIKit đã lấy title làm nhãn, thêm nữa là đọc hai lần.
    enum A11y {
        static var backButton: String { tr("prm_a11y_back", "Quay lại") }
        static var searchButton: String { tr("prm_a11y_search", "Tìm ưu đãi") }
        /// Ảnh minh hoạ của voucher — không mang thông tin, VoiceOver nên bỏ qua để không đọc thừa
        /// giữa tên và hạn dùng. Dùng làm nhãn khi ảnh **có** ý nghĩa (banner màn chi tiết).
        static var promotionBanner: String { tr("prm_a11y_promotion_banner", "Ảnh ưu đãi") }
    }

    /// Map mã lỗi (raw từ store) → chuỗi hiển thị — **dùng chung mọi màn**, KHỚP wording Android
    /// (`R.string.prm_*`). Mã là chữ **thường**, khớp `ErrorCodes` của lõi.
    static func errorMessage(_ code: String) -> String {
        switch code {
        case "missing_customer_id":
            return tr("prm_missing_customer_id", "Không tìm thấy thông tin khách hàng")
        case "no_result", "error_detail_unavailable":
            return tr("prm_no_result", "Không tìm thấy kết quả phù hợp")
        // Đối ứng `prm_error_network` / `prm_error_timeout` bên Android — giữ trùng câu chữ.
        case "network_error":
            return tr("prm_error_network", "Không có kết nối mạng. Vui lòng kiểm tra rồi thử lại")
        case "timeout":
            return tr("prm_error_timeout", "Kết nối quá lâu không phản hồi. Vui lòng thử lại")
        default:
            return tr("prm_error_general", "Đã có lỗi xảy ra. Vui lòng thử lại sau")
        }
    }
}
