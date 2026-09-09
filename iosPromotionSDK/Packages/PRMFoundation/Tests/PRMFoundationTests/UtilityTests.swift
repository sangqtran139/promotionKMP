//
//  UtilityTests.swift
//  PRMFoundationTests
//
//  Trước đây file này là template Xcode sinh sẵn: một `@Test func example()` rỗng, 0 assertion.
//  Nó chạy xanh nên nhìn như package có test, trong khi không kiểm gì cả.
//

import Testing
import UIKit
@testable import PRMFoundation

/// `UIColor(hex:)` là móng của **toàn bộ** bảng màu: `Colors.swift` bên `PRMDesignKit` khai ~150
/// token bằng chính hàm này. Một lỗi parse ở đây đổi màu cả SDK mà không có gì báo — đúng loại lỗi
/// im lặng đáng test nhất trong một package tiện ích.
struct UIColorHexTests {

    private func rgba(_ color: UIColor) -> (CGFloat, CGFloat, CGFloat, CGFloat) {
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        color.getRed(&r, green: &g, blue: &b, alpha: &a)
        return (r, g, b, a)
    }

    @Test("Đỏ ViettelPay #EE0033 ra đúng ba kênh")
    func parseMauChuDao() {
        let (r, g, b, a) = rgba(UIColor(hex: "#EE0033"))
        #expect(abs(r - 238.0 / 255.0) < 0.001)
        #expect(abs(g - 0.0) < 0.001)
        #expect(abs(b - 51.0 / 255.0) < 0.001)
        #expect(a == 1.0)
    }

    @Test("Có hay không dấu # đều ra cùng một màu")
    func dauThangKhongDoiKetQua() {
        #expect(UIColor(hex: "#EE0033") == UIColor(hex: "EE0033"))
    }

    @Test("Chữ thường và chữ hoa ra cùng một màu")
    func khongPhanBietHoaThuong() {
        #expect(UIColor(hex: "#ee0033") == UIColor(hex: "#EE0033"))
    }

    @Test("Khoảng trắng thừa hai đầu bị bỏ qua")
    func trimKhoangTrang() {
        #expect(UIColor(hex: "  #EE0033\n") == UIColor(hex: "#EE0033"))
    }

    /// Chuỗi sai độ dài → ĐEN đục, **không** crash và **không** trong suốt.
    ///
    /// Khoá lại hành vi lùi này vì nó là thứ host nhìn thấy khi truyền theme sai: một ô đen dễ nhận
    /// ra ngay, khác hẳn `alpha = 0` (view biến mất, đi tìm bug ở nhầm chỗ).
    @Test("Chuỗi không hợp lệ lùi về đen đục, không trong suốt", arguments: [
        "", "#", "#FFF", "#EE00", "#EE003", "#EE00333", "khong-phai-hex",
    ])
    func chuoiSaiLuiVeDenDuc(_ input: String) {
        let (r, g, b, a) = rgba(UIColor(hex: input))
        #expect(r == 0 && g == 0 && b == 0)
        #expect(a == 1.0, "alpha phải là 1 — trong suốt thì view biến mất, khó lần ra nguyên nhân")
    }

    @Test("Trắng và đen ở hai đầu dải")
    func haiDauDai() {
        let trang = rgba(UIColor(hex: "#FFFFFF"))
        #expect(trang.0 == 1.0 && trang.1 == 1.0 && trang.2 == 1.0)

        let den = rgba(UIColor(hex: "#000000"))
        #expect(den.0 == 0.0 && den.1 == 0.0 && den.2 == 0.0)
    }
}
