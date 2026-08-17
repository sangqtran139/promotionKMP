//
//  PromotionApiResult.swift
//  PromotionSDK
//
//  Đối ứng 1-1 với `PromotionApiResult.kt` bên Android. Sửa một bên thì sửa cả hai.
//

import Foundation
@_implementationOnly import PRMKotlinBridge

/// Kết quả của `PromotionSDKApi`. Bên Android là `sealed interface PromotionApiResult<T>` với hai
/// nhánh `Success` / `Failure`; Swift đã có `Result` nên chỉ cần đặt tên chung để hai tài liệu khớp nhau.
///
/// API **không ném lỗi nghiệp vụ**: mọi thất bại về `.failure`.
public typealias PromotionApiResult<T> = Result<T, PromotionSDKError>

/// Lỗi trả về từ SDK. Đối tác switch trên enum này để xử lý từng loại.
///
/// Bên Android là `sealed class PromotionSDKError : Exception()`, và `errorDescription` ở đây tương
/// ứng với `message` của `Throwable` bên đó.
public enum PromotionSDKError: Error, LocalizedError {
    /// Lỗi từ server: `code` là HTTP status nếu có, `message` là mô tả của server.
    case networkFailure(code: Int?, message: String)
    /// Token hết hạn — host refresh token rồi gọi lại.
    case sessionExpired
    case timeout
    /// Server trả `data: null` ở nơi bắt buộc phải có dữ liệu (chi tiết / validate / redemption).
    case parseFailed
    /// Tính năng đang TẮT qua feature flag. Tương ứng mã nghiệp vụ `PRM_MOB_021`.
    case featureDisabled
    case unknown(Error)

    public var errorDescription: String? {
        switch self {
        case .networkFailure(_, let message): return message
        case .sessionExpired:                 return "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại."
        case .timeout:                        return "Yêu cầu bị timeout, vui lòng thử lại."
        case .parseFailed:                    return "Có lỗi xảy ra với dữ liệu trả về."
        case .featureDisabled:                return "Tính năng ưu đãi hiện đang tạm thời không khả dụng. Vui lòng thử lại sau."
        case .unknown(let error):             return error.localizedDescription
        }
    }

    /// HTTP status nếu có — chỉ với `.networkFailure`.
    public var serverCode: Int? {
        if case .networkFailure(let code, _) = self { return code }
        return nil
    }
}

// MARK: - Mã lỗi thô → kiểu lỗi công khai

public extension PromotionSDKError {

    /// Mã lỗi **thô** (chuỗi) → kiểu lỗi công khai, để host bắt **tường minh** thay vì so chuỗi.
    ///
    /// Cần hàm này vì SDK có hai bề mặt lỗi không giống nhau:
    /// - `PromotionSDKApi` (headless) đã trả sẵn `PromotionSDKError` — `switch` là xong.
    /// - Callback của widget (`PromotionSDK.confirmRedemption(onError:)`) chỉ trả `String`, mà hằng
    ///   số mã lỗi nằm trong lõi Kotlin, không phơi ra cho host. Host đành hardcode `"PRM_MOB_021"`.
    ///
    /// ```swift
    /// PromotionSDK.confirmRedemption(onSuccess: { … }, onError: { code in
    ///     switch PromotionSDKError.from(code) {
    ///     case .featureDisabled: stopCheckout()   // SDK đã tự hiện popup, host chỉ cần dừng luồng
    ///     default:               showMyOwnError()
    ///     }
    /// })
    /// ```
    ///
    /// Đối ứng `PromotionSDKError.from(errorCode)` bên Android — sửa một bên thì sửa cả hai.
    static func from(_ errorCode: String) -> PromotionSDKError {
        switch errorCode {
        case PromotionErrorCodes.shared.FEATURE_DISABLED: return .featureDisabled
        case PromotionErrorCodes.shared.TIMEOUT:          return .timeout
        case PromotionErrorCodes.shared.NO_RESULT:        return .parseFailed
        case PromotionErrorCodes.shared.NETWORK_ERROR:    return .networkFailure(code: nil, message: "")
        default:                                          return .networkFailure(code: nil, message: errorCode)
        }
    }
}
