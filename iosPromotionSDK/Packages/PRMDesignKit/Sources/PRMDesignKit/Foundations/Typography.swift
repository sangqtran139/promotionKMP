//
//  Typography.swift
//  CoreUIKit
//
//  Created by Natariannn on 8/7/20.
//  Copyright © 2020 ViettelPay App Team. All rights reserved.
//

import UIKit

public enum FontWeights {
    // Font Weights
    public static let tokenFontWeight400: UIFont.Weight = .regular
    public static let tokenFontWeight500: UIFont.Weight = .medium
    public static let tokenFontWeight700: UIFont.Weight = .bold
}

public enum FontSizes {
    // Font Sizes
    public static let tokenFontSize40: CGFloat = 40.0
    public static let tokenFontSize32: CGFloat = 32.0
    public static let tokenFontSize24: CGFloat = 24.0
    public static let tokenFontSize22: CGFloat = 22.0
    public static let tokenFontSize18: CGFloat = 18.0
    public static let tokenFontSize16: CGFloat = 16.0
    public static let tokenFontSize14: CGFloat = 14.0
    public static let tokenFontSize13: CGFloat = 13.0
    public static let tokenFontSize12: CGFloat = 12.0
    public static let tokenFontSize10: CGFloat = 10.0
    public static let tokenFontSize9: CGFloat = 9.0
    public static let tokenFontSize8: CGFloat = 8.0
}

public enum Typography {

    // MARK: - Dynamic Type

    /// Trần phóng to của cỡ chữ, tính theo bội số cỡ gốc.
    ///
    /// Vì sao phải có trần: iOS cho user kéo cỡ chữ tới **AX5** — khoảng 3,8× cỡ mặc định. Nhiều ô
    /// trong XIB của SDK đang cố định chiều cao (13 ràng buộc: 13, 30, 32, 44, 48, 50, 128, 173pt),
    /// nên để chữ phóng tự do là **chữ bị cắt** — tệ hơn hiện trạng, không phải tốt hơn.
    ///
    /// `1.3` là mức chữ vẫn nằm gọn trong mọi ô đang có (14pt → 18,2pt trong ô 48pt). Muốn nới thì
    /// **phải gỡ chiều cao cố định trong XIB trước**, rồi mới nâng số này và duyệt lại bằng mắt ở
    /// cỡ chữ lớn nhất — đó là hai việc, không phải một.
    public static let dynamicTypeMaxScale: CGFloat = 1.3

    /// Cỡ gốc → cỡ đã scale theo cài đặt cỡ chữ của user, chặn trần ở ``dynamicTypeMaxScale``.
    ///
    /// `static var` (computed) chứ **không** phải `static let`: `static let` chỉ tính một lần cho cả
    /// vòng đời tiến trình, nên user đổi cỡ chữ giữa chừng thì mọi label vẫn giữ cỡ của lần đọc đầu.
    private static func scaled(_ size: CGFloat, _ weight: UIFont.Weight) -> UIFont {
        let base = UIFont.systemFont(ofSize: size, weight: weight)
        return UIFontMetrics(forTextStyle: .body)
            .scaledFont(for: base, maximumPointSize: size * dynamicTypeMaxScale)
    }
    // Line Heights
    public static let tokenLineHeight28: CGFloat = 28.0
    public static let tokenLineHeight26: CGFloat = 26.0
    public static let tokenLineHeight22: CGFloat = 22.0
    public static let tokenLineHeight20: CGFloat = 20.0
    public static let tokenLineHeight18: CGFloat = 18.0
    public static let tokenLineHeight16: CGFloat = 16.0
    public static let tokenLineHeight14: CGFloat = 14.0
    public static let tokenLineHeight12: CGFloat = 12.0
    public static let tokenLineHeight10: CGFloat = 10.0
    
    // Font 32
    public static var fontRegular40: UIFont { scaled(FontSizes.tokenFontSize40, FontWeights.tokenFontWeight400) }
    public static var fontMedium40: UIFont { scaled(FontSizes.tokenFontSize40, FontWeights.tokenFontWeight500) }
    public static var fontBold40: UIFont { scaled(FontSizes.tokenFontSize40, FontWeights.tokenFontWeight700) }
    
    
    // Font 32
    public static var fontRegular32: UIFont { scaled(FontSizes.tokenFontSize32, FontWeights.tokenFontWeight400) }
    public static var fontMedium32: UIFont { scaled(FontSizes.tokenFontSize32, FontWeights.tokenFontWeight500) }
    public static var fontBold32: UIFont { scaled(FontSizes.tokenFontSize32, FontWeights.tokenFontWeight700) }
    
