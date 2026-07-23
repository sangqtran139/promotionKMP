//
//  PRMDiscountBadgeToken.swift
//  PRMSDK
//
//  Token theme. Đối ứng `token/PRMDiscountBadgeToken.kt` bên Android — cùng field, cùng thứ tự.
//

import UIKit

/// Token cho badge giảm giá trên card ưu đãi.
public struct PRMDiscountBadgeToken {
    public var availableTextColor: UIColor?
    public var unavailableTextColor: UIColor?
    public var availableBackgroundColor: UIColor?
    public var unavailableBackgroundColor: UIColor?
    public var actionTextColor: UIColor?

    public init(availableTextColor: UIColor? = nil,
                unavailableTextColor: UIColor? = nil,
                availableBackgroundColor: UIColor? = nil,
                unavailableBackgroundColor: UIColor? = nil,
                actionTextColor: UIColor? = nil) {
        self.availableTextColor = availableTextColor
        self.unavailableTextColor = unavailableTextColor
        self.availableBackgroundColor = availableBackgroundColor
        self.unavailableBackgroundColor = unavailableBackgroundColor
        self.actionTextColor = actionTextColor
    }
}
