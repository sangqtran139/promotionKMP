//
//  PromotionFeatureModels.swift
//  PromotionSDK
//
//  DTO công khai của bề mặt **feature flag**. Đối ứng 1-1 với `PromotionFeatureModels.kt` bên Android:
//  cùng tên type, cùng tên case/field, cùng thứ tự khai báo. Sửa một bên thì sửa cả hai.
//
//  Chỉ dùng type của Foundation — không Kotlin, không type nội bộ. `PromotionFeatureFlag` (hằng chuỗi)
//  và `PromotionFeatureFlags` (data class) của lõi thuộc `PRMKotlinBridge`; type nào của module đó lọt
//  vào đây sẽ bị ghi vào `.swiftinterface` của framework và app host không build được:
//
//      error: Unable to find module dependency: 'PRMKotlinBridge'
//
//  Ánh xạ lõi ↔ public nằm ở `PromotionSDKImpl` (`flagName(for:)` / `featureFlagsSnapshot()`).
//

import Foundation

/// Tính năng có thể bị bật/tắt từ xa (kill-switch, không phải A/B test).
///
/// Dùng enum thay vì chuỗi để host không gõ sai tên cờ; tên cờ thật (`"PROMOTION.VOUCHER_LIST"`…)
/// là chi tiết nội bộ của lõi Kotlin, giữ ở `PromotionFeatureFlag`.
public enum PromotionFeature: CaseIterable {
    /// Công tắc tổng. TẮT → mọi tính năng dưới đây đều TẮT, bất kể giá trị riêng.
    case all

    /// Màn "Ưu đãi của tôi" — `PromotionSDK.openMyPromotion(from:)`.
    case voucherList

    /// Màn "Chi tiết ưu đãi" — `PromotionSDK.openPromotionDetail(voucherId:from:)`.
    case voucherDetail

    /// Widget chọn ưu đãi ở màn thanh toán + màn "Chọn ưu đãi".
    case voucherSelection

    /// Áp voucher vào đơn hàng — `PromotionSDKApi.validateDiscounts`.
    case voucherApply

    /// Tạo phiên thanh toán — `PromotionSDKApi.createRedemption`.
    case voucherRedeem
}

/// Ảnh chụp **toàn bộ** cờ tại một thời điểm, đọc từ cache (không gọi mạng).
///
/// Gọi là *snapshot* vì đúng như vậy: giá trị có thể đổi sau lần `PromotionSDK.refreshFeatureFlags`
/// kế tiếp. Đừng cache lại nó lâu dài — hỏi SDK mỗi khi cần dựng UI.
///
/// Mọi field đã **áp sẵn công tắc tổng**: `all` TẮT thì các field còn lại đều `false`, host không
/// phải tự nhân hai điều kiện.
public struct PromotionFeatureFlagsSnapshot {
    public let all: Bool
    public let voucherList: Bool
    public let voucherDetail: Bool
    public let voucherSelection: Bool
    public let voucherApply: Bool
    public let voucherRedeem: Bool

    public init(all: Bool, voucherList: Bool, voucherDetail: Bool,
                voucherSelection: Bool, voucherApply: Bool, voucherRedeem: Bool) {
        self.all = all
        self.voucherList = voucherList
        self.voucherDetail = voucherDetail
        self.voucherSelection = voucherSelection
        self.voucherApply = voucherApply
        self.voucherRedeem = voucherRedeem
    }

    /// Tra một `PromotionFeature` trên chính snapshot này (không hỏi lại SDK).
    public func isEnabled(_ feature: PromotionFeature) -> Bool {
        switch feature {
        case .all: return all
        case .voucherList: return voucherList
        case .voucherDetail: return voucherDetail
        case .voucherSelection: return voucherSelection
        case .voucherApply: return voucherApply
        case .voucherRedeem: return voucherRedeem
        }
    }

    /// Mặc định **fail-open**: chưa `initialize()` hoặc chưa có cache → coi như bật hết.
    /// SDK không tự khoá tính năng chỉ vì chưa gọi được API lần nào.
    public static let allEnabled = PromotionFeatureFlagsSnapshot(
        all: true,
        voucherList: true,
        voucherDetail: true,
        voucherSelection: true,
        voucherApply: true,
        voucherRedeem: true
    )
}
