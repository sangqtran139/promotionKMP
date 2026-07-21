//
//  PromotionApiModels.swift
//  PromotionSDK
//
//  DTO công khai của `PromotionSDKApi`. Đối ứng 1-1 với `PromotionApiModels.kt` bên Android:
//  cùng tên type, cùng tên field, cùng thứ tự khai báo. Sửa một bên thì sửa cả hai.
//
//  Chỉ dùng type của Foundation — không Kotlin, không RxSwift. Type nào của `PRMKotlinBridge` lọt vào
//  đây sẽ bị ghi vào `.swiftinterface` của framework và app host không build được:
//
//      error: Unable to find module dependency: 'PRMKotlinBridge'
//
//  Ngày tháng giữ nguyên **chuỗi thô của server**. Trước đây tầng này parse sang `Date` qua
//  `PRMPromotionDate`, nhưng parse hỏng thì trả `nil` — host không phân biệt được "voucher vô thời hạn"
//  với "server trả định dạng lạ". Việc định dạng ngày là của tầng hiển thị; `PRMPromotionDate` vẫn còn,
//  dùng cho UI nội bộ của SDK.
//

import Foundation

/// 1 voucher trong danh sách trả cho đối tác.
public struct PromotionVoucher {
    public let id: String
    public let merchantName: String
    /// Tên ưu đãi.
    public let title: String
    public let imageURL: String?
    public let expireDate: String?
    public let isUsed: Bool
    /// Mã trạng thái thô từ server (ACTIVE/USED/EXPIRED...).
    public let status: String?
    /// Nhãn trạng thái do server cung cấp (ưu tiên hiển thị).
    public let displayStatusLabel: String?

    public init(id: String, merchantName: String, title: String, imageURL: String?,
                expireDate: String?, isUsed: Bool, status: String?, displayStatusLabel: String?) {
        self.id = id
        self.merchantName = merchantName
        self.title = title
        self.imageURL = imageURL
        self.expireDate = expireDate
        self.isUsed = isUsed
        self.status = status
        self.displayStatusLabel = displayStatusLabel
    }
}

/// Kết quả lấy voucher **của khách** (Search Customer Vouchers).
///
/// API chỉ trả voucher đã sở hữu. Để lấy "Ưu đãi khác" (campaign chưa sở hữu, đủ điều kiện cho đơn)
/// dùng `PromotionSDKApi.findEligible`.
public struct PromotionVoucherPage {
    public let vouchers: [PromotionVoucher]
    public let isLastPage: Bool
    /// Ngưỡng cảnh báo sắp hết hạn (đơn vị ngày) — cấu hình tĩnh BFF; nil nếu không trả.
    public let expireWarningDate: Int?

    public init(vouchers: [PromotionVoucher], isLastPage: Bool, expireWarningDate: Int? = nil) {
        self.vouchers = vouchers
        self.isLastPage = isLastPage
        self.expireWarningDate = expireWarningDate
    }
}

/// Chi tiết 1 voucher (Get Customer Voucher Detail).
public struct PromotionVoucherDetail {
    public let id: String
    public let merchantName: String
    public let title: String
    public let description: String
    /// Hướng dẫn sử dụng (điều khoản / cách dùng).
    public let guideline: String
    public let startDate: String?
    public let expireDate: String?
    public let bannerURL: String?
    public let logoURL: String?
    public let status: String
    public let displayStatusLabel: String?
    /// Danh sách mã (codex) đã cấp cho khách.
    public let codes: [String]
    /// Link hướng dẫn sử dụng.
    public let usageGuideUrl: String?

    public init(id: String, merchantName: String, title: String, description: String,
                guideline: String, startDate: String?, expireDate: String?, bannerURL: String?,
                logoURL: String?, status: String, displayStatusLabel: String?,
                codes: [String] = [], usageGuideUrl: String? = nil) {
        self.id = id
        self.merchantName = merchantName
        self.title = title
        self.description = description
        self.guideline = guideline
        self.startDate = startDate
        self.expireDate = expireDate
        self.bannerURL = bannerURL
        self.logoURL = logoURL
        self.status = status
        self.displayStatusLabel = displayStatusLabel
        self.codes = codes
        self.usageGuideUrl = usageGuideUrl
    }
}

