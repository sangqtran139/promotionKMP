//
//  VDSThemeRegistry.swift
//  CoreUI
//
//  Internal theme registry — giữ cấu hình theme host truyền vào để các view trong
//  CoreUI/PromotionUI đọc lại khi render (applier null-safe ở Pha 2).
//
//  Token là UIColor?/CGFloat? — field nil nghĩa là "giữ default của SDK" (applier bỏ qua).
//  Registry là singleton thread-safe; PromotionSDKImpl ghi vào qua @_implementationOnly import CoreUI.
//

import UIKit

// MARK: - Tokens

/// Token cho `VDSButton`.
public struct VDSButtonThemeToken {
    public var backgroundColor: UIColor?
    public var textColor: UIColor?
    public var shadowColor: UIColor?
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

/// Token cho `VDSSearchTextField`.
public struct VDSSearchBarThemeToken {
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

/// Token cho item voucher trong list (link, badge "đã dùng", nút radio chọn).
public struct VDSListItemThemeToken {
    public var linkTextColor: UIColor?
    public var usedBadgeTextColor: UIColor?
    public var usedBadgeBackgroundColor: UIColor?
    public var radioSelectedColor: UIColor?
    public var radioUnselectedColor: UIColor?

    public init(linkTextColor: UIColor? = nil,
                usedBadgeTextColor: UIColor? = nil,
                usedBadgeBackgroundColor: UIColor? = nil,
                radioSelectedColor: UIColor? = nil,
                radioUnselectedColor: UIColor? = nil) {
        self.linkTextColor = linkTextColor
        self.usedBadgeTextColor = usedBadgeTextColor
        self.usedBadgeBackgroundColor = usedBadgeBackgroundColor
        self.radioSelectedColor = radioSelectedColor
        self.radioUnselectedColor = radioUnselectedColor
    }
}

/// Token cho tab dạng chip (`PromotionTabView` — vd "Tất cả / Sắp hết hạn").
public struct VDSTabChipThemeToken {
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

/// Token cho tab gạch chân (`UnderlinedSegmentControlView`).
public struct VDSTabUnderlineThemeToken {
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

/// Token cho badge giảm giá (`CouponValueView` / card ưu đãi).
public struct VDSDiscountBadgeThemeToken {
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

// MARK: - Config

/// Gom toàn bộ token. Field nil = giữ default SDK cho nhóm view đó.
public struct VDSThemeConfig {
    public var button: VDSButtonThemeToken?
    public var searchBar: VDSSearchBarThemeToken?
    public var listItem: VDSListItemThemeToken?
    public var tabChip: VDSTabChipThemeToken?
    public var tabUnderline: VDSTabUnderlineThemeToken?
    public var discountBadge: VDSDiscountBadgeThemeToken?

    public init(button: VDSButtonThemeToken? = nil,
                searchBar: VDSSearchBarThemeToken? = nil,
                listItem: VDSListItemThemeToken? = nil,
                tabChip: VDSTabChipThemeToken? = nil,
                tabUnderline: VDSTabUnderlineThemeToken? = nil,
                discountBadge: VDSDiscountBadgeThemeToken? = nil) {
        self.button = button
        self.searchBar = searchBar
        self.listItem = listItem
        self.tabChip = tabChip
        self.tabUnderline = tabUnderline
        self.discountBadge = discountBadge
    }
}

// MARK: - Registry

/// Singleton thread-safe giữ `VDSThemeConfig` hiện tại. Token chưa cấu hình → getter trả nil.
public final class VDSThemeRegistry {

    public static let shared = VDSThemeRegistry()

    private let lock = NSLock()
    private var config: VDSThemeConfig?

    private init() {}

    /// Ghi đè cấu hình theme. Truyền nil để reset về default SDK.
    public func configure(_ config: VDSThemeConfig?) {
        lock.lock(); defer { lock.unlock() }
        self.config = config
    }

    /// Reset theme về default SDK.
    public func clear() {
        configure(nil)
    }

    public var current: VDSThemeConfig? {
        lock.lock(); defer { lock.unlock() }
        return config
    }

    public func button() -> VDSButtonThemeToken? { current?.button }
    public func searchBar() -> VDSSearchBarThemeToken? { current?.searchBar }
    public func listItem() -> VDSListItemThemeToken? { current?.listItem }
    public func tabChip() -> VDSTabChipThemeToken? { current?.tabChip }
    public func tabUnderline() -> VDSTabUnderlineThemeToken? { current?.tabUnderline }
    public func discountBadge() -> VDSDiscountBadgeThemeToken? { current?.discountBadge }
}
