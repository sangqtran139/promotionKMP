//
//  PromotionUITests.swift
//  PRMPromotionUITests
//
//  Trước đây file này là template Xcode sinh sẵn: một `@Test func example()` rỗng, 0 assertion.
//

import Testing
import UIKit
@testable import PRMPromotionUI

/// `PromotionCardModel` là **hợp đồng dựng card** giữa tầng màn hình và `PromotionCardView`.
///
/// Nó có 13 field mà 12 field có giá trị mặc định, nên `init` gọi được với đúng một tham số. Tiện,
/// nhưng cũng nghĩa là **thêm/đổi thứ tự tham số không làm vỡ chỗ gọi nào** — chỗ gọi vẫn biên dịch
/// và lặng lẽ nhận giá trị mặc định. Bộ test này chốt lại các mặc định đó.
struct PromotionCardModelTests {

    @Test("Chỉ truyền title — mọi thứ còn lại về mặc định 'không hiện gì'")
    func macDinhLaKhongHienGi() {
        let model = PromotionCardModel(title: "Giảm 50K")

        #expect(model.title == "Giảm 50K")
        #expect(model.logoURLString == nil)
        #expect(model.dateString == nil)
        #expect(model.descriptionText == nil)
        #expect(model.buttonTitle == nil)
        #expect(model.stateText == nil)
        #expect(model.highlightKeyword == nil)
    }

    /// Ba cờ boolean đều mặc định `false`. Đây là mặc định **an toàn**: card không tick, không mờ,
    /// không lòi ô chọn ở màn chỉ để xem.
    @Test("Ba cờ boolean mặc định đều false")
    func coBooleanMacDinhFalse() {
        let model = PromotionCardModel(title: "x")

        #expect(model.showsCheckbox == false)
        #expect(model.isChecked == false)
        #expect(model.isDisabled == false)
    }

    /// `stateText` rỗng ≠ `nil` nhưng **cùng nghĩa** với `PromotionCardView`: nó ẩn dải trạng thái
    /// theo `stateText?.isEmpty ?? true`. Chốt lại để ai đổi sang `!= nil` thì test đỏ — đổi vậy là
    /// card bỗng hiện một badge trống trơn.
    @Test("stateText rỗng và nil cùng nghĩa 'không có badge'", arguments: [nil, ""])
    func stateTextRongVaNilCungNghia(_ value: String?) {
        let model = PromotionCardModel(title: "x", stateText: value)

        #expect((model.stateText?.isEmpty ?? true) == true)
    }

    @Test("Giá trị truyền vào được giữ nguyên, không bị biến đổi")
    func giuNguyenGiaTriTruyenVao() {
        let model = PromotionCardModel(
            logoURLString: "https://example.invalid/logo.png",
            dateString: "HSD: 31/12/2026",
            title: "Giảm 50K",
            highlightKeyword: "50K",
            descriptionText: "Cho đơn từ 500K",
            buttonTitle: "Chi tiết",
            stateText: "Đã hết hạn",
            showsCheckbox: true,
            isChecked: true,
            isDisabled: true
        )

        #expect(model.logoURLString == "https://example.invalid/logo.png")
        #expect(model.dateString == "HSD: 31/12/2026")
        #expect(model.highlightKeyword == "50K")
        #expect(model.descriptionText == "Cho đơn từ 500K")
        #expect(model.buttonTitle == "Chi tiết")
        #expect(model.stateText == "Đã hết hạn")
        #expect(model.showsCheckbox && model.isChecked && model.isDisabled)
    }
}
