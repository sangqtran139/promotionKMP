//
//  PromotionSDKTheme.swift
//  PromotionSDK
//
//  Public theming API. Chỉ dùng UIColor/CGFloat (Foundation/UIKit) để không kéo
//  CoreUI vào module interface công khai (xem ADR/0002-nsobject-box-public-facade).
//
//  Đối ứng 1-1 với `PromotionSDKTheme.kt` + `token/*.kt` bên Android: cùng tên type, cùng tên field,
//  cùng ngữ nghĩa nil = "giữ default của SDK". Sửa một bên thì sửa cả hai.
//
//  Cấu hình theo 2 cách: PromotionSDK(customerId:theme:) hoặc sdk.configure(theme:).
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

/// Token cho tab gạch chân (Ưu đãi của tôi / khác).
public struct TabUnderlineToken {
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
public struct DiscountBadgeToken {
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
    public var buttonToken: ButtonToken?
    public var searchBarToken: SearchBarToken?
    public var listItemToken: ListItemToken?
    public var tabChipToken: TabChipToken?
    public var tabUnderlineToken: TabUnderlineToken?
    public var discountBadgeToken: DiscountBadgeToken?

    public init(buttonToken: ButtonToken? = nil,
                searchBarToken: SearchBarToken? = nil,
                listItemToken: ListItemToken? = nil,
                tabChipToken: TabChipToken? = nil,
                tabUnderlineToken: TabUnderlineToken? = nil,
                discountBadgeToken: DiscountBadgeToken? = nil) {
        self.buttonToken = buttonToken
        self.searchBarToken = searchBarToken
        self.listItemToken = listItemToken
        self.tabChipToken = tabChipToken
        self.tabUnderlineToken = tabUnderlineToken
        self.discountBadgeToken = discountBadgeToken
    }
}

// MARK: - JSON serialize

public extension PromotionSDKTheme {

    /// Serialize theme → JSON. **Một định dạng duy nhất, dùng chung với Android**
    /// (`PromotionThemeJson.kt`) — đối tác ship một file theme cho cả hai nền tảng. `nil` nếu lỗi.
    func jsonString() -> String? {
        let dto = ThemeDTO(self)
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        guard let data = try? encoder.encode(dto) else { return nil }
        return String(data: data, encoding: .utf8)
    }

    /// Parse theme từ JSON. `nil` nếu chuỗi không hợp lệ.
    static func from(jsonString: String) -> PromotionSDKTheme? {
        guard let data = jsonString.data(using: .utf8),
              let dto = try? JSONDecoder().decode(ThemeDTO.self, from: data) else { return nil }
        return dto.toTheme()
    }
}

// MARK: - Codable DTO (nội bộ — màu ↔ hex)
//
// Key nhóm là `button`/`searchBar`/… chứ không phải `buttonToken`/… — đây là hình dạng JSON, không
// phải hình dạng API; nó khớp `ThemeDto` bên Kotlin và `PromotionThemeDisplay` (model preview).

private struct ThemeDTO: Codable {
    var button: ButtonDTO?
    var searchBar: SearchBarDTO?
    var listItem: ListItemDTO?
    var tabChip: TabChipDTO?
    var tabUnderline: TabUnderlineDTO?
    var discountBadge: DiscountBadgeDTO?

    init(_ t: PromotionSDKTheme) {
        button = t.buttonToken.map { ButtonDTO($0) }
        searchBar = t.searchBarToken.map { SearchBarDTO($0) }
        listItem = t.listItemToken.map { ListItemDTO($0) }
        tabChip = t.tabChipToken.map { TabChipDTO($0) }
        tabUnderline = t.tabUnderlineToken.map { TabUnderlineDTO($0) }
        discountBadge = t.discountBadgeToken.map { DiscountBadgeDTO($0) }
    }

    func toTheme() -> PromotionSDKTheme {
        PromotionSDKTheme(
            buttonToken: button?.toToken(),
            searchBarToken: searchBar?.toToken(),
            listItemToken: listItem?.toToken(),
            tabChipToken: tabChip?.toToken(),
            tabUnderlineToken: tabUnderline?.toToken(),
            discountBadgeToken: discountBadge?.toToken()
        )
    }

