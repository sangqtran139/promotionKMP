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

/// Callback sự kiện SDK — set qua `PromotionSDKOptions.callback`.
///
/// **Hợp đồng — bắt buộc đọc.** Giống `PromotionTokenSource` và `PromotionTracker`, đây là một object
/// của host mà SDK giữ; ba mục dưới đây là cùng một hợp đồng, phát biểu cho cả ba cổng. Đối ứng từng
/// mục với `PromotionSDKCallback` bên Android.
///
/// 1. **Thread: luôn là main thread.** Cả ba sự kiện đều phát ra từ tầng UI của SDK
///    (`PromotionSDKImpl`, `PRMStoreViewModel`, các ViewController) — đều `@MainActor` — nên host
///    **được phép** đụng UIKit / điều hướng thẳng trong thân method, không cần
///    `DispatchQueue.main.async`. Đây là **bảo đảm**, không phải tình cờ: đổi chỗ bắn sang thread nền
///    là breaking change và phải ghi vào `CHANGELOG.md`.
/// 2. **Vòng đời — bẫy dễ dính nhất.** `PromotionSDK` là singleton; nó giữ **strong reference** tới
///    object này từ `initialize` đến `release()`. Cho một `UIViewController` conform protocol này rồi
///    truyền `self` là giữ màn hình đó vĩnh viễn, và sau khi màn hình biến mất thì callback vẫn chạy
///    trên một view đã rời hierarchy. Trỏ vào một singleton **cấp app** rồi từ đó phát tiếp đi đâu
///    cũng được — xem `DemoPromotionCallback` ở app demo.
///
///    SDK **cố ý giữ strong, không giữ weak** dù protocol là `AnyObject`: weak thì host truyền một
///    object cục bộ là callback lặng lẽ chết ngay khi nó rời scope, không lỗi không log — hỏng theo
///    kiểu khó lần hơn hẳn rò rỉ. Đối ứng `private var callback` (strong) bên Android.
/// 3. **Sau `release()` thì không còn sự kiện nào.** `release()` xoá tham chiếu này, nên host không
///    phải tự huỷ đăng ký. Đổi callback = gọi lại `initialize`.
///
/// Ném/`fatalError` trong các method này thì **SDK không cứu** — nó nổi lên đúng chỗ bắn, tức trong
/// luồng UI của màn SDK. Khác `PromotionTracker` (tracking là phụ trợ nên lõi nuốt + log): những sự
/// kiện ở đây là nghiệp vụ của host, nuốt đi thì host tưởng đã xử lý xong.
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

    /// Gọi khi 1 API bên trong màn SDK (Ưu đãi của tôi / Tìm kiếm / Chi tiết / Chọn ưu đãi /
    /// widget ưu đãi) trả về HTTP 401 và **không cứu được** — tức `PromotionTokenSource.refreshToken(_:)` đã
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
