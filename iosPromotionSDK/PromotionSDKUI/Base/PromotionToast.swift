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

    /// Thông báo **LUÔN hiện**, không đi qua `isEnabled` — dành cho "tính năng bị cờ chặn" (PRM_MOB_021).
    ///
    /// Ngoại lệ có chủ đích: các toast lỗi khác còn state thay thế (empty/shimmer/list cũ) nên tắt đi
    /// vẫn hiểu được; còn ở đây user bấm mà màn không mở, im lặng thì thành "app đơ".
    ///
    /// Hiện bằng `PRMConfirmationDialog` (1 nút "Đóng") thay vì toast — Toast dễ bị trôi qua quá
    /// nhanh, dialog đảm bảo user thấy được. Đối ứng `PromotionToastGate.showFeatureDisabled(context)`
    /// bên Android (cũng dùng `PRMBaseConfirmDialog`, không phải Toast dù tên class là ToastGate).
    static func showAlways(_ message: String, in view: UIView) {
        PRMConfirmationDialog.showError(message, in: view)
    }
}
