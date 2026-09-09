//
//  CoreUITests.swift
//  PRMDesignKitTests
//
//  Trước đây file này là template Xcode sinh sẵn: một `@Test func example()` rỗng, 0 assertion.
//

import Testing
import UIKit
@testable import PRMDesignKit

/// Golden test cho **bảng màu**: khoá lại giá trị hex của các token mà host nhìn thấy.
///
/// `Colors.swift` khai ~150 token bằng chuỗi hex viết tay. Sửa nhầm một ký tự là đổi màu ở mọi màn
/// mà không có gì báo — không compile error, không crash, chỉ là màu khác đi. Bộ này không phủ hết
/// 150 token (vô nghĩa), mà chốt những token **thật sự là bản sắc thương hiệu** và những token bị
/// dùng nhiều nhất.
struct ColorTokenTests {

    private func hex(_ color: UIColor) -> String {
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        color.getRed(&r, green: &g, blue: &b, alpha: &a)
        return String(format: "#%02X%02X%02X", Int(r * 255 + 0.5), Int(g * 255 + 0.5), Int(b * 255 + 0.5))
    }

    @Test("Đỏ ViettelPay 100 là #EE0033 — màu thương hiệu, đổi là sai nhận diện")
    func doViettelPay() {
        #expect(hex(Colors.tokenViettelPayRed100) == "#EE0033")
    }

    @Test("Dải đỏ ViettelPay nhạt dần đúng thứ tự 100 → 02")
    func daiDoNhatDan() {
        // Mỗi bậc phải sáng hơn bậc trước. Bắt được ca chép nhầm giá trị giữa hai bậc — thứ mắt
        // thường không thấy vì hai màu cạnh nhau rất giống.
        let dai = [
            Colors.tokenViettelPayRed100,
            Colors.tokenViettelPayRed80,
            Colors.tokenViettelPayRed60,
            Colors.tokenViettelPayRed40,
            Colors.tokenViettelPayRed20,
            Colors.tokenViettelPayRed10,
            Colors.tokenViettelPayRed05,
            Colors.tokenViettelPayRed02,
        ]
        let doSang = dai.map { color -> CGFloat in
            var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
            color.getRed(&r, green: &g, blue: &b, alpha: &a)
            return 0.299 * r + 0.587 * g + 0.114 * b
        }
        for i in 1..<doSang.count {
            #expect(doSang[i] > doSang[i - 1],
                    "Bậc \(i) phải sáng hơn bậc \(i - 1) — kiểm lại giá trị hex trong Colors.swift")
        }
    }

    @Test("Mọi token đều đục — alpha = 1")
    func moiTokenDeuDuc() {
        // `UIColor(hex:)` lùi về ĐEN ĐỤC khi chuỗi sai, nên alpha = 1 không chứng minh chuỗi đúng.
        // Nhưng alpha < 1 thì chắc chắn có ai đó dựng màu bằng đường khác — đáng biết.
        let tokens: [UIColor] = [
            Colors.tokenViettelPayRed100, Colors.tokenScarletRed100,
            Colors.tokenCrimsonRed100, Colors.tokenSpaceBlue100,
        ]
        for token in tokens {
            var a: CGFloat = 0
            token.getRed(nil, green: nil, blue: nil, alpha: &a)
            #expect(a == 1.0)
        }
    }
}

/// Token khoảng cách là thang 4pt. Một giá trị lệch thang làm layout lệch ở mọi chỗ dùng nó.
struct SpacingTokenTests {

    @Test("Thang spacing đúng giá trị và tăng dần")
    func thangSpacing() {
        let thang: [CGFloat] = [
            Spacing.tokenSpacing00, Spacing.tokenSpacing02, Spacing.tokenSpacing04,
            Spacing.tokenSpacing08, Spacing.tokenSpacing12, Spacing.tokenSpacing16,
            Spacing.tokenSpacing22, Spacing.tokenSpacing24,
        ]
        #expect(thang == [0, 2, 4, 8, 12, 16, 22, 24])
        for i in 1..<thang.count {
            #expect(thang[i] > thang[i - 1], "Thang spacing phải tăng dần")
        }
    }
}
