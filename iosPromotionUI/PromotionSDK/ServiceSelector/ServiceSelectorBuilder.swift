//
//  ServiceSelectorBuilder.swift
//  PromotionSDK
//
//  Lọc danh mục dịch vụ host cung cấp (`PromotionSessionConfig.availableServices`)
//  theo `applicableProducts` của voucher → items cho bottom sheet "Chọn dịch vụ".
//  Parity Android `MyPromotionViewModel.openServiceSelector`.
//

import Foundation
@_implementationOnly import PromotionKit

enum ServiceSelectorBuilder {

    /// Trả về danh sách dịch vụ khả dụng cho voucher: giao giữa `applicableProducts.productId`
    /// và `availableServices.serviceCode`, loại trùng theo serviceCode (giữ thứ tự host cung cấp).
    static func items(forApplicableProducts products: [ApplicableProduct]) -> [ServiceSelectorItem] {
        let applicableIds = Set(products.map { $0.productId })
        var seen = Set<String>()
        return PromotionSessionConfig.availableServices
            .filter { applicableIds.contains($0.serviceCode) }
            .filter { seen.insert($0.serviceCode).inserted }
            .map { ServiceSelectorItem(serviceCode: $0.serviceCode, serviceName: $0.serviceName, iconUrl: $0.iconUrl) }
    }
}
