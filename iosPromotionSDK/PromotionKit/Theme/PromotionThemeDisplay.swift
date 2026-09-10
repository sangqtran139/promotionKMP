//
//  PromotionThemeDisplay.swift
//  PromotionSDK
//
//  Biểu diễn token dạng **chuỗi hex**, phục vụ màn cấu hình theme của host.
//  Đối ứng 1-1 với `PromotionThemeDisplay.kt` bên Android: cùng tên type, cùng tên field,
//  cùng `load` / `mergeWithSaved` / `themeFromDisplayValues` / `toToken`.
//
//  Khác `PromotionSDKTheme`: ở đây **nhóm luôn có mặt**, chỉ leaf mới nullable — vì màn preview cần
//  hiển thị giá trị mặc định của SDK cho từng ô. Còn `PromotionSDKTheme` để nhóm nullable, vì
//  "nhóm nil" nghĩa là "đừng động vào nhóm đó".
//
//  Tên field ở đây (`button`, `searchBar`, …) cũng chính là **key của JSON theme** — xem `ThemeDTO`.
//

import UIKit

public enum PromotionThemeDisplay {

    public struct Defaults: Equatable {
        public var button: ButtonValues
        public var searchBar: SearchBarValues
        public var listItem: ListItemValues
        public var tabChip: TabChipValues
        public var tabUnderline: TabUnderlineValues
        public var discountBadge: DiscountBadgeValues

        public init(button: ButtonValues = .init(),
                    searchBar: SearchBarValues = .init(),
                    listItem: ListItemValues = .init(),
                    tabChip: TabChipValues = .init(),
                    tabUnderline: TabUnderlineValues = .init(),
                    discountBadge: DiscountBadgeValues = .init()) {
            self.button = button
            self.searchBar = searchBar
            self.listItem = listItem
            self.tabChip = tabChip
            self.tabUnderline = tabUnderline
            self.discountBadge = discountBadge
        }
    }

    public struct ButtonValues: Equatable {
        public var backgroundColor: String?
        public var textColor: String?
        public var shadowColor: String?
        public var cornerRadius: CGFloat?

        public init(backgroundColor: String? = nil, textColor: String? = nil,
                    shadowColor: String? = nil, cornerRadius: CGFloat? = nil) {
            self.backgroundColor = backgroundColor
            self.textColor = textColor
            self.shadowColor = shadowColor
            self.cornerRadius = cornerRadius
        }
    }

    public struct SearchBarValues: Equatable {
        public var borderColor: String?
        public var hintTextColor: String?
        public var textColor: String?
        public var iconColor: String?
        public var cornerRadius: CGFloat?

        public init(borderColor: String? = nil, hintTextColor: String? = nil,
                    textColor: String? = nil, iconColor: String? = nil,
                    cornerRadius: CGFloat? = nil) {
            self.borderColor = borderColor
            self.hintTextColor = hintTextColor
            self.textColor = textColor
            self.iconColor = iconColor
            self.cornerRadius = cornerRadius
        }
    }

    public struct ListItemValues: Equatable {
        public var linkTextColor: String?
        public var usedBadgeTextColor: String?
        public var usedBadgeBackgroundColor: String?
        public var radioButtonStrokeColor: String?
        public var radioButtonSelectedStrokeColor: String?

        public init(linkTextColor: String? = nil, usedBadgeTextColor: String? = nil,
                    usedBadgeBackgroundColor: String? = nil, radioButtonStrokeColor: String? = nil,
                    radioButtonSelectedStrokeColor: String? = nil) {
            self.linkTextColor = linkTextColor
            self.usedBadgeTextColor = usedBadgeTextColor
            self.usedBadgeBackgroundColor = usedBadgeBackgroundColor
            self.radioButtonStrokeColor = radioButtonStrokeColor
            self.radioButtonSelectedStrokeColor = radioButtonSelectedStrokeColor
        }
    }

    public struct TabChipValues: Equatable {
        public var activeBackgroundColor: String?
        public var inactiveBackgroundColor: String?
        public var activeTextColor: String?
        public var inactiveTextColor: String?
        public var cornerRadius: CGFloat?

