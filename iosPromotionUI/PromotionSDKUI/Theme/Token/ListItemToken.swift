//
//  ListItemToken.swift
//  PromotionSDK
//
//  Token theme. Đối ứng `token/ListItemToken.kt` bên Android — cùng field, cùng thứ tự.
//

import UIKit

/// Token cho item voucher trong danh sách (link, badge "đã dùng", nút radio chọn).
public struct ListItemToken {
    public var linkTextColor: UIColor?
    public var usedBadgeTextColor: UIColor?
    public var usedBadgeBackgroundColor: UIColor?
    /// Viền nút radio khi **chưa** chọn.
    public var radioButtonStrokeColor: UIColor?
    /// Ruột nút radio khi **đã** chọn. Tên mang chữ "Stroke" vì lịch sử bên Android; nó được áp làm
    /// màu **fill** (xem `PromotionListItemApplier.kt`). Giữ tên để hai nền tảng không lệch.
    public var radioButtonSelectedStrokeColor: UIColor?

    public init(linkTextColor: UIColor? = nil,
                usedBadgeTextColor: UIColor? = nil,
                usedBadgeBackgroundColor: UIColor? = nil,
                radioButtonStrokeColor: UIColor? = nil,
                radioButtonSelectedStrokeColor: UIColor? = nil) {
        self.linkTextColor = linkTextColor
        self.usedBadgeTextColor = usedBadgeTextColor
        self.usedBadgeBackgroundColor = usedBadgeBackgroundColor
        self.radioButtonStrokeColor = radioButtonStrokeColor
        self.radioButtonSelectedStrokeColor = radioButtonSelectedStrokeColor
    }
}
