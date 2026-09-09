//
//  PromotionSDKErrorTests.swift
//  PromotionSDKTests
//
//  Test đầu tiên của tầng Entry (iOS). Thư mục này từng **không tồn tại** trong khi target
//  `PromotionSDKTests` vẫn khai trong `PRM.xcodeproj` — target rỗng thì `xcodebuild test` xanh mà
//  không chạy gì. Group của target là `PBXFileSystemSynchronizedRootGroup`, nên chỉ cần đặt file
//  vào đây là Xcode nhận, không phải sửa `project.pbxproj`.
//

import XCTest
@testable import PRM

/// Phủ `PromotionSDKError.from(_:serverMessage:httpStatus:)` — **đường map lỗi duy nhất** của SDK,
/// dùng chung cho cả bề mặt headless lẫn callback của widget.
///
/// Vì sao đáng test: đây đúng loại lỗi mà "0 test tầng Swift" để lọt — code biên dịch được, doc đầy
/// đủ, hành vi sai. Hai bug thật đã sống ở đây tới lúc rà soát mới lộ ra:
/// lỗi mạng trả `message` rỗng (host hiện popup trắng không chữ), và mã nghiệp vụ của server bị
/// nhét vào `.networkFailure` với mã lỗi thô nằm đúng chỗ đáng lẽ là câu cho người dùng.
///
/// Mã lỗi ở đây viết **thô** (`"network_error"`, không dùng `PromotionErrorCodes.shared`) là có chủ
/// đích: chúng là hợp đồng trên dây với BFF. Tham chiếu hằng số thì đổi hằng số một chỗ là test vẫn
/// xanh trong khi hợp đồng đã gãy.
final class PromotionSDKErrorTests: XCTestCase {

    // MARK: - Lỗi mạng

    func test_networkError_khongServerMessage_luiVeCauCuaSDK() {
        let error = PromotionSDKError.from("network_error")

        guard case .networkFailure(let code, let message) = error else {
            return XCTFail("Phải là .networkFailure, nhận: \(error)")
        }
        XCTAssertNil(code)
        XCTAssertFalse(message.isEmpty, "Đây là bug cũ: message rỗng → host hiện popup trắng")
        XCTAssertEqual(message, "Không có kết nối mạng. Vui lòng kiểm tra rồi thử lại")
    }

    func test_networkError_coServerMessage_thiUuTienCuaServer() {
        let error = PromotionSDKError.from("network_error",
                                           serverMessage: "Mất kết nối tới máy chủ",
                                           httpStatus: 503)

        guard case .networkFailure(let code, let message) = error else {
            return XCTFail("Phải là .networkFailure, nhận: \(error)")
        }
        XCTAssertEqual(code, 503)
        XCTAssertEqual(message, "Mất kết nối tới máy chủ")
    }

    /// Server trả chuỗi toàn khoảng trắng cũng phải coi như không có.
    func test_networkError_serverMessageToanKhoangTrang_van_luiVeCauCuaSDK() {
        let error = PromotionSDKError.from("network_error", serverMessage: "   ")

        guard case .networkFailure(_, let message) = error else {
            return XCTFail("Phải là .networkFailure, nhận: \(error)")
        }
        XCTAssertEqual(message, "Không có kết nối mạng. Vui lòng kiểm tra rồi thử lại")
    }

    // MARK: - Lỗi nghiệp vụ

    /// Mã không nhận ra là **rule nghiệp vụ**, KHÔNG phải lỗi mạng.
    func test_maLaKhongNhanRa_raBusinessRule_khongPhaiNetworkFailure() {
        let error = PromotionSDKError.from("VOUCHER_EXPIRED",
                                           serverMessage: "Voucher đã hết hạn")

        guard case .businessRule(let code, let message) = error else {
            return XCTFail("Phải là .businessRule, nhận: \(error)")
        }
        XCTAssertEqual(code, "VOUCHER_EXPIRED")
        XCTAssertEqual(message, "Voucher đã hết hạn")
    }

    /// Không có câu của server → `message` là `nil`, và `errorDescription` lùi về câu chung.
    /// Bug cũ: mã thô `VOUCHER_EXPIRED` bị đặt vào chỗ câu hiển thị, host hiện thẳng lên UI.
    func test_businessRule_khongCoServerMessage_khongLoMaThoRaUI() {
        let error = PromotionSDKError.from("VOUCHER_EXPIRED")

        guard case .businessRule(let code, let message) = error else {
            return XCTFail("Phải là .businessRule, nhận: \(error)")
        }
        XCTAssertEqual(code, "VOUCHER_EXPIRED")
        XCTAssertNil(message)
        XCTAssertNotEqual(error.errorDescription, "VOUCHER_EXPIRED",
                          "Mã lỗi thô không được rơi vào câu hiển thị cho người dùng")
    }

    // MARK: - Các mã có nhánh riêng

    func test_maCoNhanhRieng_mapDungType() {
        guard case .featureDisabled = PromotionSDKError.from("PRM_MOB_021") else {
            return XCTFail("PRM_MOB_021 phải ra .featureDisabled")
        }
        guard case .timeout = PromotionSDKError.from("timeout") else {
            return XCTFail("timeout phải ra .timeout")
        }
        guard case .parseFailed = PromotionSDKError.from("no_result") else {
            return XCTFail("no_result phải ra .parseFailed")
        }
    }

    /// Mọi nhánh phải có câu đọc được — không nhánh nào để rỗng.
    func test_moiNhanh_deuCoErrorDescription_khongRong() {
        let errors: [PromotionSDKError] = [
            .networkFailure(code: nil, message: "x"),
            .businessRule(code: "C", message: nil),
            .sessionExpired,
            .timeout,
            .parseFailed,
            .featureDisabled,
            .notInitialized,
        ]
        for error in errors {
            let text = error.errorDescription ?? ""
            XCTAssertFalse(text.isEmpty, "Nhánh \(error) không có câu hiển thị")
        }
    }

    // MARK: - serverCode

    func test_serverCode_chiCoONetworkFailure() {
        XCTAssertEqual(PromotionSDKError.networkFailure(code: 502, message: "x").serverCode, 502)
        XCTAssertNil(PromotionSDKError.businessRule(code: "C", message: nil).serverCode)
        XCTAssertNil(PromotionSDKError.notInitialized.serverCode)
    }
}
