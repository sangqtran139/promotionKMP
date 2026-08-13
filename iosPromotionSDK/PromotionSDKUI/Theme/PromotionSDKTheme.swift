//
//  PromotionSDKTheme.swift
//  PromotionSDK
//
//  Cấu hình theme công khai. Chỉ UIColor/CGFloat để không kéo PRMDesignKit vào module interface.
//  Đối ứng `PromotionSDKTheme.kt`. Sáu token ở `Token/`, serialize ở `PromotionThemeJson.swift`,
//  codec màu ở `ThemeHex.swift` — chia thư mục giống Android.
//

import UIKit

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
