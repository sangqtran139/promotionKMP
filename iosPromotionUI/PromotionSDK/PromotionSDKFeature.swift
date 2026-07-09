//
//  PromotionSDKFeature.swift
//  PromotionSDK
//
//  Danh mục tính năng ưu đãi có thể bật/tắt qua feature flag (Unleash).
//  Chỉ dùng Foundation type ở public interface.
//

import Foundation

/// Các tính năng ưu đãi bật/tắt độc lập (khớp feature_code KBNV PRM_KBNV_API001).
/// Cờ master `ENABLE_ALL` luôn gate ngầm: master TẮT → mọi feature đều TẮT.
public enum PromotionSDKFeature {
    /// Danh sách "Ưu đãi của tôi" (`openMyPromotion`).
    case voucherList
    /// Màn chi tiết ưu đãi.
    case voucherDetail
    /// Widget chọn ưu đãi (`createEndowView`).
    case voucherSelection
    /// Áp dụng ưu đãi (validate).
    case voucherApply
    /// Tạo phiên sử dụng ưu đãi (redemption).
    case voucherRedeem
}
