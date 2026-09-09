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
///
/// ⚠️ **Thêm case là breaking change.** Enum này public, có associated value, và Swift không cho
/// `@frozen` ở module thường — nên mọi `switch` đã liệt kê đủ case bên host sẽ vỡ khi SDK thêm một
/// case. Chốt xong danh sách trước go-live; sau đó thêm case = major bump.
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
    /// **Lỗi nghiệp vụ của server**, không phải lỗi mạng: `code` là mã server trả (vd
    /// `VOUCHER_EXPIRED`), `message` là câu server soạn cho người dùng — `nil` khi server không kèm.
    ///
    /// Trước đây nhánh này bị nhét vào `.networkFailure` với `message = errorCode`: host nhận một
    /// "lỗi mạng" mà thật ra là rule nghiệp vụ, và **mã lỗi thô nằm đúng chỗ đáng lẽ là câu hiển thị
    /// cho người dùng** — hiện thẳng lên UI là ra chữ `VOUCHER_EXPIRED`.
    case businessRule(code: String, message: String?)
    /// Gọi API khi chưa `PromotionSDK.initialize`. Trước đây ca này là `preconditionFailure` —
    /// SDK làm **crash app của host** vì lỗi thứ tự khởi tạo của host.
    case notInitialized
    case unknown(Error)

    public var errorDescription: String? {
        switch self {
        case .networkFailure(_, let message): return message
        case .sessionExpired:                 return "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại."
        case .timeout:                        return "Yêu cầu bị timeout, vui lòng thử lại."
        case .parseFailed:                    return "Có lỗi xảy ra với dữ liệu trả về."
        case .featureDisabled:                return "Tính năng ưu đãi hiện đang tạm thời không khả dụng. Vui lòng thử lại sau."
        case .businessRule(_, let message):   return message ?? "Đã có lỗi xảy ra."
        case .notInitialized:                 return "PromotionSDK chưa được khởi tạo."
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
    /// - Parameters:
    ///   - serverMessage: câu của server, chỉ có ở bề mặt headless. Callback của widget chỉ có mã.
    ///   - httpStatus: HTTP status, cùng lý do.
    static func from(_ errorCode: String,
                     serverMessage: String? = nil,
                     httpStatus: Int? = nil) -> PromotionSDKError {
        switch errorCode {
        case PromotionErrorCodes.shared.FEATURE_DISABLED: return .featureDisabled
        case PromotionErrorCodes.shared.TIMEOUT:          return .timeout
        case PromotionErrorCodes.shared.NO_RESULT:        return .parseFailed
        // Lùi về câu tiếng Việt của SDK khi server không nói gì: đây là case HAY XẢY RA NHẤT, và
        // trước đây là case DUY NHẤT trả `message: ""` — host hiện `error.localizedDescription` thì
        // ra popup trắng không chữ. Chuỗi trùng `PromotionUIStrings` để hai bề mặt (headless và UI
        // của SDK) không nói hai câu khác nhau cho cùng một sự cố.
        case PromotionErrorCodes.shared.NETWORK_ERROR:
            let message = serverMessage?.trimmingCharacters(in: .whitespaces)
            return .networkFailure(code: httpStatus,
                                   message: (message?.isEmpty == false ? message! : networkErrorMessage))
        // Mã nghiệp vụ của server (vd VOUCHER_EXPIRED) — KHÔNG phải lỗi mạng.
        default:
            let message = serverMessage?.trimmingCharacters(in: .whitespaces)
            return .businessRule(code: errorCode, message: message?.isEmpty == false ? message : nil)
        }
    }

    /// Trùng `R.string.prm_error_network` bên Android — sửa một bên thì sửa cả hai.
    private static var networkErrorMessage: String {
        "Không có kết nối mạng. Vui lòng kiểm tra rồi thử lại"
    }
}