        public init(activeBackgroundColor: String? = nil, inactiveBackgroundColor: String? = nil,
                    activeTextColor: String? = nil, inactiveTextColor: String? = nil,
                    cornerRadius: CGFloat? = nil) {
            self.activeBackgroundColor = activeBackgroundColor
            self.inactiveBackgroundColor = inactiveBackgroundColor
            self.activeTextColor = activeTextColor
            self.inactiveTextColor = inactiveTextColor
            self.cornerRadius = cornerRadius
        }
    }

    public struct TabUnderlineValues: Equatable {
        public var indicatorColor: String?
        public var activeTextColor: String?
        public var inactiveTextColor: String?
        public var backgroundColor: String?

        public init(indicatorColor: String? = nil, activeTextColor: String? = nil,
                    inactiveTextColor: String? = nil, backgroundColor: String? = nil) {
            self.indicatorColor = indicatorColor
            self.activeTextColor = activeTextColor
            self.inactiveTextColor = inactiveTextColor
            self.backgroundColor = backgroundColor
        }
    }

    public struct DiscountBadgeValues: Equatable {
        public var availableTextColor: String?
        public var unavailableTextColor: String?
        public var availableBackgroundColor: String?
        public var unavailableBackgroundColor: String?
        public var actionTextColor: String?

        public init(availableTextColor: String? = nil, unavailableTextColor: String? = nil,
                    availableBackgroundColor: String? = nil, unavailableBackgroundColor: String? = nil,
                    actionTextColor: String? = nil) {
            self.availableTextColor = availableTextColor
            self.unavailableTextColor = unavailableTextColor
            self.availableBackgroundColor = availableBackgroundColor
            self.unavailableBackgroundColor = unavailableBackgroundColor
            self.actionTextColor = actionTextColor
        }
    }

    // MARK: - API

    /// Giá trị mặc định của SDK ở dạng hex. Đối ứng `PromotionThemeDisplay.load(context)`.
    public static func load() -> Defaults { fromTheme(PromotionThemeDefaults.theme) }

    /// Phủ theme đã lưu lên default: field nào `saved` có thì thắng.
    public static func mergeWithSaved(sdk: Defaults, saved: PromotionSDKTheme?) -> Defaults {
        guard let saved else { return sdk }
        let s = fromTheme(saved)
        return Defaults(
            button: sdk.button.merged(with: s.button),
            searchBar: sdk.searchBar.merged(with: s.searchBar),
            listItem: sdk.listItem.merged(with: s.listItem),
            tabChip: sdk.tabChip.merged(with: s.tabChip),
            tabUnderline: sdk.tabUnderline.merged(with: s.tabUnderline),
            discountBadge: sdk.discountBadge.merged(with: s.discountBadge)
        )
    }

    /// Nhóm nào **không đổi** so với default SDK thì trả `nil` — đừng ghi đè thứ SDK tự lo.
    public static func themeFromDisplayValues(_ display: Defaults, sdk: Defaults) -> PromotionSDKTheme {
        PromotionSDKTheme(
            buttonToken: display.button == sdk.button ? nil : display.button.toToken(),
            searchBarToken: display.searchBar == sdk.searchBar ? nil : display.searchBar.toToken(),
            listItemToken: display.listItem == sdk.listItem ? nil : display.listItem.toToken(),
            tabChipToken: display.tabChip == sdk.tabChip ? nil : display.tabChip.toToken(),
            tabUnderlineToken: resolveTabUnderline(display.tabUnderline, sdk: sdk.tabUnderline),
            discountBadgeToken: display.discountBadge == sdk.discountBadge ? nil : display.discountBadge.toToken()
        )
    }

    /// Tab gạch chân lấp field trống bằng default: `UnderlinedSegmentControlView` đọc `backgroundColor`
    /// **rời** khỏi item, nên token nửa vời sẽ làm thanh tab mất nền. Khớp `resolveTabUnderlineToken`
    /// bên Android.
    private static func resolveTabUnderline(_ display: TabUnderlineValues,
                                            sdk: TabUnderlineValues) -> PRMTabUnderlineToken? {
        if display == sdk { return nil }
        let d = display.toToken()
        let s = sdk.toToken()
        return PRMTabUnderlineToken(
            indicatorColor: d.indicatorColor ?? s.indicatorColor,
            activeTextColor: d.activeTextColor ?? s.activeTextColor,
            inactiveTextColor: d.inactiveTextColor ?? s.inactiveTextColor,
            backgroundColor: d.backgroundColor ?? s.backgroundColor
        )
    }

