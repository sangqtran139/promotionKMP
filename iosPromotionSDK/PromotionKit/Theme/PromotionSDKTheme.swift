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
    public var buttonToken: PRMButtonToken?
    public var searchBarToken: PRMSearchBarToken?
    public var listItemToken: PRMListItemToken?
    public var tabChipToken: PRMTabChipToken?
    public var tabUnderlineToken: PRMTabUnderlineToken?
    public var discountBadgeToken: PRMDiscountBadgeToken?

    public init(buttonToken: PRMButtonToken? = nil,
                searchBarToken: PRMSearchBarToken? = nil,
                listItemToken: PRMListItemToken? = nil,
                tabChipToken: PRMTabChipToken? = nil,
                tabUnderlineToken: PRMTabUnderlineToken? = nil,
                discountBadgeToken: PRMDiscountBadgeToken? = nil) {
        self.buttonToken = buttonToken
        self.searchBarToken = searchBarToken
        self.listItemToken = listItemToken
        self.tabChipToken = tabChipToken
        self.tabUnderlineToken = tabUnderlineToken
        self.discountBadgeToken = discountBadgeToken
    }
}
