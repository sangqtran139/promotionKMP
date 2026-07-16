//
//  PromotionSDKOptions.swift
//  PromotionSDK
//
//  Tham số khởi tạo SDK — mirror `PromotionSDKOptions` bên Android.
//

import Foundation

/// Tuỳ chọn khởi tạo `PromotionSDK.initialize`. Đối ứng `PromotionSDKOptions` bên Android.
///
/// - `theme`: `nil` (mặc định) = SDK **tự khôi phục** theme đã lưu lần trước; host cấu hình một
///   lần rồi thôi. Truyền theme cụ thể = ghi đè và lưu lại. Xem `PromotionSDK.initialize`.
/// - `callback`: nơi nhận sự kiện SDK (áp/huỷ voucher, chọn dịch vụ, đổi tình trạng khả dụng…).
///   SDK giữ tới khi `release` (đối ứng `val callback` bên Android).
public struct PromotionSDKOptions {
    public let session: PromotionSessionConfig
    public let availableServices: [PromotionAvailableService]
    public let theme: PromotionSDKTheme?
    public let callback: PromotionSDKCallback?

    public init(
        session: PromotionSessionConfig,
        availableServices: [PromotionAvailableService] = [],
        theme: PromotionSDKTheme? = nil,
        callback: PromotionSDKCallback? = nil
    ) {
        self.session = session
        self.availableServices = availableServices
        self.theme = theme
        self.callback = callback
    }
}
