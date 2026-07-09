//
//  VDSPromotionResults.swift
//  PromotionSDK
//
//  Foundation-only public result types for the headless API.
//  Keeps RxSwift and internal domain models out of the framework's public module interface.
//

import Foundation

/// Result of validating a set of vouchers against an order.
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

/// Individual discount item within a validation result.
public struct PromotionDiscountItem {
    public let objectId: String
    public let discountAmount: String
    public let isValid: Bool
    /// Lý do voucher không hợp lệ, ví dụ: "EXPIRED", "BUDGET_EXCEEDED", "NOT_ELIGIBLE".
    public let eligibilityStatus: String

    public init(objectId: String, discountAmount: String, isValid: Bool, eligibilityStatus: String = "") {
        self.objectId = objectId
        self.discountAmount = discountAmount
        self.isValid = isValid
        self.eligibilityStatus = eligibilityStatus
    }
}

/// Lỗi validation trả về trong kết quả tạo redemption session.
public struct PromotionRedemptionError {
    /// Mã lỗi nghiệp vụ, ví dụ: "VOUCHER_EXPIRED", "INSUFFICIENT_ORDER_VALUE".
    public let code: String
    /// Mô tả lỗi có thể hiển thị cho user.
    public let message: String

    public init(code: String, message: String) {
        self.code = code
        self.message = message
    }
}

/// Result of creating a redemption session.
public struct PromotionRedemptionResult {
    public let sessionId: String
    public let totalDiscount: String
    public let finalAmount: String
    /// Danh sách lỗi validation nếu có voucher không hợp lệ trong session.
    public let validationErrors: [PromotionRedemptionError]

    public init(sessionId: String, totalDiscount: String, finalAmount: String, validationErrors: [PromotionRedemptionError] = []) {
        self.sessionId = sessionId
        self.totalDiscount = totalDiscount
        self.finalAmount = finalAmount
        self.validationErrors = validationErrors
    }
}

// MARK: - Error

/// Lỗi trả về từ SDK. Đối tác switch trên enum này để xử lý từng loại lỗi khác nhau.
public enum PromotionSDKError: Error, LocalizedError {
    /// Lỗi từ server: có code và message cụ thể.
    case networkFailure(code: Int?, message: String)
    /// Token hết hạn — cần refresh token và tạo lại PromotionSDK instance.
    case sessionExpired
    /// Request bị timeout.
    case timeout
    /// Không parse được response từ server.
    case parseFailed
    /// Tính năng đang bị TẮT qua feature flag (Unleash). Tương ứng lỗi nghiệp vụ PRM_MOB_021.
    case featureDisabled
    /// Lỗi không xác định.
    case unknown(Error)

    public var errorDescription: String? {
        switch self {
        case .networkFailure(_, let message): return message
        case .sessionExpired:                 return "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại."
        case .timeout:                        return "Yêu cầu bị timeout, vui lòng thử lại."
        case .parseFailed:                    return "Có lỗi xảy ra với dữ liệu trả về."
        case .featureDisabled:                return "Tính năng hiện đang tạm thời không khả dụng. Vui lòng thử lại sau."
        case .unknown(let error):             return error.localizedDescription
        }
    }

    /// HTTP status code nếu có (chỉ với case `.networkFailure`).
    public var serverCode: Int? {
        if case .networkFailure(let code, _) = self { return code }
        return nil
    }
}

/// 1 voucher trong danh sách trả cho đối tác.
public struct PromotionVoucher {
    public let id: String
    public let merchantName: String
    public let title: String
    public let imageURL: String?
    public let expireDate: Date?
    public let isUsed: Bool
    /// Mã trạng thái thô từ server (ACTIVE/USED/EXPIRED...).
    public let status: String?
    /// Nhãn trạng thái do server cung cấp (ưu tiên hiển thị).
    public let displayStatusLabel: String?

