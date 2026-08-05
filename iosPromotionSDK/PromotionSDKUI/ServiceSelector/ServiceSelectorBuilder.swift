//
//  ServiceSelectorBuilder.swift
//  PromotionSDK
//
//  Lọc danh mục dịch vụ host cung cấp (`PromotionContainer.requireConfig().availableServices`)
//  theo `applicableProducts` của voucher → items cho bottom sheet "Chọn dịch vụ".
//  Đọc thẳng từ core config như Android `MyPromotionViewModel.openServiceSelector` (`config.availableServices`).
//

import Foundation
@_implementationOnly import PRMKotlinBridge

enum ServiceSelectorBuilder {

    /// Trả về danh sách dịch vụ khả dụng cho voucher: giao giữa `applicableProducts.productId`
    /// và `availableServices.serviceCode`, loại trùng theo serviceCode (giữ thứ tự host cung cấp).
    /// Rule lọc **dùng chung** với Android ở `promotionLogic` (`servicesForApplicableProducts`);
    /// iOS chỉ đọc config + map sang model bottom sheet.
    static func items(forApplicableProducts products: [ApplicableProduct]) -> [ServiceSelectorItem] {
        let services = PromotionContainer.shared.requireConfig().availableServices
        return ServiceSelectorKt.servicesForApplicableProducts(applicableProducts: products, availableServices: services)
            .map { ServiceSelectorItem(serviceCode: $0.serviceCode, serviceName: $0.serviceName, serviceType: $0.serviceType, iconUrl: $0.iconUrl) }
    }
}
