//
//  DemoThemeSource.swift
//  PromotionSDKDemo
//
//  Hai cách host lấy một PromotionSDKTheme — đối ứng `DemoThemeSource.kt` bên Android.
//
//  - fromJsonFile(): đọc `promotion_theme.json` trong bundle (CÙNG MỘT FILE với bản ở
//    androidApp/src/main/assets) rồi parse bằng PromotionThemeJson. Trả lời cho câu hỏi "SDK dùng
//    file JSON được không": được, một file dùng chung hai nền tảng.
//  - fromObject(): dựng token bằng code, không qua JSON.
//
//  Cả hai cho cùng bộ màu (teal #2CA196) để thấy hai đường đi ra cùng kết quả.
//
//  File này import PRM nên CỐ TÌNH GIỮ NHỎ — xem ghi chú ở ThemePreviewViewController.
//

import UIKit
import PRM

enum DemoThemeSource {

    static let resourceName = "promotion_theme"

    /// `nil` nếu thiếu file hoặc JSON hỏng — host tự quyết định giữ theme cũ.
    static func fromJsonFile() -> PromotionSDKTheme? {
        guard let url = Bundle.main.url(forResource: resourceName, withExtension: "json"),
              let json = try? String(contentsOf: url, encoding: .utf8) else { return nil }
        return PromotionThemeJson.fromJson(json)
    }

    /// Cùng bộ màu với `fromJsonFile()`, nhưng viết thẳng bằng token — không đụng JSON.
    static func fromObject() -> PromotionSDKTheme {
        let teal = UIColor(red: 0x2C / 255, green: 0xA1 / 255, blue: 0x96 / 255, alpha: 1)
        let tealLight = UIColor(red: 0xEA / 255, green: 0xF6 / 255, blue: 0xF4 / 255, alpha: 1)
        let white = UIColor.white
        let grey = UIColor(white: 0x7A / 255, alpha: 1)
        let greyLight = UIColor(white: 0xF4 / 255, alpha: 1)
        let greyMid = UIColor(white: 0xA7 / 255, alpha: 1)

        return PromotionSDKTheme(
            buttonToken: ButtonToken(
                backgroundColor: teal,
                textColor: white,
                shadowColor: teal.withAlphaComponent(0.2),
                cornerRadius: 12
            ),
            searchBarToken: SearchBarToken(
                borderColor: teal,
                hintTextColor: grey,
                textColor: UIColor(white: 0x22 / 255, alpha: 1),
                iconColor: teal,
                cornerRadius: 10
            ),
            listItemToken: ListItemToken(
                linkTextColor: teal,
                usedBadgeTextColor: grey,
                usedBadgeBackgroundColor: greyLight,
                radioButtonStrokeColor: greyMid,
                radioButtonSelectedStrokeColor: teal
            ),
            tabChipToken: TabChipToken(
                activeBackgroundColor: teal,
                inactiveBackgroundColor: tealLight,
                activeTextColor: white,
                inactiveTextColor: UIColor(white: 0x4E / 255, alpha: 1),
                cornerRadius: 16
            ),
            tabUnderlineToken: TabUnderlineToken(
                indicatorColor: teal,
                activeTextColor: teal,
                inactiveTextColor: grey,
                backgroundColor: white
            ),
            discountBadgeToken: DiscountBadgeToken(
                availableTextColor: teal,
                unavailableTextColor: greyMid,
                availableBackgroundColor: tealLight,
                unavailableBackgroundColor: greyLight,
                actionTextColor: teal
            )
        )
    }
}
