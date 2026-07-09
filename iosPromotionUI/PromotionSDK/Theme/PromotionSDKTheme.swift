//
//  PromotionSDKTheme.swift
//  PromotionSDK
//
//  Public theming API. Chỉ dùng UIColor/CGFloat (Foundation/UIKit) để không kéo
//  CoreUI vào module interface công khai (xem ADR/0002-nsobject-box-public-facade).
//
//  Mọi field đều optional: nil = giữ default của SDK.
//  Cấu hình theo 2 cách: PromotionSDK(customerId:theme:) hoặc sdk.configure(theme:).
//

import UIKit

/// Token cho nút bấm chính của SDK.
public struct VDSPromotionButtonToken {
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

/// Token cho ô tìm kiếm.
public struct VDSPromotionSearchBarToken {
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

/// Token cho item voucher trong danh sách (link, badge "đã dùng").
///
/// Lưu ý: không có token cho nút radio — radio dùng ảnh asset cố định, không tint/đổi màu được
/// mà giữ đúng design (xem Theming.md §5).
public struct VDSPromotionListItemToken {
    public var linkTextColor: UIColor?
    public var usedBadgeTextColor: UIColor?
    public var usedBadgeBackgroundColor: UIColor?
    /// Màu nút radio khi item được chọn / chưa chọn (widget chọn ưu đãi). nil = giữ ảnh mặc định SDK.
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

/// Token cho tab dạng chip (vd "Tất cả / Sắp hết hạn" ở màn ưu đãi của tôi).
public struct VDSPromotionTabChipToken {
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

/// Token cho tab gạch chân (Ưu đãi của tôi / khác).
public struct VDSPromotionTabUnderlineToken {
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

/// Token cho badge giảm giá trên card ưu đãi.
public struct VDSPromotionDiscountBadgeToken {
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

/// Cấu hình theme tổng cho SDK. Nhóm nil = giữ toàn bộ default của SDK cho nhóm đó.
public struct PromotionSDKTheme {
    public var button: VDSPromotionButtonToken?
    public var searchBar: VDSPromotionSearchBarToken?
    public var listItem: VDSPromotionListItemToken?
    public var tabChip: VDSPromotionTabChipToken?
    public var tabUnderline: VDSPromotionTabUnderlineToken?
    public var discountBadge: VDSPromotionDiscountBadgeToken?

    public init(button: VDSPromotionButtonToken? = nil,
                searchBar: VDSPromotionSearchBarToken? = nil,
                listItem: VDSPromotionListItemToken? = nil,
                tabChip: VDSPromotionTabChipToken? = nil,
                tabUnderline: VDSPromotionTabUnderlineToken? = nil,
                discountBadge: VDSPromotionDiscountBadgeToken? = nil) {
        self.button = button
        self.searchBar = searchBar
        self.listItem = listItem
        self.tabChip = tabChip
        self.tabUnderline = tabUnderline
        self.discountBadge = discountBadge
    }
}

// MARK: - JSON serialize (mirror Android PromotionThemeJson)

public extension PromotionSDKTheme {

    /// Serialize theme → JSON string (màu lưu dạng hex `#RRGGBBAA`). Trả nil nếu lỗi.
    func jsonString() -> String? {
        let dto = ThemeDTO(self)
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        guard let data = try? encoder.encode(dto) else { return nil }
        return String(data: data, encoding: .utf8)
    }

    /// Parse theme từ JSON string. Trả nil nếu chuỗi không hợp lệ.
    static func from(jsonString: String) -> PromotionSDKTheme? {
        guard let data = jsonString.data(using: .utf8),
              let dto = try? JSONDecoder().decode(ThemeDTO.self, from: data) else { return nil }
        return dto.toTheme()
    }
}

// MARK: - Codable DTO (nội bộ — màu ↔ hex)

private struct ThemeDTO: Codable {
    var button: ButtonDTO?
    var searchBar: SearchBarDTO?
    var listItem: ListItemDTO?
    var tabChip: TabChipDTO?
    var tabUnderline: TabUnderlineDTO?
    var discountBadge: DiscountBadgeDTO?

