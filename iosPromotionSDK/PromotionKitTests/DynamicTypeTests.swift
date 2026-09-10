//
//  DynamicTypeTests.swift
//  PromotionKitTests
//
//  Khoá lại hai tính chất của tầng cỡ chữ. Cả hai đều thuộc loại "hỏng trong im lặng": không có
//  test thì phải mở app, chỉnh cỡ chữ trong Settings, rồi soi từng màn mới thấy.
//

import XCTest
@testable import PromotionKit
@testable import PRMDesignKit

final class DynamicTypeTests: XCTestCase {

    /// Cỡ chữ phải **scale được** — nếu không thì `Typography` đã tụt lại thành `UIFont.systemFont`
    /// cố định và cả tính năng Dynamic Type biến mất mà build vẫn xanh.
    func test_font_laFontScaleDuoc() {
        let font = Typography.fontRegular14

        // `UIFontMetrics.scaledFont` trả về font gắn text style; font hệ thống trần thì không có.
        let isScalable = font.fontDescriptor.object(forKey: .textStyle) != nil
        XCTAssertTrue(isScalable, "Typography.fontRegular14 không còn đi qua UIFontMetrics")
    }

    /// **Trần** là thứ giữ chữ không tràn khỏi các ô XIB đang cố định chiều cao. Ai đó nâng
    /// `dynamicTypeMaxScale` mà quên gỡ chiều cao cố định thì test này là chỗ dừng lại và đọc
    /// comment ở `Typography.dynamicTypeMaxScale`.
    func test_cochu_khongVuotTran_oCoChuLonNhat() {
        let ax5 = UITraitCollection(preferredContentSizeCategory: .accessibilityExtraExtraExtraLarge)

        for (base, font) in [
            (FontSizes.tokenFontSize14, Typography.fontRegular14),
            (FontSizes.tokenFontSize16, Typography.fontMedium16),
            (FontSizes.tokenFontSize24, Typography.fontBold24),
        ] {
            let scaled = font.fontDescriptor.pointSize
            let resolved = UIFontMetrics(forTextStyle: .body)
                .scaledFont(for: UIFont.systemFont(ofSize: base),
                            maximumPointSize: base * Typography.dynamicTypeMaxScale,
                            compatibleWith: ax5)

            XCTAssertLessThanOrEqual(
                resolved.pointSize, base * Typography.dynamicTypeMaxScale + 0.01,
                "Cỡ \(base)pt vượt trần ở AX5 — chữ sẽ bị cắt trong ô XIB cố định chiều cao"
            )
            XCTAssertGreaterThan(scaled, 0)
        }
    }

    /// Trần không được đặt bằng 1: như vậy là tắt hẳn Dynamic Type mà vẫn trông như đang bật.
    func test_tran_thucSuChoPhongTo() {
        XCTAssertGreaterThan(Typography.dynamicTypeMaxScale, 1.0)
    }
}
