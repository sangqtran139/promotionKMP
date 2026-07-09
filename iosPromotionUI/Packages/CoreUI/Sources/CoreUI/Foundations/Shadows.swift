//
//  Shadows.swift
//  CoreUIKit
//
//  Created by Natariannn on 8/7/20.
//  Copyright © 2020 ViettelPay App Team. All rights reserved.
//

import UIKit

public class Shadow {
    public var color: UIColor?
    public var offset: CGSize
    public var opacity: Float
    public var spread: CGFloat
    
    public init(color: UIColor?, offset: CGSize, opacity: Float, spread: CGFloat) {
        self.color = color
        self.offset = offset
        self.opacity = opacity
        self.spread = spread
    }
}

public enum Shadows {
    
    // Shadow Opacity
    public static let tokenShadowOpacity10: Float = 0.1
    public static let tokenShadowOpacity30: Float = 0.3
    
    // Shadow Offset
    public static let tokenShadowOffset08: CGSize = CGSize(width: 0.0, height: 8.0)
    public static let tokenShadowOffsetMinus04: CGSize = CGSize(width: 0.0, height: -4.0)
    
    // Shadow Tokens
    public static let tokenShadowNormal = Shadow(color: Colors.tokenBlack,
                                                 offset: .zero,
                                                 opacity: Float(Opacity.tokenOpacity25),
                                                 spread: .zero)
    public static let tokenShadowCard = Shadow(color: Colors.tokenBlack,
                                               offset: CGSize(width: 0, height: Sizing.tokenSizing04),
                                               opacity: Float(Opacity.tokenOpacity08),
                                               spread: .zero)
    public static let tokenShadowBottomTab = Shadow(color: Colors.tokenBlack,
                                                    offset: CGSize(width: 0, height: -Sizing.tokenSizing04),
                                                    opacity: Float(Opacity.tokenOpacity08),
                                                    spread: .zero)
    public static let tokenShadowButtonLarge = Shadow(color: Colors.tokenViettelPayRed100,
                                                      offset: CGSize(width: 0, height: Sizing.tokenSizing06),
                                                      opacity: Float(Opacity.tokenOpacity25),
                                                      spread: .zero)
    public static let tokenShadowButtonMedium = Shadow(color: Colors.tokenViettelPayRed100,
                                                       offset: CGSize(width: 0, height: Sizing.tokenSizing04),
                                                       opacity: Float(Opacity.tokenOpacity25),
                                                       spread: .zero)
    public static let tokenShadowButtonSmall = Shadow(color: Colors.tokenViettelPayRed100,
                                                      offset: CGSize(width: 0, height: Sizing.tokenSizing02),
                                                      opacity: Float(Opacity.tokenOpacity25),
                                                      spread: .zero)
    public static let tokenShadowDropDown = Shadow(color: Colors.tokenBlack,
                                                   offset: CGSize(width: 0, height: Sizing.tokenSizing04),
                                                   opacity: Float(Opacity.tokenOpacity08),
                                                   spread: .zero)
    public static let tokenShadowVoucher = Shadow(color: Colors.tokenBlack,
                                                 offset: .zero,
                                                 opacity: Float(Opacity.tokenOpacity02),
                                                 spread: .zero)
}
