//
//  BaseRouter+FeatureFlag.swift
//  VDSPromotionUI
//
//  Gate điều hướng theo feature flag. Dùng chung cho các router mở màn Chi tiết ưu đãi.
//

import UIKit
@_implementationOnly import CoreUI
@_implementationOnly import PromotionKit

extension BaseRouter {

    /// Kiểm tra cờ `VOUCHER_DETAIL` trước khi mở màn Chi tiết ưu đãi.
    /// - Returns: `true` nếu được phép mở. Nếu TẮT → hiện popup lỗi PRM_MOB_021 và trả `false`.
    ///
    /// Cờ đọc từ cache đồng bộ của lõi Kotlin (`isEnabled` không gọi mạng), y như bản iOS cũ đọc
    /// từ `UserDefaults`.
    ///
    /// **Fail-open** được giữ nguyên: chưa có cache → mọi cờ bật. Đây cũng là lý do gate này an toàn
    /// dù schema API chưa chốt — xem `TODO(feature-flag)` bên dưới.
    ///
    /// TODO(feature-flag): lõi Kotlin (port từ Android) và bản iOS cũ parse **hai schema khác nhau**
    /// cho cùng endpoint `POST api/v1/vtm/feature-flag/list`:
    ///
    ///     iOS    → data.enableSdk + data.features[] { featureCode: "voucher_detail", allowed }
    ///     Kotlin → data[] { flagName: "PROMOTION.VOUCHER_DETAIL", enabled }
    ///
    /// Nếu server dùng schema iOS thì Kotlin parse hỏng, `refresh()` nuốt lỗi, cache giữ mặc định
    /// bật-hết → gate này cho qua, đúng hành vi cũ. Khi backend xác nhận schema, sửa
    /// `FeatureFlagItemResponse` trong `promotionLogic` — file này không phải đổi.
    func canRouteToDetail() -> Bool {
        let flags = PromotionFeatureFlagUseCases()
        if flags.isEnabled(featureName: PromotionFeatureFlag.shared.VOUCHER_DETAIL) {
            return true
        }
        if let container = viewController?.view ?? navigator?.topViewController?.view {
            let message = PromotionSDKError.featureDisabled.errorDescription
                ?? "Tính năng hiện đang tạm thời không khả dụng. Vui lòng thử lại sau."
            VDSConfirmationDialog.showError(message, in: container)
        }
        return false
    }
}