    public init(id: String, merchantName: String, title: String, imageURL: String?, expireDate: Date?, isUsed: Bool, status: String?, displayStatusLabel: String?) {
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

/// 1 ưu đãi đủ điều kiện (Eligible API) cho luồng checkout.
public struct PromotionEligibleOffer {
    /// Định danh để chọn/validate: voucherId (nhóm "của tôi") hoặc campaignId (nhóm "khác").
    public let id: String
    /// Tên ưu đãi hiển thị.
    public let name: String
    /// Loại campaign (DISCOUNT/COUPON...), dùng làm objectType khi validate.
    public let objectType: String
    /// true = dùng được ngay; false = chưa đủ điều kiện (hiển thị mờ).
    public let usable: Bool
    /// Số tiền giảm dự kiến (chuỗi số thô), nil nếu không có preview.
    public let estimatedDiscount: String?
    /// Ngày hết hạn (nếu có).
    public let expireDate: Date?
    /// Gợi ý lý do chưa đủ điều kiện (khi `usable == false`).
    public let ineligibleReason: String?

    public init(id: String, name: String, objectType: String, usable: Bool,
                estimatedDiscount: String?, expireDate: Date?, ineligibleReason: String?) {
        self.id = id
        self.name = name
        self.objectType = objectType
        self.usable = usable
        self.estimatedDiscount = estimatedDiscount
        self.expireDate = expireDate
        self.ineligibleReason = ineligibleReason
    }
}

/// Kết quả Find Eligible Campaigns: 2 nhóm "của tôi" / "khác" phân trang ĐỘC LẬP.
public struct PromotionEligibleResult {
    public let myOffers: [PromotionEligibleOffer]
    public let otherOffers: [PromotionEligibleOffer]
    public let myIsLastPage: Bool
    public let otherIsLastPage: Bool

    public init(myOffers: [PromotionEligibleOffer], otherOffers: [PromotionEligibleOffer],
                myIsLastPage: Bool, otherIsLastPage: Bool) {
        self.myOffers = myOffers
        self.otherOffers = otherOffers
        self.myIsLastPage = myIsLastPage
        self.otherIsLastPage = otherIsLastPage
    }
}

/// Chi tiết 1 voucher (Get Customer Voucher Detail).
public struct PromotionVoucherDetail {
    public let id: String
    public let merchantName: String
    public let title: String
    public let descriptionText: String
    /// Hướng dẫn sử dụng (điều khoản / cách dùng).
    public let guideline: String
    public let startDate: Date?
    public let expireDate: Date?
    public let bannerURL: String?
    public let logoURL: String?
    public let status: String
    public let displayStatusLabel: String?

    public init(id: String, merchantName: String, title: String, descriptionText: String,
                guideline: String, startDate: Date?, expireDate: Date?, bannerURL: String?,
                logoURL: String?, status: String, displayStatusLabel: String?) {
        self.id = id
        self.merchantName = merchantName
        self.title = title
        self.descriptionText = descriptionText
        self.guideline = guideline
        self.startDate = startDate
        self.expireDate = expireDate
        self.bannerURL = bannerURL
        self.logoURL = logoURL
        self.status = status
        self.displayStatusLabel = displayStatusLabel
    }
}

/// Kết quả lấy voucher **của khách** (Search Customer Vouchers).
/// API chỉ còn voucher đã sở hữu — tăng `myPage` để load thêm. Để lấy "Ưu đãi khác"
/// (campaign chưa sở hữu, đủ điều kiện cho đơn) dùng `findEligible(...)` → `PromotionEligibleResult`.
public struct PromotionVoucherPage {
    public let myVouchers: [PromotionVoucher]
    /// Trang cuối của danh sách voucher.
    public let myIsLastPage: Bool

    public init(myVouchers: [PromotionVoucher], myIsLastPage: Bool) {
        self.myVouchers = myVouchers
        self.myIsLastPage = myIsLastPage
    }
}
