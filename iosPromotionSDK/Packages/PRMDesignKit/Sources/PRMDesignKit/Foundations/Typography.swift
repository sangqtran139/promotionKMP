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
    public static let fontRegular40: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize40, weight: FontWeights.tokenFontWeight400)
    public static let fontMedium40: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize40, weight: FontWeights.tokenFontWeight500)
    public static let fontBold40: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize40, weight: FontWeights.tokenFontWeight700)
    
    
    // Font 32
    public static let fontRegular32: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize32, weight: FontWeights.tokenFontWeight400)
    public static let fontMedium32: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize32, weight: FontWeights.tokenFontWeight500)
    public static let fontBold32: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize32, weight: FontWeights.tokenFontWeight700)
    
    // Font 24
    public static let fontRegular24: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize24, weight: FontWeights.tokenFontWeight400)
    public static let fontMedium24: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize24, weight: FontWeights.tokenFontWeight500)
    public static let fontBold24: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize24, weight: FontWeights.tokenFontWeight700)

    // Font 22
    public static let fontRegular22: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize22, weight: FontWeights.tokenFontWeight400)
    public static let fontMedium22: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize22, weight: FontWeights.tokenFontWeight500)
    public static let fontBold22: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize22, weight: FontWeights.tokenFontWeight700)
    
    // Font 18
    public static let fontRegular18: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize18, weight: FontWeights.tokenFontWeight400)
    public static let fontMedium18: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize18, weight: FontWeights.tokenFontWeight500)
    public static let fontBold18: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize18, weight: FontWeights.tokenFontWeight700)
    
    // Font 16
    public static let fontRegular16: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize16, weight: FontWeights.tokenFontWeight400)
    public static let fontMedium16: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize16, weight: FontWeights.tokenFontWeight500)
    public static let fontBold16: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize16, weight: FontWeights.tokenFontWeight700)
    
    // Font 14
    public static let fontRegular14: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize14, weight: FontWeights.tokenFontWeight400)
    public static let fontMedium14: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize14, weight: FontWeights.tokenFontWeight500)
    public static let fontBold14: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize14, weight: FontWeights.tokenFontWeight700)
    
    // Font 13
    public static let fontRegular13: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize13, weight: FontWeights.tokenFontWeight400)
    public static let fontMedium13: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize13, weight: FontWeights.tokenFontWeight500)
    public static let fontBold13: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize13, weight: FontWeights.tokenFontWeight700)
    
    // Font 12
    public static let fontRegular12: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize12, weight: FontWeights.tokenFontWeight400)
    public static let fontMedium12: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize12, weight: FontWeights.tokenFontWeight500)
    public static let fontBold12: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize12, weight: FontWeights.tokenFontWeight700)
    
    // Font 10
    public static let fontRegular10: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize10, weight: FontWeights.tokenFontWeight400)
    public static let fontMedium10: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize10, weight: FontWeights.tokenFontWeight500)
    public static let fontBold10: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize10, weight: FontWeights.tokenFontWeight700)

    // Font 9
    public static let fontRegular9: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize9, weight: FontWeights.tokenFontWeight400)
    public static let fontMedium9: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize9, weight: FontWeights.tokenFontWeight500)
    public static let fontBold9: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize9, weight: FontWeights.tokenFontWeight700)
    
    // Font 8
    public static let fontRegular8: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize8, weight: FontWeights.tokenFontWeight400)
    public static let fontMedium8: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize8, weight: FontWeights.tokenFontWeight500)
    public static let fontBold8: UIFont = UIFont.systemFont(ofSize: FontSizes.tokenFontSize8, weight: FontWeights.tokenFontWeight700)
}