    private static func fromTheme(_ t: PromotionSDKTheme) -> Defaults {
        func hex(_ c: UIColor?) -> String? { ThemeHex.format(c) }
        return Defaults(
            button: ButtonValues(
                backgroundColor: hex(t.buttonToken?.backgroundColor),
                textColor: hex(t.buttonToken?.textColor),
                shadowColor: hex(t.buttonToken?.shadowColor),
                cornerRadius: t.buttonToken?.cornerRadius
            ),
            searchBar: SearchBarValues(
                borderColor: hex(t.searchBarToken?.borderColor),
                hintTextColor: hex(t.searchBarToken?.hintTextColor),
                textColor: hex(t.searchBarToken?.textColor),
                iconColor: hex(t.searchBarToken?.iconColor),
                cornerRadius: t.searchBarToken?.cornerRadius
            ),
            listItem: ListItemValues(
                linkTextColor: hex(t.listItemToken?.linkTextColor),
                usedBadgeTextColor: hex(t.listItemToken?.usedBadgeTextColor),
                usedBadgeBackgroundColor: hex(t.listItemToken?.usedBadgeBackgroundColor),
                radioButtonStrokeColor: hex(t.listItemToken?.radioButtonStrokeColor),
                radioButtonSelectedStrokeColor: hex(t.listItemToken?.radioButtonSelectedStrokeColor)
            ),
            tabChip: TabChipValues(
                activeBackgroundColor: hex(t.tabChipToken?.activeBackgroundColor),
                inactiveBackgroundColor: hex(t.tabChipToken?.inactiveBackgroundColor),
                activeTextColor: hex(t.tabChipToken?.activeTextColor),
                inactiveTextColor: hex(t.tabChipToken?.inactiveTextColor),
                cornerRadius: t.tabChipToken?.cornerRadius
            ),
            tabUnderline: TabUnderlineValues(
                indicatorColor: hex(t.tabUnderlineToken?.indicatorColor),
                activeTextColor: hex(t.tabUnderlineToken?.activeTextColor),
                inactiveTextColor: hex(t.tabUnderlineToken?.inactiveTextColor),
                backgroundColor: hex(t.tabUnderlineToken?.backgroundColor)
            ),
            discountBadge: DiscountBadgeValues(
                availableTextColor: hex(t.discountBadgeToken?.availableTextColor),
                unavailableTextColor: hex(t.discountBadgeToken?.unavailableTextColor),
                availableBackgroundColor: hex(t.discountBadgeToken?.availableBackgroundColor),
                unavailableBackgroundColor: hex(t.discountBadgeToken?.unavailableBackgroundColor),
                actionTextColor: hex(t.discountBadgeToken?.actionTextColor)
            )
        )
    }
}

// MARK: - merge (giá trị của `other` thắng)

private extension PromotionThemeDisplay.ButtonValues {
    func merged(with o: Self) -> Self {
        .init(backgroundColor: o.backgroundColor ?? backgroundColor,
              textColor: o.textColor ?? textColor,
              shadowColor: o.shadowColor ?? shadowColor,
              cornerRadius: o.cornerRadius ?? cornerRadius)
    }
}

private extension PromotionThemeDisplay.SearchBarValues {
    func merged(with o: Self) -> Self {
        .init(borderColor: o.borderColor ?? borderColor,
              hintTextColor: o.hintTextColor ?? hintTextColor,
              textColor: o.textColor ?? textColor,
              iconColor: o.iconColor ?? iconColor,
              cornerRadius: o.cornerRadius ?? cornerRadius)
    }
}

private extension PromotionThemeDisplay.ListItemValues {
    func merged(with o: Self) -> Self {
        .init(linkTextColor: o.linkTextColor ?? linkTextColor,
              usedBadgeTextColor: o.usedBadgeTextColor ?? usedBadgeTextColor,
              usedBadgeBackgroundColor: o.usedBadgeBackgroundColor ?? usedBadgeBackgroundColor,
              radioButtonStrokeColor: o.radioButtonStrokeColor ?? radioButtonStrokeColor,
              radioButtonSelectedStrokeColor: o.radioButtonSelectedStrokeColor ?? radioButtonSelectedStrokeColor)
    }
}

