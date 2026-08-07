//
//  PromotionThemeDefaults.swift
//  PromotionSDK
//
//  Giá trị mặc định của SDK cho màn cấu hình theme của host: token nào chưa set thì hiện giá trị này.
//  Đối ứng `PromotionThemeDefaults.kt`.
//
//  **Màu khớp Android.** Dùng token PRMDesignKit có giá trị **bằng** `R.color.*` bên Android:
//
//    tokenViettelPayRed100 = #EE0033   tokenWhite   = #FFFFFF   tokenDark02 = #FBFBFB
//    tokenDark10  = #E9E9E9  tokenDark40 = #A7A7A7   tokenDark100 = #222222
//    tokenDark05  = #F4F4F4  tokenDark80 = #4E4E4E   tokenDark60  = #7A7A7A
//    tokenBlack   = #000000  tokenPineBlue100 = #2CA196   tokenPineBlue10 = #EAF6F4
//
//  Dùng token thay hex thô: nếu design system đổi token, default đi theo. `cornerRadius` giữ pt cố
//  định (Android dùng sdp co giãn theo màn, không có một giá trị duy nhất).
//

import UIKit
@_implementationOnly import PRMDesignKit

enum PromotionThemeDefaults {

    /// Nút chính. `backgroundColor` = màu cuối gradient; `cornerRadius` viên thuốc (`height/2` = 24 ở
    /// size lớn); `shadowColor` đỏ + opacity 25% = `#40EE0033` (khớp `tokenShadowsButtonColor`).
    static var button: ButtonToken {
        ButtonToken(
            backgroundColor: Colors.tokenViettelPayRed100,
            textColor: Colors.tokenWhite,
            shadowColor: Colors.tokenViettelPayRed100.withAlphaComponent(0.25),
            cornerRadius: 24
        )
    }

    /// Ô tìm kiếm. `iconColor` không có default thật (SDK render ảnh gốc); lấy `tokenDark40` cho khớp
    /// Android và để preview có gì đó hiển thị.
    static var searchBar: SearchBarToken {
        SearchBarToken(
            borderColor: Colors.tokenDark10,
            hintTextColor: Colors.tokenDark40,
            textColor: Colors.tokenDark100,
            iconColor: Colors.tokenDark40,
            cornerRadius: 8
        )
    }

    /// Item voucher. `linkTextColor` và hai màu radio bên iOS lấy từ ảnh asset; giá trị dưới đây khớp
    /// Android.
    static var listItem: ListItemToken {
        ListItemToken(
            linkTextColor: Colors.tokenViettelPayRed100,
            usedBadgeTextColor: Colors.tokenDark100,
            usedBadgeBackgroundColor: Colors.tokenDark05,
            radioButtonStrokeColor: Colors.tokenDark40,
            radioButtonSelectedStrokeColor: Colors.tokenViettelPayRed100
        )
    }

    /// Tab dạng chip.
    static var tabChip: TabChipToken {
        TabChipToken(
            activeBackgroundColor: Colors.tokenDark80,
            inactiveBackgroundColor: Colors.tokenDark05,
            activeTextColor: Colors.tokenWhite,
            inactiveTextColor: Colors.tokenDark60,
            cornerRadius: 7
        )
    }

    /// Tab gạch chân.
    static var tabUnderline: TabUnderlineToken {
        TabUnderlineToken(
            indicatorColor: Colors.tokenViettelPayRed100,
            activeTextColor: Colors.tokenBlack,
            inactiveTextColor: Colors.tokenDark60,
            backgroundColor: Colors.tokenDark02
        )
    }

    /// Badge giảm giá.
    static var discountBadge: DiscountBadgeToken {
        DiscountBadgeToken(
            availableTextColor: Colors.tokenPineBlue100,
            unavailableTextColor: Colors.tokenDark60,
            availableBackgroundColor: Colors.tokenPineBlue10,
            unavailableBackgroundColor: Colors.tokenDark05,
            actionTextColor: Colors.tokenViettelPayRed100
        )
    }

    /// Theme mặc định đầy đủ. Đối ứng `PromotionThemeDefaults.theme(context)` bên Android.
    static var theme: PromotionSDKTheme {
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
    static func colorToHex(_ color: UIColor) -> String { ThemeHex.format(color) ?? "#000000" }
}
