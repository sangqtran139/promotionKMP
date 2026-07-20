//
//  ButtonToken.swift
//  PromotionSDK
//
//  Token theme. Đối ứng `token/ButtonToken.kt` bên Android — cùng field, cùng thứ tự.
//

import UIKit


/// Token cho nút bấm chính của SDK.
public struct ButtonToken {
    public var backgroundColor: UIColor?
    public var textColor: UIColor?
    public var shadowColor: UIColor?
    /// Bo góc, đơn vị **pt** (Android dùng dp — cùng con số trong JSON).
    public var cornerRadius: CGFloat?

    public init(backgroundColor: UIColor? = nil,
                textColor: UIColor? = nil,
                shadowColor: UIColor? = nil,
                cornerRadius: CGFloat? = nil) {
        self.backgroundColor = backgroundColor
        self.textColor = textColor
        self.shadowColor = shadowColor
        self.cornerRadius = cornerRadius
    }
}
