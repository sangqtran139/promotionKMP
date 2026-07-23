//
//  PRMTabUnderlineToken.swift
//  PRMSDK
//
//  Token theme. Đối ứng `token/PRMTabUnderlineToken.kt` bên Android — cùng field, cùng thứ tự.
//

import UIKit

/// Token cho tab gạch chân (Ưu đãi của tôi / khác).
public struct PRMTabUnderlineToken {
    public var indicatorColor: UIColor?
    public var activeTextColor: UIColor?
    public var inactiveTextColor: UIColor?
    public var backgroundColor: UIColor?

    public init(indicatorColor: UIColor? = nil,
                activeTextColor: UIColor? = nil,
                inactiveTextColor: UIColor? = nil,
                backgroundColor: UIColor? = nil) {
        self.indicatorColor = indicatorColor
        self.activeTextColor = activeTextColor
        self.inactiveTextColor = inactiveTextColor
        self.backgroundColor = backgroundColor
    }
}
