//
//  EndowHostNotifierEmitTests.swift
//  PromotionSDKTests
//

import XCTest
@testable import PRM
// `EndowHostEvent` là type Kotlin, PRM import nó bằng `@_implementationOnly` nên nó KHÔNG đi kèm
// `@testable import PRM`. Test phải tự import bridge để dựng được sự kiện.
import PRMKotlinBridge

/// Khoá **thứ tự và số lần** SDK gọi callback của host.
///
/// Vì sao cần: CHANGELOG ghi lại một lần `PromotionSDKImpl.emit` đổi thứ tự phát callback
/// (applied-trước → count-trước-applied). **Không một chữ ký nào đổi**, nên không có gì bắt được —
/// host nào dựa vào thứ tự thì vỡ im lặng. Đây là loại thay đổi dễ xảy ra nhất khi ai đó sắp xếp lại
/// một vòng `for` trông như dọn dẹp.
///
/// Rule "khi nào bắn" nằm ở `EndowHostNotifier` (Kotlin, đã có golden test bên `commonTest`). File
/// này phủ nốt vế còn lại: **phép map từ sự kiện sang callback iOS**, thứ chỉ tồn tại ở tầng Swift.
/// `@MainActor` cả class: `PromotionSDKImpl` nay MainActor-isolated (xem `PromotionSDK`, API-3).
/// XCTest chạy test trên main thread nên đây chỉ là nói ra điều vốn đã đúng.
@MainActor
final class EndowHostNotifierEmitTests: XCTestCase {

    private var impl: PromotionSDKImpl!
    private var nhanDuoc: [String] = []

    override func setUp() {
        super.setUp()
        nhanDuoc = []
        // `.invalid` là TLD dành riêng (RFC 2606) — không phân giải được, nên lượt nạp cờ mà `init`
        // bắn đi sẽ hỏng ngay thay vì gọi ra mạng thật. Các test ở đây không chạm tầng network.
        impl = PromotionSDKImpl(options: PromotionSDKOptions(
            session: PromotionSessionConfig(tokenSource: StubTokenSource(),
                                            baseUrl: "https://khong-ton-tai.invalid")
        ))
        impl.onApplyVoucher = { [weak self] voucherId in
            self?.nhanDuoc.append(voucherId)
        }
    }

    override func tearDown() {
        impl = nil
        super.tearDown()
    }

    /// Danh sách rỗng → **không** gọi callback lần nào.
    func test_khongCoSuKien_thiKhongGoiCallback() {
        impl.emit([])

        XCTAssertEqual(nhanDuoc, [])
    }

    /// Nhiều sự kiện → gọi **đúng thứ tự đó**, không đảo, không gộp.
    /// `assertEqual` trên cả mảng chứ không `contains`: `contains` vẫn xanh khi thứ tự đảo.
    func test_nhieuSuKien_goiDungThuTu() {
        impl.emit([
            EndowHostEventVoucherApplied(voucherId: "v1"),
            EndowHostEventVoucherApplied(voucherId: "v2"),
            EndowHostEventVoucherApplied(voucherId: "v3"),
        ])

        XCTAssertEqual(nhanDuoc, ["v1", "v2", "v3"])
    }

    /// Một sự kiện = **đúng một** lần gọi. Gọi hai lần cho một sự kiện là host xử lý trùng.
    func test_moiSuKienGoiDungMotLan() {
        impl.emit([EndowHostEventVoucherApplied(voucherId: "v1")])

        XCTAssertEqual(nhanDuoc.count, 1)
    }

    /// Host không đăng ký callback → `emit` không được nổ.
    func test_khongCoCallback_thiKhongCrash() {
        impl.onApplyVoucher = nil

        impl.emit([EndowHostEventVoucherApplied(voucherId: "v1")])
    }
}

/// Không gọi mạng: token rỗng là đủ, các test ở đây không chạm tầng network.
private final class StubTokenSource: PromotionTokenSource {
    func currentToken() -> String? { nil }
}