    init(_ t: PromotionSDKTheme) {
        button = t.button.map { ButtonDTO($0) }
        searchBar = t.searchBar.map { SearchBarDTO($0) }
        listItem = t.listItem.map { ListItemDTO($0) }
        tabChip = t.tabChip.map { TabChipDTO($0) }
        tabUnderline = t.tabUnderline.map { TabUnderlineDTO($0) }
        discountBadge = t.discountBadge.map { DiscountBadgeDTO($0) }
    }

    func toTheme() -> PromotionSDKTheme {
        PromotionSDKTheme(
            button: button?.toToken(),
            searchBar: searchBar?.toToken(),
            listItem: listItem?.toToken(),
            tabChip: tabChip?.toToken(),
            tabUnderline: tabUnderline?.toToken(),
            discountBadge: discountBadge?.toToken()
        )
    }

    struct ButtonDTO: Codable {
        var backgroundColor, textColor, shadowColor: String?
        var cornerRadius: Double?
        init(_ t: VDSPromotionButtonToken) {
            backgroundColor = t.backgroundColor?.vdsHexString
            textColor = t.textColor?.vdsHexString
            shadowColor = t.shadowColor?.vdsHexString
            cornerRadius = t.cornerRadius.map { Double($0) }
        }
        func toToken() -> VDSPromotionButtonToken {
            VDSPromotionButtonToken(
                backgroundColor: UIColor(vdsHex: backgroundColor),
                textColor: UIColor(vdsHex: textColor),
                shadowColor: UIColor(vdsHex: shadowColor),
                cornerRadius: cornerRadius.map { CGFloat($0) }
            )
        }
    }

    struct SearchBarDTO: Codable {
        var borderColor, hintTextColor, textColor, iconColor: String?
        var cornerRadius: Double?
        init(_ t: VDSPromotionSearchBarToken) {
            borderColor = t.borderColor?.vdsHexString
            hintTextColor = t.hintTextColor?.vdsHexString
            textColor = t.textColor?.vdsHexString
            iconColor = t.iconColor?.vdsHexString
            cornerRadius = t.cornerRadius.map { Double($0) }
        }
        func toToken() -> VDSPromotionSearchBarToken {
            VDSPromotionSearchBarToken(
                borderColor: UIColor(vdsHex: borderColor),
                hintTextColor: UIColor(vdsHex: hintTextColor),
                textColor: UIColor(vdsHex: textColor),
                iconColor: UIColor(vdsHex: iconColor),
                cornerRadius: cornerRadius.map { CGFloat($0) }
            )
        }
    }

    struct ListItemDTO: Codable {
        var linkTextColor, usedBadgeTextColor, usedBadgeBackgroundColor, radioSelectedColor, radioUnselectedColor: String?
        init(_ t: VDSPromotionListItemToken) {
            linkTextColor = t.linkTextColor?.vdsHexString
            usedBadgeTextColor = t.usedBadgeTextColor?.vdsHexString
            usedBadgeBackgroundColor = t.usedBadgeBackgroundColor?.vdsHexString
            radioSelectedColor = t.radioSelectedColor?.vdsHexString
            radioUnselectedColor = t.radioUnselectedColor?.vdsHexString
        }
        func toToken() -> VDSPromotionListItemToken {
            VDSPromotionListItemToken(
                linkTextColor: UIColor(vdsHex: linkTextColor),
                usedBadgeTextColor: UIColor(vdsHex: usedBadgeTextColor),
                usedBadgeBackgroundColor: UIColor(vdsHex: usedBadgeBackgroundColor),
                radioSelectedColor: UIColor(vdsHex: radioSelectedColor),
                radioUnselectedColor: UIColor(vdsHex: radioUnselectedColor)
            )
        }
    }

