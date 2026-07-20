//
//  SearchBarToken.swift
//  PromotionSDK
//
//  Token theme. Đối ứng `token/SearchBarToken.kt` bên Android — cùng field, cùng thứ tự.
//

import UIKit

/// Token cho ô tìm kiếm.
public struct SearchBarToken {
    public var borderColor: UIColor?
    public var hintTextColor: UIColor?
    public var textColor: UIColor?
    public var iconColor: UIColor?
    public var cornerRadius: CGFloat?

    public init(borderColor: UIColor? = nil,
                hintTextColor: UIColor? = nil,
                textColor: UIColor? = nil,
                iconColor: UIColor? = nil,
                cornerRadius: CGFloat? = nil) {
        self.borderColor = borderColor
        self.hintTextColor = hintTextColor
        self.textColor = textColor
        self.iconColor = iconColor
        self.cornerRadius = cornerRadius
    }
}
