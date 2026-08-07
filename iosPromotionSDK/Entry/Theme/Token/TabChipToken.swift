//
//  TabChipToken.swift
//  PromotionSDK
//
//  Token theme. Đối ứng `token/TabChipToken.kt` bên Android — cùng field, cùng thứ tự.
//

import UIKit

/// Token cho tab dạng chip (vd "Tất cả / Sắp hết hạn" ở màn ưu đãi của tôi).
public struct TabChipToken {
    public var activeBackgroundColor: UIColor?
    public var inactiveBackgroundColor: UIColor?
    public var activeTextColor: UIColor?
    public var inactiveTextColor: UIColor?
    public var cornerRadius: CGFloat?

    public init(activeBackgroundColor: UIColor? = nil,
                inactiveBackgroundColor: UIColor? = nil,
                activeTextColor: UIColor? = nil,
                inactiveTextColor: UIColor? = nil,
                cornerRadius: CGFloat? = nil) {
        self.activeBackgroundColor = activeBackgroundColor
        self.inactiveBackgroundColor = inactiveBackgroundColor
        self.activeTextColor = activeTextColor
        self.inactiveTextColor = inactiveTextColor
        self.cornerRadius = cornerRadius
    }
}
