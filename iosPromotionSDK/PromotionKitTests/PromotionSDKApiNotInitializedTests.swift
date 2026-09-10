//
//  PromotionSDKApiNotInitializedTests.swift
//  PromotionKitTests
//

import XCTest
@testable import PromotionKit

/// Khoá lại hành vi "gọi `api` trước `initialize`" — chỗ này từng là `preconditionFailure`, tức SDK
/// **làm crash app của host** vì lỗi thứ tự khởi tạo của *host*. Loại lỗi rất dễ xảy ra khi host vào
/// màn bằng deep link, và SDK này nhúng vào luồng thanh toán.
///
/// Test chạy được đúng vì nó **không** `initialize`: đây là trạng thái mặc định của process test.
/// Nếu ai đó sau này thêm `initialize` vào `setUp` chung thì test này mất ý nghĩa — giữ nó tách file.
/// `@MainActor`: `PromotionSDK` nay MainActor-isolated (API-3).
@MainActor
final class PromotionSDKApiNotInitializedTests: XCTestCase {

    func test_chuaInitialize_thi_isInitialized_false() {
        XCTAssertFalse(PromotionSDK.isInitialized())
    }

    /// Không crash, không ném, không optional: chỉ trả về một bề mặt dùng được.
    func test_docApi_truocInitialize_khongCrash() {
        _ = PromotionSDK.api
    }

    /// Và mọi hàm của bề mặt đó trả `.notInitialized` thay vì im lặng hoặc treo.
    func test_goiHam_truocInitialize_traNotInitialized() {
        let done = expectation(description: "getVouchers gọi completion")

        PromotionSDK.api.getVouchers { result in
            switch result {
            case .success:
                XCTFail("Chưa initialize thì không được có dữ liệu")
            case .failure(let error):
                guard case .notInitialized = error else {
                    return XCTFail("Phải là .notInitialized, nhận: \(error)")
                }
            }
            done.fulfill()
        }

        wait(for: [done], timeout: 2)
    }

    /// `sdkVersion` đọc được **trước** `initialize` — đó là điểm của nó: support hỏi "bản nào" thì
    /// trả lời được ngay, không phụ thuộc SDK đã khởi tạo hay chưa.
    func test_sdkVersion_docDuoc_truocInitialize() {
        let version = PromotionSDK.sdkVersion
        XCTAssertFalse(version.isEmpty)
        XCTAssertNotEqual(version, "unknown",
                          "Không đọc được CFBundleShortVersionString — kiểm MARKETING_VERSION của target PRM")
    }
}
