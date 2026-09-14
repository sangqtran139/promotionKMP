//
//  PromotionHostServices.swift
//  PromotionSDK
//
//  Gói năng lực do host cấp — mirror `PromotionHostServices.kt` bên Android (cùng kiểu, cùng thứ tự):
//  `PromotionHostServices`, `PromotionEvent`, `PromotionTracker`, `PromotionStorage`,
//  và phần map public→core (nội bộ).
//
//  Type public ở đây chỉ dùng String/Foundation nên KHÔNG lộ PRMKotlinBridge ra module interface;
//  chỉ các lớp adapter (nội bộ) mới chạm lõi Kotlin.
//
//  Lưu ý về tên: lõi Kotlin cũng export `PromotionEvent`/`PromotionTracker`/`PromotionHostServices`
//  sang Swift. Tên trong module này che tên của module nhập vào, nên chỗ nào cần kiểu của lõi thì
//  phải viết đủ `PromotionLogic.…` — xem phần adapter ở cuối file.
//

import Foundation
@_implementationOnly import PRMKotlinBridge

/// **Gói năng lực do host cấp** — một chỗ duy nhất cho mọi thứ SDK không tự làm được vì nó nằm ở
/// tầng native của app: tracking, kho dữ liệu, và những cổng sẽ thêm về sau.
/// Đối ứng `PromotionHostServices` bên Android (cùng field, cùng thứ tự).
///
/// Đây là *cái gói*, không phải một tính năng. Nếu mỗi năng lực là một tham số riêng của
/// `PromotionSDKOptions` và `PromotionSDK.initialize` thì thêm năng lực thứ ba là sửa sáu chữ ký
/// public trên hai nền tảng. Với gói này, thêm năng lực = thêm **một field**.
///
/// Mọi field đều **tuỳ chọn** — host bật đúng thứ mình cần:
///
/// ```swift
/// PromotionSDK.initialize(
///     tokenSource: AppTokenSource.shared, baseUrl: baseUrl,
///     hostServices: PromotionHostServices(tracker: AppPromotionTracker.shared)
/// )
/// ```
///
/// **Không thuộc gói này:** `PromotionTokenSource`. Nó cũng là cổng do host cấp, nhưng đứng ở
/// `PromotionSessionConfig` vì token là **thông tin phiên** — gắn với lần đăng nhập, đổi theo user.
/// Gói này là **năng lực hạ tầng**, gắn với vòng đời app.
///
/// Xem `docs/common/HostCapabilities.md`.
public struct PromotionHostServices {
    /// Nơi nhận event của SDK. `nil` → SDK không bắn đi đâu. Xem `PromotionTracker`.
    public let tracker: PromotionTracker?
    /// Kho khoá–giá trị của app thay cho kho mặc định của SDK. `nil` → SDK tự lo. Xem `PromotionStorage`.
    public let storage: PromotionStorage?

    public init(tracker: PromotionTracker? = nil, storage: PromotionStorage? = nil) {
        self.tracker = tracker
        self.storage = storage
    }
}

// MARK: - Tracking

/// Một sự kiện SDK bắn ra cho hệ thống tracking của host (Firebase, AppsFlyer, hệ log nội bộ…).
/// Đối ứng `PromotionEvent` bên Android.
///
/// `name` do SDK đặt và **giống hệt bên Android** — tên event sinh ở tầng logic dùng chung
/// (`promotionLogic`), không phải ở ViewController, nên phễu của hai nền tảng gộp được.
/// Danh sách event xem `docs/common/HostCapabilities.md`.
///
/// `params` chỉ chứa `String`. Số/bool đã được SDK đổi sang chuỗi sẵn để hai nền tảng gửi đi giống
/// nhau; host tự parse nếu hệ tracking của mình cần kiểu khác.
///
/// SDK **không bao giờ** đặt PII vào đây (token, số điện thoại, email, từ khoá người dùng gõ).
public struct PromotionEvent {
    public let name: String
    public let params: [String: String]

    public init(name: String, params: [String: String] = [:]) {
        self.name = name
        self.params = params
    }
}

