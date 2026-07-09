//
//  PromotionOrderItem.swift
//  PromotionSDK
//
//  Foundation-only public type: 1 dòng sản phẩm trong đơn hàng.
//  Host truyền vào để SDK gọi Find Eligible Campaigns (cần orderInfo.items[]) —
//  campaign theo SKU chỉ trả về khi có items; đơn không kèm items chỉ nhận campaign cấp đơn.
//

import Foundation

public struct PromotionOrderItem {
    /// Mã SKU sản phẩm (bắt buộc).
    public let skuId: String
    /// Mã product gốc (tuỳ chọn).
    public let productId: String?
    /// Tên sản phẩm — cho rule theo tên / hiển thị (tuỳ chọn).
    public let productName: String?
    /// Ngành hàng / danh mục — cho rule theo category (tuỳ chọn).
    public let productCategory: String?
    /// Số lượng (> 0).
    public let quantity: Int
    /// Đơn giá — chuỗi số nguyên (VNĐ), vd "500000".
    public let unitPrice: String

    public init(
        skuId: String,
        productId: String? = nil,
        productName: String? = nil,
        productCategory: String? = nil,
        quantity: Int,
        unitPrice: String
    ) {
        self.skuId = skuId
        self.productId = productId
        self.productName = productName
        self.productCategory = productCategory
        self.quantity = quantity
        self.unitPrice = unitPrice
    }
}