/// 1 ưu đãi đủ điều kiện (Find Eligible Campaigns) cho luồng checkout.
public struct PromotionEligibleOffer {
    /// Định danh để chọn/validate: `voucherId` (nhóm "của tôi") hoặc `campaignId` (nhóm "khác").
    public let id: String
    /// Tên ưu đãi hiển thị.
    public let name: String
    /// Loại campaign (DISCOUNT/COUPON...), dùng làm `objectType` khi validate.
    public let objectType: String
    /// true = dùng được ngay; false = chưa đủ điều kiện (hiển thị mờ).
    public let usable: Bool
    /// Số tiền giảm dự kiến (chuỗi số thô), nil nếu không có preview.
    public let estimatedDiscount: String?
    public let expireDate: String?
    /// Gợi ý lý do chưa đủ điều kiện (khi `usable == false`). SDK trả rule thô, không dựng sẵn câu.
    public let ineligibleReason: String?
    /// Logo voucher/ưu đãi (có ở cả 2 nhóm).
    public let logoUrl: String?
    /// Tên đối tác/merchant phát hành (có ở cả 2 nhóm).
    public let partnerName: String?
    /// Mã code đã phát hành — chỉ nhóm "của tôi".
    public let voucherCode: String?

    public init(id: String, name: String, objectType: String, usable: Bool,
                estimatedDiscount: String?, expireDate: String?, ineligibleReason: String?,
                logoUrl: String? = nil, partnerName: String? = nil, voucherCode: String? = nil) {
        self.id = id
        self.name = name
        self.objectType = objectType
        self.usable = usable
        self.estimatedDiscount = estimatedDiscount
        self.expireDate = expireDate
        self.ineligibleReason = ineligibleReason
        self.logoUrl = logoUrl
        self.partnerName = partnerName
        self.voucherCode = voucherCode
    }
}

/// Kết quả Find Eligible Campaigns: 2 nhóm "của tôi" / "khác", phân trang ĐỘC LẬP.
public struct PromotionEligibleResult {
    public let myOffers: [PromotionEligibleOffer]
    public let otherOffers: [PromotionEligibleOffer]
    public let myIsLastPage: Bool
    public let otherIsLastPage: Bool
    /// Ngưỡng cảnh báo sắp hết hạn (đơn vị ngày) — cấu hình tĩnh BFF; nil nếu không trả.
    public let expireWarningDate: Int?

    public init(myOffers: [PromotionEligibleOffer], otherOffers: [PromotionEligibleOffer],
                myIsLastPage: Bool, otherIsLastPage: Bool, expireWarningDate: Int? = nil) {
        self.myOffers = myOffers
        self.otherOffers = otherOffers
        self.myIsLastPage = myIsLastPage
        self.otherIsLastPage = otherIsLastPage
        self.expireWarningDate = expireWarningDate
    }
}

/// 1 dòng sản phẩm trong đơn. Host truyền vào `PromotionSDKApi.findEligible` để lấy campaign theo SKU
/// — đơn không kèm items chỉ nhận campaign cấp đơn.
public struct PromotionOrderItem {
    /// Mã SKU sản phẩm (bắt buộc).
    public let skuId: String
    public let productId: String?
    /// Tên sản phẩm — cho rule theo tên / hiển thị.
    public let productName: String?
    /// Ngành hàng / danh mục — cho rule theo category.
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

/// Kết quả validate một tập voucher với đơn hàng, trước khi áp.
public struct PromotionValidationResult {
    public let overallValid: Bool
    public let totalDiscountAmount: String
    public let finalAmount: String
    public let items: [PromotionDiscountItem]

    public init(overallValid: Bool, totalDiscountAmount: String, finalAmount: String, items: [PromotionDiscountItem]) {
        self.overallValid = overallValid
        self.totalDiscountAmount = totalDiscountAmount
        self.finalAmount = finalAmount
        self.items = items
    }
}

public struct PromotionDiscountItem {
    public let objectId: String
    public let discountAmount: String
    public let isValid: Bool
    /// Lý do voucher không hợp lệ, vd "EXPIRED", "BUDGET_EXCEEDED", "NOT_ELIGIBLE".
    public let eligibilityStatus: String

    public init(objectId: String, discountAmount: String, isValid: Bool, eligibilityStatus: String = "") {
        self.objectId = objectId
        self.discountAmount = discountAmount
        self.isValid = isValid
        self.eligibilityStatus = eligibilityStatus
    }
}

/// Kết quả tạo redemption session.
public struct PromotionRedemptionResult {
    public let sessionId: String
    public let totalDiscount: String
    public let finalAmount: String
    /// Lỗi validation nếu có voucher không hợp lệ trong session.
    public let validationErrors: [PromotionRedemptionError]

    public init(sessionId: String, totalDiscount: String, finalAmount: String, validationErrors: [PromotionRedemptionError] = []) {
        self.sessionId = sessionId
        self.totalDiscount = totalDiscount
        self.finalAmount = finalAmount
        self.validationErrors = validationErrors
    }
}

public struct PromotionRedemptionError {
    /// Mã lỗi nghiệp vụ, vd "VOUCHER_EXPIRED", "INSUFFICIENT_BUDGET".
    public let code: String
    /// Mô tả lỗi có thể hiển thị cho user.
    public let message: String

    public init(code: String, message: String) {
        self.code = code
        self.message = message
    }
}
