//
//  HostRequestContextProvider.swift
//  VDSPromotionUI
//
//  Cấp token / customerId cho lõi Kotlin. Trước đây iOS truyền `token` vào **từng** request
//  (`MyPromotionInput.token`, `EligibleCampaignsInput.token`...). Lõi KMP thay bằng một nguồn
//  duy nhất, áp cho mọi request — giống Android.
//

import Foundation
@_implementationOnly import PromotionKit

final class HostRequestContextProvider: NSObject, PromotionRequestContextProvider {

    private let customerId: String
    private let token: String?

    init(customerId: String, token: String?) {
        self.customerId = customerId
        self.token = token
    }

    func getCustomerId() -> String? { customerId }

    /// Lõi tự thêm tiền tố `Bearer ` nếu chuỗi chưa có.
    func getAccessToken() -> String? { token }

    func getLanguage() -> String? { "vi-VN" }

    func getService() -> String? { nil }
    func getOrderId() -> String? { nil }
    func getOrderValue() -> String? { nil }
}
