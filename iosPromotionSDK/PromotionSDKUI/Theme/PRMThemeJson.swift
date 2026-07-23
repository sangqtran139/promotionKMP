//
//  PRMThemeJson.swift
//  PRMSDK
//
//  Serialize PRMSDKTheme ↔ JSON. **Một định dạng dùng chung với Android**
//  (`PRMThemeJson.kt`): key nhóm `button`/`searchBar`…, màu hex `#AARRGGBB`.
//  Đối tác ship một file theme cho cả hai nền tảng.
//

import UIKit

/// Serialize `PRMSDKTheme` ↔ JSON. **Một định dạng dùng chung với Android** — cùng tên type
/// (`PRMThemeJson`), cùng hàm `toJson` / `fromJson`. Đối ứng `PRMThemeJson.kt`.
public enum PRMThemeJson {

    /// Serialize theme → JSON (hex `#AARRGGBB`, key nhóm `button`/`searchBar`…). "{}" nếu encode lỗi.
    public static func toJson(_ theme: PRMSDKTheme) -> String {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        guard let data = try? encoder.encode(ThemeDTO(theme)),
              let json = String(data: data, encoding: .utf8) else { return "{}" }
        return json
    }

    /// Parse theme từ JSON. `nil` nếu chuỗi không hợp lệ.
    public static func fromJson(_ json: String) -> PRMSDKTheme? {
        guard let data = json.data(using: .utf8),
              let dto = try? JSONDecoder().decode(ThemeDTO.self, from: data) else { return nil }
        return dto.toTheme()
    }
}

// MARK: - Codable DTO (nội bộ — màu ↔ hex)
//
// Key nhóm là `button`/`searchBar`/… chứ không phải `buttonToken`/… — đây là hình dạng JSON, không
// phải hình dạng API; nó khớp `ThemeDto` bên Kotlin và `PRMThemeDisplay` (model preview).

private struct ThemeDTO: Codable {
    var button: ButtonDTO?
    var searchBar: SearchBarDTO?
    var listItem: ListItemDTO?
    var tabChip: TabChipDTO?
    var tabUnderline: TabUnderlineDTO?
    var discountBadge: DiscountBadgeDTO?

    init(_ t: PRMSDKTheme) {
        button = t.buttonToken.map { ButtonDTO($0) }
        searchBar = t.searchBarToken.map { SearchBarDTO($0) }
        listItem = t.listItemToken.map { ListItemDTO($0) }
        tabChip = t.tabChipToken.map { TabChipDTO($0) }
        tabUnderline = t.tabUnderlineToken.map { TabUnderlineDTO($0) }
        discountBadge = t.discountBadgeToken.map { DiscountBadgeDTO($0) }
    }