    struct ButtonDTO: Codable {
        var backgroundColor, textColor, shadowColor: String?
        var cornerRadius: Double?
        init(_ t: ButtonToken) {
            backgroundColor = t.backgroundColor?.promotionHexString
            textColor = t.textColor?.promotionHexString
            shadowColor = t.shadowColor?.promotionHexString
            cornerRadius = t.cornerRadius.map { Double($0) }
        }
        func toToken() -> ButtonToken {
            ButtonToken(
                backgroundColor: UIColor(promotionHex: backgroundColor),
                textColor: UIColor(promotionHex: textColor),
                shadowColor: UIColor(promotionHex: shadowColor),
                cornerRadius: cornerRadius.map { CGFloat($0) }
            )
        }
    }

    struct SearchBarDTO: Codable {
        var borderColor, hintTextColor, textColor, iconColor: String?
        var cornerRadius: Double?
        init(_ t: SearchBarToken) {
            borderColor = t.borderColor?.promotionHexString
            hintTextColor = t.hintTextColor?.promotionHexString
            textColor = t.textColor?.promotionHexString
            iconColor = t.iconColor?.promotionHexString
            cornerRadius = t.cornerRadius.map { Double($0) }
        }
        func toToken() -> SearchBarToken {
            SearchBarToken(
                borderColor: UIColor(promotionHex: borderColor),
                hintTextColor: UIColor(promotionHex: hintTextColor),
                textColor: UIColor(promotionHex: textColor),
                iconColor: UIColor(promotionHex: iconColor),
                cornerRadius: cornerRadius.map { CGFloat($0) }
            )
        }
    }

    struct ListItemDTO: Codable {
        var linkTextColor, usedBadgeTextColor, usedBadgeBackgroundColor: String?
        var radioButtonStrokeColor, radioButtonSelectedStrokeColor: String?
        init(_ t: ListItemToken) {
            linkTextColor = t.linkTextColor?.promotionHexString
            usedBadgeTextColor = t.usedBadgeTextColor?.promotionHexString
            usedBadgeBackgroundColor = t.usedBadgeBackgroundColor?.promotionHexString
            radioButtonStrokeColor = t.radioButtonStrokeColor?.promotionHexString
            radioButtonSelectedStrokeColor = t.radioButtonSelectedStrokeColor?.promotionHexString
        }
        func toToken() -> ListItemToken {
            ListItemToken(
                linkTextColor: UIColor(promotionHex: linkTextColor),
                usedBadgeTextColor: UIColor(promotionHex: usedBadgeTextColor),
                usedBadgeBackgroundColor: UIColor(promotionHex: usedBadgeBackgroundColor),
                radioButtonStrokeColor: UIColor(promotionHex: radioButtonStrokeColor),
                radioButtonSelectedStrokeColor: UIColor(promotionHex: radioButtonSelectedStrokeColor)
            )
        }
    }

    struct TabChipDTO: Codable {
        var activeBackgroundColor, inactiveBackgroundColor, activeTextColor, inactiveTextColor: String?
        var cornerRadius: Double?
        init(_ t: TabChipToken) {
            activeBackgroundColor = t.activeBackgroundColor?.promotionHexString
            inactiveBackgroundColor = t.inactiveBackgroundColor?.promotionHexString
            activeTextColor = t.activeTextColor?.promotionHexString
            inactiveTextColor = t.inactiveTextColor?.promotionHexString
            cornerRadius = t.cornerRadius.map { Double($0) }
        }
        func toToken() -> TabChipToken {
            TabChipToken(
                activeBackgroundColor: UIColor(promotionHex: activeBackgroundColor),
                inactiveBackgroundColor: UIColor(promotionHex: inactiveBackgroundColor),
                activeTextColor: UIColor(promotionHex: activeTextColor),
                inactiveTextColor: UIColor(promotionHex: inactiveTextColor),
                cornerRadius: cornerRadius.map { CGFloat($0) }
            )
        }
    }