/// Nơi host nhận event của SDK. Đối ứng `PromotionTracker` bên Android (cùng một hàm, cùng thứ tự).
///
/// ```swift
/// final class AppPromotionTracker: PromotionTracker {
///     static let shared = AppPromotionTracker()
///     func track(event: PromotionEvent) {
///         Analytics.logEvent(event.name, parameters: event.params)
///     }
/// }
/// ```
///
/// **Hợp đồng — bắt buộc đọc:**
/// - Lõi Kotlin gọi từ **thread nền**, ngay trong lúc màn hình đang cập nhật state. Vì vậy nó
///   **không được** `@MainActor`, và phải **không chặn**: đẩy sang queue của hệ tracking rồi trả về
///   ngay, đừng ghi đĩa đồng bộ hay gọi mạng tại chỗ.
/// - Ném/`fatalError` thì SDK không cứu được — Swift không có checked exception để lõi bọc lại. Giữ
///   thân hàm đơn giản.
/// - **Vòng đời:** `PromotionSDK` giữ object này tới tận `release()`. Cho một `UIViewController`
///   conform rồi truyền `self` là giữ màn hình đó vĩnh viễn. Trỏ vào một singleton **cấp app**,
///   giống `PromotionTokenSource`.
public protocol PromotionTracker: AnyObject {
    func track(event: PromotionEvent)
}

// MARK: - Kho dữ liệu

/// Kho khoá–giá trị của host, dùng **thay** kho mặc định của SDK (`NSUserDefaults`).
///
/// Dành cho app đã có sẵn một kho — điển hình là **DB của bản SDK native cũ** — và muốn SDK đọc ghi
/// vào đúng chỗ đó thay vì mở thêm một kho thứ hai. Không cấp → SDK tự dùng `NSUserDefaults` của
/// riêng nó (`promotion_sdk_prefs`), host không phải làm gì.
///
/// Cấp rồi thì **toàn bộ** SDK dùng nó: cache cờ tính năng và theme đã lưu đều đi qua đây.
/// Đối ứng `PromotionStorage` bên Android.
///
/// **Hợp đồng — bắt buộc đọc:**
/// - **Đồng bộ**: `getString` phải trả giá trị ngay, `putString` phải thấy được ở lượt đọc kế tiếp.
///   Có thể bị gọi từ **thread nền** → hiện thực phải thread-safe và **nhanh** (SDK gọi nó trên
///   đường dựng màn). Bọc một truy vấn CoreData/SQLite đồng bộ ở đây là tự cắm một lần chặn vào mỗi
///   lượt mở màn.
/// - `clear()` xoá **kho** — nếu kho dùng chung với dữ liệu khác của app thì hãy thu hẹp phạm vi xoá
///   về đúng phần của SDK, đừng xoá cả bảng.
/// - SDK **không** ghi token hay dữ liệu nhạy cảm vào đây (`docs/common/StorageGuide.md` §5.3), nên
///   kho không cần mã hoá.
/// - **Vòng đời:** giống `PromotionTracker` — singleton cấp app.
public protocol PromotionStorage: AnyObject {
    func putBoolean(key: String, value: Bool)
    func getBoolean(key: String, default defaultValue: Bool) -> Bool
    func putString(key: String, value: String)
    func getString(key: String) -> String?
    func contains(key: String) -> Bool
    func remove(key: String)
    func clear()
}

// ─── Public → core ──────────────────────────────────────────────────────────

extension PromotionHostServices {
    /// Public (`PromotionKit`) → core (Kotlin). Đối ứng `PromotionHostServices.toCore()` bên Android.
    ///
    /// Các lớp adapter dưới đây tồn tại để kiểu của `promotionLogic` **không** lọt vào chữ ký public
    /// của SDK iOS (`docs/common/PublicApi.md`) — cùng vai trò mà `PromotionMutableContext` đang làm
    /// cho `PromotionTokenSource`.
    func toCore() -> PromotionLogic.PromotionHostServices {
        PromotionLogic.PromotionHostServices(
            tracker: tracker.map { PromotionTrackerAdapter($0) },
            storage: storage.map { PromotionStorageAdapter($0) }
        )
    }
}

final class PromotionTrackerAdapter: NSObject, PromotionLogic.PromotionTracker {

    private let delegate: PromotionTracker

    init(_ delegate: PromotionTracker) {
        self.delegate = delegate
        super.init()
    }

    func track(event: PromotionLogic.PromotionEvent) {
        delegate.track(event: PromotionEvent(name: event.name, params: event.params))
    }
}

final class PromotionStorageAdapter: NSObject, PromotionPreferences {

    private let delegate: PromotionStorage

    init(_ delegate: PromotionStorage) {
        self.delegate = delegate
        super.init()
    }

    func putBoolean(key: String, value: Bool) { delegate.putBoolean(key: key, value: value) }
    func getBoolean(key: String, default defaultValue: Bool) -> Bool {
        delegate.getBoolean(key: key, default: defaultValue)
    }
    func putString(key: String, value: String) { delegate.putString(key: key, value: value) }
    func getString(key: String) -> String? { delegate.getString(key: key) }
    func contains(key: String) -> Bool { delegate.contains(key: key) }
    func remove(key: String) { delegate.remove(key: key) }
    func clear() { delegate.clear() }
}
