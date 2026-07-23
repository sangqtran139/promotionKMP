//
//  KotlinBridge.swift
//  PRMKotlinBridge
//
//  Cầu nối duy nhất giữa Swift và lõi Kotlin. KHÔNG đặt nghiệp vụ ở đây.
//
//  Ghi chú: prefix `PromotionLogic*` chỉ có ở header Objective-C. Kotlin/Native gắn `swift_name`
//  nên phía Swift thấy đúng tên gốc — `SearchCustomerVouchersUseCase`, `VoucherItem`, `KotlinInt`.
//  Không cần typealias để "làm đẹp" tên.
//

import Foundation
@_exported import PromotionLogic

// MARK: - Kiểu bọc số của Kotlin

/// `Int?` của Swift → `KotlinInt?` mà Kotlin/Native chờ đợi.
/// Kotlin `Int?`, `Boolean?`, `Long?` export thành `KotlinInt`, `KotlinBoolean`, `KotlinLong`
/// (đều kế thừa `NSNumber`), nên đọc ngược lại bằng `.intValue` / `.boolValue`.
public func boxed(_ value: Int?) -> KotlinInt? {
    guard let value else { return nil }
    return KotlinInt(int: Int32(value))
}

// MARK: - Lỗi

/// Lỗi đã chuẩn hoá, tương đương `PromotionResult.Failure` bên Kotlin.
/// `errorCode` là một trong `PromotionErrorCodes`, hoặc mã nghiệp vụ do server trả.
public struct PromotionError: Error {
    public let errorCode: String
    public let message: String?
    public let httpStatus: Int?

    public init(errorCode: String, message: String? = nil, httpStatus: Int? = nil) {
        self.errorCode = errorCode
        self.message = message
        self.httpStatus = httpStatus
    }
}

/// Bóc exception Kotlin ra khỏi `NSError`.
///
/// Kotlin/Native chỉ chuyển exception được đánh `@Throws` thành `NSError`, và nhét object gốc vào
/// `userInfo["KotlinException"]`. Exception **không** khai báo sẽ `abort()` tiến trình — vì vậy năm
/// use case đều mang `@Throws(PromotionException, NetworkException, CancellationException)`.
public func toPromotionError(_ error: Error) -> PromotionError {
    let nsError = error as NSError
    let kotlinException = nsError.userInfo["KotlinException"]

    if let promotionException = kotlinException as? PromotionException {
        return PromotionError(
            errorCode: promotionException.errorCode ?? PromotionErrorCodes.shared.GENERAL,
            message: promotionException.message,
            httpStatus: promotionException.httpStatus?.intValue
        )
    }

    if let networkException = kotlinException as? NetworkException {
        return PromotionError(
            errorCode: networkException.errorCode,
            message: networkException.message
        )
    }

    return PromotionError(
        errorCode: PromotionErrorCodes.shared.GENERAL,
        message: nsError.localizedDescription
    )
}
