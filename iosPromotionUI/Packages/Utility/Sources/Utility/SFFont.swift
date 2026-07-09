//
//  SFFont.swift
//  CoreUIKit
//
//  Created by VSS on 15/01/2024.
//  Copyright © 2024 ViettelPay App Team. All rights reserved.
//

import UIKit

public struct Fonts {
    public static func regular(_ pointSize: CGFloat) -> UIFont {
        return UIFont(name: "SFProText-Regular", size: pointSize) ?? UIFont.systemFont(ofSize: pointSize)
    }
    public static func medium(_ pointSize: CGFloat) -> UIFont {
        guard let font = UIFont(name: "SFProText-Medium", size: pointSize) else {
            return  UIFont.systemFont(ofSize: pointSize, weight: .medium)
        }
        return font
    }
    public static func bold(_ pointSize: CGFloat) -> UIFont {
        return UIFont(name: "SFProText-Bold", size: pointSize) ?? UIFont.boldSystemFont(ofSize: pointSize)
    }
    public static func italic(_ pointSize: CGFloat) -> UIFont {
        return UIFont(name: "SFProText-Italic", size: pointSize) ?? UIFont.italicSystemFont(ofSize: pointSize)
    }
    public static func semibold(_ pointSize: CGFloat) -> UIFont {
        guard let font = UIFont(name: "SFProText-Semibold", size: pointSize) else {
            return  UIFont.systemFont(ofSize: pointSize, weight: .semibold)
        }
        return font
    }

    public static func light(_ pointSize: CGFloat) -> UIFont {
        return UIFont(name: "SFProText-Light", size: pointSize) ?? UIFont.systemFont(ofSize: pointSize, weight: .light)
    }
}

typealias MainFont = Font.HelveticaNeue

enum Font {
    enum HelveticaNeue: String {
        case ultraLightItalic = "UltraLightItalic"
        case medium = "Medium"
        case mediumItalic = "MediumItalic"
        case ultraLight = "UltraLight"
        case italic = "Italic"
        case light = "Light"
        case thinItalic = "ThinItalic"
        case lightItalic = "LightItalic"
        case bold = "Bold"
        case thin = "Thin"
        case condensedBlack = "CondensedBlack"
        case condensedBold = "CondensedBold"
        case boldItalic = "BoldItalic"
        case regular = "Regular"

        func with(size: CGFloat) -> UIFont {
            return UIFont(name: "HelveticaNeue-\(rawValue)", size: size) ?? UIFont.systemFont(ofSize: size)
        }
    }
}

public struct FontsProDisplay {
    public static func regular(_ pointSize: CGFloat) -> UIFont {
        return UIFont(name: "SFProDisplay-Regular", size: pointSize) ?? UIFont.systemFont(ofSize: pointSize)
    }
    public static func medium(_ pointSize: CGFloat) -> UIFont {
        guard let font = UIFont(name: "SFProDisplay-Medium", size: pointSize) else {
            return  UIFont.systemFont(ofSize: pointSize, weight: .medium)
        }
        return font
    }
    public static func bold(_ pointSize: CGFloat) -> UIFont {
        return UIFont(name: "SFProDisplay-Bold", size: pointSize) ?? UIFont.boldSystemFont(ofSize: pointSize)
    }

    public static func semibold(_ pointSize: CGFloat) -> UIFont {
        guard let font = UIFont(name: "SFProDisplay-Semibold", size: pointSize) else {
            return  UIFont.systemFont(ofSize: pointSize, weight: .semibold)
        }
        return font
    }

}

