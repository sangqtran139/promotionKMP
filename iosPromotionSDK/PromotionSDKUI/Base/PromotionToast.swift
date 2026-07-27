//
//  PromotionToast.swift
//  PromotionSDK
//
//  Cổng hiển thị toast lỗi của SDK — gom một chỗ để dễ bật/tắt toàn bộ.
//

import UIKit
@_implementationOnly import PRMDesignKit

/// Cổng bật/tắt **toàn bộ** toast lỗi của SDK.
///
/// Mặc định **TẮT** (`isEnabled = false`): SDK vẫn **bắt lỗi như cũ** (VC vẫn nhận `.showError`,
/// ViewModel vẫn `ConsumeError`), chỉ **không hiển thị** toast. Đổi thành `true` để bật lại.
///
/// Đối ứng `PromotionToastGate.isEnabled` bên Android — đổi một bên thì đổi bên kia.
enum PromotionToast {
    static var isEnabled = false

    static func show(_ message: String, in view: UIView) {
        guard isEnabled else { return }
        PRMToast.show(message, in: view)
    }
}