private extension PromotionThemeDisplay.TabChipValues {
    func merged(with o: Self) -> Self {
        .init(activeBackgroundColor: o.activeBackgroundColor ?? activeBackgroundColor,
              inactiveBackgroundColor: o.inactiveBackgroundColor ?? inactiveBackgroundColor,
              activeTextColor: o.activeTextColor ?? activeTextColor,
              inactiveTextColor: o.inactiveTextColor ?? inactiveTextColor,
              cornerRadius: o.cornerRadius ?? cornerRadius)
    }
}

private extension PromotionThemeDisplay.TabUnderlineValues {
    func merged(with o: Self) -> Self {
        .init(indicatorColor: o.indicatorColor ?? indicatorColor,
              activeTextColor: o.activeTextColor ?? activeTextColor,
              inactiveTextColor: o.inactiveTextColor ?? inactiveTextColor,
              backgroundColor: o.backgroundColor ?? backgroundColor)
    }
}

private extension PromotionThemeDisplay.DiscountBadgeValues {
    func merged(with o: Self) -> Self {
        .init(availableTextColor: o.availableTextColor ?? availableTextColor,
              unavailableTextColor: o.unavailableTextColor ?? unavailableTextColor,
              availableBackgroundColor: o.availableBackgroundColor ?? availableBackgroundColor,
              unavailableBackgroundColor: o.unavailableBackgroundColor ?? unavailableBackgroundColor,
              actionTextColor: o.actionTextColor ?? actionTextColor)
    }
}

// MARK: - toToken (hex → UIColor)

public extension PromotionThemeDisplay.ButtonValues {
    func toToken() -> PRMButtonToken {
        PRMButtonToken(backgroundColor: ThemeHex.parse(backgroundColor),
                    textColor: ThemeHex.parse(textColor),
                    shadowColor: ThemeHex.parse(shadowColor),
                    cornerRadius: cornerRadius)
    }
}

public extension PromotionThemeDisplay.SearchBarValues {
    func toToken() -> PRMSearchBarToken {
        PRMSearchBarToken(borderColor: ThemeHex.parse(borderColor),
                       hintTextColor: ThemeHex.parse(hintTextColor),
                       textColor: ThemeHex.parse(textColor),
                       iconColor: ThemeHex.parse(iconColor),
                       cornerRadius: cornerRadius)
    }
}

public extension PromotionThemeDisplay.ListItemValues {
    func toToken() -> PRMListItemToken {
        PRMListItemToken(linkTextColor: ThemeHex.parse(linkTextColor),
                      usedBadgeTextColor: ThemeHex.parse(usedBadgeTextColor),
                      usedBadgeBackgroundColor: ThemeHex.parse(usedBadgeBackgroundColor),
                      radioButtonStrokeColor: ThemeHex.parse(radioButtonStrokeColor),
                      radioButtonSelectedStrokeColor: ThemeHex.parse(radioButtonSelectedStrokeColor))
    }
}

public extension PromotionThemeDisplay.TabChipValues {
    func toToken() -> PRMTabChipToken {
        PRMTabChipToken(activeBackgroundColor: ThemeHex.parse(activeBackgroundColor),
                     inactiveBackgroundColor: ThemeHex.parse(inactiveBackgroundColor),
                     activeTextColor: ThemeHex.parse(activeTextColor),
                     inactiveTextColor: ThemeHex.parse(inactiveTextColor),
                     cornerRadius: cornerRadius)
    }
}

public extension PromotionThemeDisplay.TabUnderlineValues {
    func toToken() -> PRMTabUnderlineToken {
        PRMTabUnderlineToken(indicatorColor: ThemeHex.parse(indicatorColor),
                          activeTextColor: ThemeHex.parse(activeTextColor),
                          inactiveTextColor: ThemeHex.parse(inactiveTextColor),
                          backgroundColor: ThemeHex.parse(backgroundColor))
    }
}

public extension PromotionThemeDisplay.DiscountBadgeValues {
    func toToken() -> PRMDiscountBadgeToken {
        PRMDiscountBadgeToken(availableTextColor: ThemeHex.parse(availableTextColor),
                           unavailableTextColor: ThemeHex.parse(unavailableTextColor),
                           availableBackgroundColor: ThemeHex.parse(availableBackgroundColor),
                           unavailableBackgroundColor: ThemeHex.parse(unavailableBackgroundColor),
                           actionTextColor: ThemeHex.parse(actionTextColor))
    }
}