    // Font 24
    public static var fontRegular24: UIFont { scaled(FontSizes.tokenFontSize24, FontWeights.tokenFontWeight400) }
    public static var fontMedium24: UIFont { scaled(FontSizes.tokenFontSize24, FontWeights.tokenFontWeight500) }
    public static var fontBold24: UIFont { scaled(FontSizes.tokenFontSize24, FontWeights.tokenFontWeight700) }

    // Font 22
    public static var fontRegular22: UIFont { scaled(FontSizes.tokenFontSize22, FontWeights.tokenFontWeight400) }
    public static var fontMedium22: UIFont { scaled(FontSizes.tokenFontSize22, FontWeights.tokenFontWeight500) }
    public static var fontBold22: UIFont { scaled(FontSizes.tokenFontSize22, FontWeights.tokenFontWeight700) }
    
    // Font 18
    public static var fontRegular18: UIFont { scaled(FontSizes.tokenFontSize18, FontWeights.tokenFontWeight400) }
    public static var fontMedium18: UIFont { scaled(FontSizes.tokenFontSize18, FontWeights.tokenFontWeight500) }
    public static var fontBold18: UIFont { scaled(FontSizes.tokenFontSize18, FontWeights.tokenFontWeight700) }
    
    // Font 16
    public static var fontRegular16: UIFont { scaled(FontSizes.tokenFontSize16, FontWeights.tokenFontWeight400) }
    public static var fontMedium16: UIFont { scaled(FontSizes.tokenFontSize16, FontWeights.tokenFontWeight500) }
    public static var fontBold16: UIFont { scaled(FontSizes.tokenFontSize16, FontWeights.tokenFontWeight700) }
    
    // Font 14
    public static var fontRegular14: UIFont { scaled(FontSizes.tokenFontSize14, FontWeights.tokenFontWeight400) }
    public static var fontMedium14: UIFont { scaled(FontSizes.tokenFontSize14, FontWeights.tokenFontWeight500) }
    public static var fontBold14: UIFont { scaled(FontSizes.tokenFontSize14, FontWeights.tokenFontWeight700) }
    
    // Font 13
    public static var fontRegular13: UIFont { scaled(FontSizes.tokenFontSize13, FontWeights.tokenFontWeight400) }
    public static var fontMedium13: UIFont { scaled(FontSizes.tokenFontSize13, FontWeights.tokenFontWeight500) }
    public static var fontBold13: UIFont { scaled(FontSizes.tokenFontSize13, FontWeights.tokenFontWeight700) }
    
    // Font 12
    public static var fontRegular12: UIFont { scaled(FontSizes.tokenFontSize12, FontWeights.tokenFontWeight400) }
    public static var fontMedium12: UIFont { scaled(FontSizes.tokenFontSize12, FontWeights.tokenFontWeight500) }
    public static var fontBold12: UIFont { scaled(FontSizes.tokenFontSize12, FontWeights.tokenFontWeight700) }
    
    // Font 10
    public static var fontRegular10: UIFont { scaled(FontSizes.tokenFontSize10, FontWeights.tokenFontWeight400) }
    public static var fontMedium10: UIFont { scaled(FontSizes.tokenFontSize10, FontWeights.tokenFontWeight500) }
    public static var fontBold10: UIFont { scaled(FontSizes.tokenFontSize10, FontWeights.tokenFontWeight700) }

    // Font 9
    public static var fontRegular9: UIFont { scaled(FontSizes.tokenFontSize9, FontWeights.tokenFontWeight400) }
    public static var fontMedium9: UIFont { scaled(FontSizes.tokenFontSize9, FontWeights.tokenFontWeight500) }
    public static var fontBold9: UIFont { scaled(FontSizes.tokenFontSize9, FontWeights.tokenFontWeight700) }
    
    // Font 8
    public static var fontRegular8: UIFont { scaled(FontSizes.tokenFontSize8, FontWeights.tokenFontWeight400) }
    public static var fontMedium8: UIFont { scaled(FontSizes.tokenFontSize8, FontWeights.tokenFontWeight500) }
    public static var fontBold8: UIFont { scaled(FontSizes.tokenFontSize8, FontWeights.tokenFontWeight700) }
}