    struct TabUnderlineDTO: Codable {
        var indicatorColor, activeTextColor, inactiveTextColor, backgroundColor: String?
        init(_ t: TabUnderlineToken) {
            indicatorColor = t.indicatorColor?.promotionHexString
            activeTextColor = t.activeTextColor?.promotionHexString
            inactiveTextColor = t.inactiveTextColor?.promotionHexString
            backgroundColor = t.backgroundColor?.promotionHexString
        }
        func toToken() -> TabUnderlineToken {
            TabUnderlineToken(
                indicatorColor: UIColor(promotionHex: indicatorColor),
                activeTextColor: UIColor(promotionHex: activeTextColor),
                inactiveTextColor: UIColor(promotionHex: inactiveTextColor),
                backgroundColor: UIColor(promotionHex: backgroundColor)
            )
        }
    }

    struct DiscountBadgeDTO: Codable {
        var availableTextColor, unavailableTextColor: String?
        var availableBackgroundColor, unavailableBackgroundColor, actionTextColor: String?
        init(_ t: DiscountBadgeToken) {
            availableTextColor = t.availableTextColor?.promotionHexString
            unavailableTextColor = t.unavailableTextColor?.promotionHexString
            availableBackgroundColor = t.availableBackgroundColor?.promotionHexString
            unavailableBackgroundColor = t.unavailableBackgroundColor?.promotionHexString
            actionTextColor = t.actionTextColor?.promotionHexString
        }
        func toToken() -> DiscountBadgeToken {
            DiscountBadgeToken(
                availableTextColor: UIColor(promotionHex: availableTextColor),
                unavailableTextColor: UIColor(promotionHex: unavailableTextColor),
                availableBackgroundColor: UIColor(promotionHex: availableBackgroundColor),
                unavailableBackgroundColor: UIColor(promotionHex: unavailableBackgroundColor),
                actionTextColor: UIColor(promotionHex: actionTextColor)
            )
        }
    }
}

// MARK: - UIColor ↔ hex #AARRGGBB
//
// Alpha đứng **trước**, theo quy ước của Android (`Color.parseColor`) — không phải CSS. Bản trước
// đây ghi `#RRGGBBAA`, nên `#EE0033FF` (đỏ đục) đọc sang Android thành `alpha=EE, b=FF`: một màu
// xanh mờ, sai lặng lẽ không lỗi. Đối ứng `ThemeHex.kt`.

extension UIColor {
    /// `#AARRGGBB`, rút gọn `#RRGGBB` khi màu đục.
    var promotionHexString: String {
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        getRed(&r, green: &g, blue: &b, alpha: &a)
        let clamp: (CGFloat) -> Int = { Int((max(0, min(1, $0)) * 255).rounded()) }
        let alpha = clamp(a)
        if alpha == 0xFF {
            return String(format: "#%02X%02X%02X", clamp(r), clamp(g), clamp(b))
        }
        return String(format: "#%02X%02X%02X%02X", alpha, clamp(r), clamp(g), clamp(b))
    }

    /// Parse `#AARRGGBB` hoặc `#RRGGBB` (coi là đục). `nil` nếu chuỗi rỗng/sai.
    convenience init?(promotionHex hex: String?) {
        guard var s = hex?.trimmingCharacters(in: .whitespaces), !s.isEmpty else { return nil }
        if s.hasPrefix("#") { s.removeFirst() }
        guard s.count == 6 || s.count == 8, let value = UInt64(s, radix: 16) else { return nil }
        let r, g, b, a: CGFloat
        if s.count == 8 {
            a = CGFloat((value & 0xFF000000) >> 24) / 255
            r = CGFloat((value & 0x00FF0000) >> 16) / 255
            g = CGFloat((value & 0x0000FF00) >> 8) / 255
            b = CGFloat(value & 0x000000FF) / 255
        } else {
            r = CGFloat((value & 0xFF0000) >> 16) / 255
            g = CGFloat((value & 0x00FF00) >> 8) / 255
            b = CGFloat(value & 0x0000FF) / 255
            a = 1
        }
        self.init(red: r, green: g, blue: b, alpha: a)
    }
}
