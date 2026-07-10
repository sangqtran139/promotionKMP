//
//  PromotionThemeDefaults.swift
//  PromotionSDK
//
//  Giá trị mặc định **thật** của SDK, đọc từ nhánh `nil` của các component CoreUI/PromotionUI.
//  Đối ứng `PromotionThemeDefaults.kt` bên Android (nơi giá trị đến từ `R.color.*`).
//
//  Dùng cho màn cấu hình theme của host: token nào chưa set thì hiện giá trị SDK đang dùng.
//  Trước đây `ThemePlaygroundSupport.swift` của app demo phải **đoán** ("Màu default xấp xỉ SDK").
//
//  ⚠️ Giá trị ở đây **không** trùng Android ở mọi field — hai nền tảng vốn có default khác nhau
//  (badge giảm giá: teal bên Android, đỏ san hô bên iOS). Xem bảng trong `docs/Theming.md`.
//  File này **báo cáo trung thực** những gì iOS đang render, không phải sao chép Android.
//

import UIKit
@_implementationOnly import CoreUI

public enum PromotionThemeDefaults {

    /// Nút chính (`VDSButton`, size `.large`, không outline).
    ///
    /// - `backgroundColor`: SDK vẽ **gradient** `tokenViettelPayRed80 → tokenViettelPayRed100`;
    ///   token chỉ giữ màu cuối, đúng như Android giữ `tokenRainbowRedEnd`.
    /// - `cornerRadius`: SDK dùng `height / 2` (viên thuốc). Với `largeHeight = 48` thì bằng 24.
    /// - `shadowColor`: `Shadows.tokenShadowButtonLarge` là đỏ + opacity 0.25 → `#40EE0033`.
    public static var button: ButtonToken {
        ButtonToken(
            backgroundColor: Colors.tokenViettelPayRed100,
            textColor: Colors.tokenWhite,
            shadowColor: Colors.tokenViettelPayRed100.withAlphaComponent(0.25),
            cornerRadius: 24
        )
    }

    /// Ô tìm kiếm (`VDSSearchTextField`, không phải kiểu navigation).
    /// `iconColor` không có default thật — SDK render ảnh gốc, không tint. Lấy `tokenDark40` cho
    /// khớp Android và để màn preview có gì đó hiển thị.
    public static var searchBar: SearchBarToken {
        SearchBarToken(
            borderColor: Colors.tokenDark10,
            hintTextColor: Colors.tokenDark40,
            textColor: Colors.tokenDark100,
            iconColor: Colors.tokenDark40,
            cornerRadius: Sizing.tokenSizing08
        )
    }

    /// Item voucher (`PromotionCardView`).
    /// `linkTextColor` và hai màu radio **không có default trong code** — SDK giữ màu của XIB và ảnh
    /// asset gốc. Giá trị dưới đây là những gì asset đang thể hiện, để preview không trống.
    public static var listItem: ListItemToken {
        ListItemToken(
            linkTextColor: Colors.tokenViettelPayRed100,
            usedBadgeTextColor: Colors.tokenDark60,
            usedBadgeBackgroundColor: Colors.tokenDark05,
            radioButtonStrokeColor: Colors.tokenDark40,
            radioButtonSelectedStrokeColor: Colors.tokenViettelPayRed100
        )
    }

    /// Tab dạng chip (`PromotionTabView`).
    public static var tabChip: TabChipToken {
        TabChipToken(
            activeBackgroundColor: Colors.tokenDark80,
            inactiveBackgroundColor: Colors.tokenDark05,
            activeTextColor: Colors.tokenWhite,
            inactiveTextColor: Colors.tokenDark60,
            cornerRadius: 8
        )
    }

    /// Tab gạch chân (`UnderlinedSegmentControlItem`).
    /// `backgroundColor` không có default — thanh tab chỉ đổi nền khi host set token.
    public static var tabUnderline: TabUnderlineToken {
        TabUnderlineToken(
            indicatorColor: Colors.tokenRed100,
            activeTextColor: Colors.tokenDark100,
            inactiveTextColor: Colors.tokenDark60,
            backgroundColor: nil
        )
    }

    /// Badge giảm giá trên widget (`PRMEndowView`).
    public static var discountBadge: DiscountBadgeToken {
        DiscountBadgeToken(
            availableTextColor: Colors.tokenRed100,
            unavailableTextColor: Colors.tokenDark60,
            availableBackgroundColor: Colors.tokenRed100.withAlphaComponent(0.08),
            unavailableBackgroundColor: Colors.tokenDark05,
            actionTextColor: Colors.tokenRed100
        )
    }

    /// Theme mặc định đầy đủ. Đối ứng `PromotionThemeDefaults.defaultConfig(context)` bên Android.
    public static var theme: PromotionSDKTheme {
        PromotionSDKTheme(
            buttonToken: button,
            searchBarToken: searchBar,
            listItemToken: listItem,
            tabChipToken: tabChip,
            tabUnderlineToken: tabUnderline,
            discountBadgeToken: discountBadge
        )
    }

    /// `#AARRGGBB`, rút gọn `#RRGGBB` khi màu đục. Đối ứng `PromotionThemeDefaults.colorToHex`.
    public static func colorToHex(_ color: UIColor) -> String { color.promotionHexString }
}