    func toTheme() -> PRMSDKTheme {
        PRMSDKTheme(
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
        init(_ t: PRMButtonToken) {
            backgroundColor = ThemeHex.format(t.backgroundColor)
            textColor = ThemeHex.format(t.textColor)
            shadowColor = ThemeHex.format(t.shadowColor)
            cornerRadius = t.cornerRadius.map { Double($0) }
        }
        func toToken() -> PRMButtonToken {
            PRMButtonToken(
                backgroundColor: ThemeHex.parse(backgroundColor),
                textColor: ThemeHex.parse(textColor),
                shadowColor: ThemeHex.parse(shadowColor),
                cornerRadius: cornerRadius.map { CGFloat($0) }
            )
        }
    }

    struct SearchBarDTO: Codable {
        var borderColor, hintTextColor, textColor, iconColor: String?
        var cornerRadius: Double?
        init(_ t: PRMSearchBarToken) {
            borderColor = ThemeHex.format(t.borderColor)
            hintTextColor = ThemeHex.format(t.hintTextColor)
            textColor = ThemeHex.format(t.textColor)
            iconColor = ThemeHex.format(t.iconColor)
            cornerRadius = t.cornerRadius.map { Double($0) }
        }
        func toToken() -> PRMSearchBarToken {
            PRMSearchBarToken(
                borderColor: ThemeHex.parse(borderColor),
                hintTextColor: ThemeHex.parse(hintTextColor),
                textColor: ThemeHex.parse(textColor),
                iconColor: ThemeHex.parse(iconColor),
                cornerRadius: cornerRadius.map { CGFloat($0) }
            )
        }
    }

    struct ListItemDTO: Codable {
        var linkTextColor, usedBadgeTextColor, usedBadgeBackgroundColor: String?
        var radioButtonStrokeColor, radioButtonSelectedStrokeColor: String?
        init(_ t: PRMListItemToken) {
            linkTextColor = ThemeHex.format(t.linkTextColor)
            usedBadgeTextColor = ThemeHex.format(t.usedBadgeTextColor)
            usedBadgeBackgroundColor = ThemeHex.format(t.usedBadgeBackgroundColor)
            radioButtonStrokeColor = ThemeHex.format(t.radioButtonStrokeColor)
            radioButtonSelectedStrokeColor = ThemeHex.format(t.radioButtonSelectedStrokeColor)
        }
        func toToken() -> PRMListItemToken {
            PRMListItemToken(
                linkTextColor: ThemeHex.parse(linkTextColor),
                usedBadgeTextColor: ThemeHex.parse(usedBadgeTextColor),
                usedBadgeBackgroundColor: ThemeHex.parse(usedBadgeBackgroundColor),
                radioButtonStrokeColor: ThemeHex.parse(radioButtonStrokeColor),
                radioButtonSelectedStrokeColor: ThemeHex.parse(radioButtonSelectedStrokeColor)
            )
        }
    }

    struct TabChipDTO: Codable {
        var activeBackgroundColor, inactiveBackgroundColor, activeTextColor, inactiveTextColor: String?
        var cornerRadius: Double?
        init(_ t: PRMTabChipToken) {
            activeBackgroundColor = ThemeHex.format(t.activeBackgroundColor)
            inactiveBackgroundColor = ThemeHex.format(t.inactiveBackgroundColor)
            activeTextColor = ThemeHex.format(t.activeTextColor)
            inactiveTextColor = ThemeHex.format(t.inactiveTextColor)
            cornerRadius = t.cornerRadius.map { Double($0) }
        }
        func toToken() -> PRMTabChipToken {
            PRMTabChipToken(
                activeBackgroundColor: ThemeHex.parse(activeBackgroundColor),
                inactiveBackgroundColor: ThemeHex.parse(inactiveBackgroundColor),
                activeTextColor: ThemeHex.parse(activeTextColor),
                inactiveTextColor: ThemeHex.parse(inactiveTextColor),
                cornerRadius: cornerRadius.map { CGFloat($0) }
            )
        }
    }

    struct TabUnderlineDTO: Codable {
        var indicatorColor, activeTextColor, inactiveTextColor, backgroundColor: String?
        init(_ t: PRMTabUnderlineToken) {
            indicatorColor = ThemeHex.format(t.indicatorColor)
            activeTextColor = ThemeHex.format(t.activeTextColor)
            inactiveTextColor = ThemeHex.format(t.inactiveTextColor)
            backgroundColor = ThemeHex.format(t.backgroundColor)
        }
        func toToken() -> PRMTabUnderlineToken {
            PRMTabUnderlineToken(
                indicatorColor: ThemeHex.parse(indicatorColor),
                activeTextColor: ThemeHex.parse(activeTextColor),
                inactiveTextColor: ThemeHex.parse(inactiveTextColor),
                backgroundColor: ThemeHex.parse(backgroundColor)
            )
        }
    }

    struct DiscountBadgeDTO: Codable {
        var availableTextColor, unavailableTextColor: String?
        var availableBackgroundColor, unavailableBackgroundColor, actionTextColor: String?
        init(_ t: PRMDiscountBadgeToken) {
            availableTextColor = ThemeHex.format(t.availableTextColor)
            unavailableTextColor = ThemeHex.format(t.unavailableTextColor)
            availableBackgroundColor = ThemeHex.format(t.availableBackgroundColor)
            unavailableBackgroundColor = ThemeHex.format(t.unavailableBackgroundColor)
            actionTextColor = ThemeHex.format(t.actionTextColor)
        }
        func toToken() -> PRMDiscountBadgeToken {
            PRMDiscountBadgeToken(
                availableTextColor: ThemeHex.parse(availableTextColor),
                unavailableTextColor: ThemeHex.parse(unavailableTextColor),
                availableBackgroundColor: ThemeHex.parse(availableBackgroundColor),
                unavailableBackgroundColor: ThemeHex.parse(unavailableBackgroundColor),
                actionTextColor: ThemeHex.parse(actionTextColor)
            )
        }
    }
}
