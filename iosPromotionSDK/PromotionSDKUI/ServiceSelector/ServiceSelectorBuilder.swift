//
//  ServiceSelectorBuilder.swift
//  PromotionSDK
//
//  Lọc danh mục dịch vụ host cung cấp (`PromotionSDKConfig.availableServices`) theo
//  `applicableProducts` của voucher → items cho bottom sheet "Chọn dịch vụ".
//  Cả lấy config lẫn luật lọc đều nằm ở `promotionLogic`, chung một điểm gọi với Android.
//

import Foundation
@_implementationOnly import PRMKotlinBridge

enum ServiceSelectorBuilder {

    /// Trả về danh sách dịch vụ khả dụng cho voucher: giao giữa `applicableProducts.productId`
    /// và `availableServices.productId`, loại trùng theo productId (giữ thứ tự host cung cấp).
    /// Đọc config + lọc đều **dùng chung** với Android ở `promotionLogic` (`configuredServicesFor`);
    /// iOS chỉ map sang model bottom sheet.
    static func items(forApplicableProducts products: [ApplicableProduct]) -> [ServiceSelectorItem] {
        ServiceSelectorKt.configuredServicesFor(applicableProducts: products)
            .map { ServiceSelectorItem(productId: $0.productId, productName: $0.productName, skuSourceId: $0.skuSourceId, iconUrl: $0.iconUrl) }
    }
}