    struct TabChipDTO: Codable {
        var activeBackgroundColor, inactiveBackgroundColor, activeTextColor, inactiveTextColor: String?
        var cornerRadius: Double?
        init(_ t: VDSPromotionTabChipToken) {
            activeBackgroundColor = t.activeBackgroundColor?.vdsHexString
            inactiveBackgroundColor = t.inactiveBackgroundColor?.vdsHexString
            activeTextColor = t.activeTextColor?.vdsHexString
            inactiveTextColor = t.inactiveTextColor?.vdsHexString
            cornerRadius = t.cornerRadius.map { Double($0) }
        }
        func toToken() -> VDSPromotionTabChipToken {
            VDSPromotionTabChipToken(
                activeBackgroundColor: UIColor(vdsHex: activeBackgroundColor),
                inactiveBackgroundColor: UIColor(vdsHex: inactiveBackgroundColor),
                activeTextColor: UIColor(vdsHex: activeTextColor),
                inactiveTextColor: UIColor(vdsHex: inactiveTextColor),
                cornerRadius: cornerRadius.map { CGFloat($0) }
            )
        }
    }

    struct TabUnderlineDTO: Codable {
        var indicatorColor, activeTextColor, inactiveTextColor, backgroundColor: String?
        init(_ t: VDSPromotionTabUnderlineToken) {
            indicatorColor = t.indicatorColor?.vdsHexString
            activeTextColor = t.activeTextColor?.vdsHexString
            inactiveTextColor = t.inactiveTextColor?.vdsHexString
            backgroundColor = t.backgroundColor?.vdsHexString
        }
        func toToken() -> VDSPromotionTabUnderlineToken {
            VDSPromotionTabUnderlineToken(
                indicatorColor: UIColor(vdsHex: indicatorColor),
                activeTextColor: UIColor(vdsHex: activeTextColor),
                inactiveTextColor: UIColor(vdsHex: inactiveTextColor),
                backgroundColor: UIColor(vdsHex: backgroundColor)
            )
        }
    }

    struct DiscountBadgeDTO: Codable {
        var availableTextColor, unavailableTextColor, availableBackgroundColor, unavailableBackgroundColor, actionTextColor: String?
        init(_ t: VDSPromotionDiscountBadgeToken) {
            availableTextColor = t.availableTextColor?.vdsHexString
            unavailableTextColor = t.unavailableTextColor?.vdsHexString
            availableBackgroundColor = t.availableBackgroundColor?.vdsHexString
            unavailableBackgroundColor = t.unavailableBackgroundColor?.vdsHexString
            actionTextColor = t.actionTextColor?.vdsHexString
        }
        func toToken() -> VDSPromotionDiscountBadgeToken {
            VDSPromotionDiscountBadgeToken(
                availableTextColor: UIColor(vdsHex: availableTextColor),
                unavailableTextColor: UIColor(vdsHex: unavailableTextColor),
                availableBackgroundColor: UIColor(vdsHex: availableBackgroundColor),
                unavailableBackgroundColor: UIColor(vdsHex: unavailableBackgroundColor),
                actionTextColor: UIColor(vdsHex: actionTextColor)
            )
        }
    }
}

// MARK: - UIColor ↔ hex #RRGGBBAA

private extension UIColor {
    /// "#RRGGBBAA" (giữ alpha).
    var vdsHexString: String {
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        getRed(&r, green: &g, blue: &b, alpha: &a)
        let clamp: (CGFloat) -> Int = { Int((max(0, min(1, $0)) * 255).rounded()) }
        return String(format: "#%02X%02X%02X%02X", clamp(r), clamp(g), clamp(b), clamp(a))
    }

    /// Parse "#RRGGBBAA" hoặc "#RRGGBB" (alpha mặc định FF). nil nếu chuỗi rỗng/sai.
    convenience init?(vdsHex hex: String?) {
        guard var s = hex, !s.isEmpty else { return nil }
        if s.hasPrefix("#") { s.removeFirst() }
        guard s.count == 6 || s.count == 8, let value = UInt64(s, radix: 16) else { return nil }
        let r, g, b, a: CGFloat
        if s.count == 8 {
            r = CGFloat((value & 0xFF000000) >> 24) / 255
            g = CGFloat((value & 0x00FF0000) >> 16) / 255
            b = CGFloat((value & 0x0000FF00) >> 8) / 255
            a = CGFloat(value & 0x000000FF) / 255
        } else {
            r = CGFloat((value & 0xFF0000) >> 16) / 255
            g = CGFloat((value & 0x00FF00) >> 8) / 255
            b = CGFloat(value & 0x0000FF) / 255
            a = 1
        }
        self.init(red: r, green: g, blue: b, alpha: a)
    }
}
