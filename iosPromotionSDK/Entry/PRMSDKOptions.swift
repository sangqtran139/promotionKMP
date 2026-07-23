//
//  PRMSDKOptions.swift
//  PRMSDK
//
//  Tham số khởi tạo SDK — mirror `PRMSDKOptions` bên Android.
//

import Foundation

/// Tuỳ chọn khởi tạo `PRMSDK.initialize`. Đối ứng `PRMSDKOptions` bên Android.
///
/// - `theme`: `nil` (mặc định) = SDK **tự khôi phục** theme đã lưu lần trước; host cấu hình một
///   lần rồi thôi. Truyền theme cụ thể = ghi đè và lưu lại. Xem `PRMSDK.initialize`.
/// - `callback`: nơi nhận sự kiện SDK (áp/huỷ voucher, chọn dịch vụ, đổi tình trạng khả dụng…).
///   SDK giữ tới khi `release` (đối ứng `val callback` bên Android).
public struct PRMSDKOptions {
    public let session: PRMSessionConfig
    public let availableServices: [PRMAvailableService]
    public let theme: PRMSDKTheme?
    public let callback: PRMSDKCallback?

    public init(
        session: PRMSessionConfig,
        availableServices: [PRMAvailableService] = [],
        theme: PRMSDKTheme? = nil,
        callback: PRMSDKCallback? = nil
    ) {
        self.session = session
        self.availableServices = availableServices
        self.theme = theme
        self.callback = callback
    }
}
