//
//  BaseRouter+FeatureFlag.swift
//  PRMPromotionUI
//
//  Gate điều hướng theo feature flag. Dùng chung cho các router mở màn Chi tiết ưu đãi.
//

import UIKit
@_implementationOnly import PRMDesignKit
@_implementationOnly import PRMKotlinBridge

extension BaseRouter {

    /// Kiểm tra cờ `VOUCHER_DETAIL` trước khi mở màn Chi tiết ưu đãi.
    /// - Returns: `true` nếu được phép mở. Nếu TẮT → hiện popup lỗi PRM_MOB_021 và trả `false`.
    ///
    /// Quyết định "có được mở không" nằm ở `PromotionFeatureGate` của lõi Kotlin — **dùng chung với
    /// Android**. Ở đây chỉ còn phần riêng của iOS: hiển thị popup. Đối ứng bên Android là
    /// `PRMBaseFragment.openPromotionDetail(voucherId)`.
    ///
    /// Fail-open, `isEnabled` không gọi mạng, và ghi chú `TODO(feature-flag)` về schema API:
    /// xem `PromotionFeatureGate` trong `promotionLogic`.
    func canOpenVoucherDetail() -> Bool {
        if PromotionFeatureGate.shared.canOpenVoucherDetail() {
            return true
        }
        if let container = viewController?.view ?? navigator?.topViewController?.view {
            let message = PromotionSDKError.featureDisabled.errorDescription
                ?? "Tính năng hiện đang tạm thời không khả dụng. Vui lòng thử lại sau."
            PRMConfirmationDialog.showError(message, in: container)
        }
        return false
    }
}
