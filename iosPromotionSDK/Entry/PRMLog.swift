//
//  PRMLog.swift
//  PromotionSDK
//
//  Kênh log cho **thông điệp hướng tới dev của host** (vd "bạn gọi hàm này trước initialize").
//

import Foundation
import os

/// Thay cho `NSLog` ở tầng `Entry`.
///
/// `NSLog` có ba vấn đề với một SDK nhúng: nó ghi thẳng vào syslog của app host (không tắt được,
/// không lọc được theo subsystem), nó chậm vì đồng bộ, và nó không có mức độ — mọi dòng trông như
/// nhau nên dev của host không phân biệt được cảnh báo với thông tin.
///
/// **`os_log` chứ không phải `os.Logger`**: `Logger` chỉ có từ **iOS 14**, mà SDK khai
/// `IPHONEOS_DEPLOYMENT_TARGET = 13.0`. Dùng `Logger` thì phải bọc `if #available(iOS 14, *)` ở mọi
/// chỗ gọi, hoặc âm thầm nâng deployment target — cả hai đều tệ hơn. `os_log` có từ iOS 10.
///
/// Subsystem gắn bundle id của SDK nên dev của host lọc được đúng log của SDK trong Console.app:
/// `subsystem: com.vtm.PRM`.
enum PRMLog {

    private static let subsystem = Bundle(for: PromotionSDKImpl.self).bundleIdentifier ?? "com.vtm.PRM"

    /// Sai sót ở phía **tích hợp** — dev của host cần thấy và sửa. Không phải lỗi runtime của user.
    static let integration = OSLog(subsystem: subsystem, category: "integration")

    /// Dùng `%{public}@`: mặc định `os_log` **che** nội dung chuỗi thành `<private>` trong log của
    /// máy thật. Ở đây thông điệp là hướng dẫn tích hợp, không có dữ liệu người dùng — che đi thì
    /// dev của host chỉ thấy `<private>` và không biết mình gọi sai chỗ nào.
    static func integrationError(_ message: String) {
        os_log("%{public}@", log: integration, type: .error, message)
    }
}
