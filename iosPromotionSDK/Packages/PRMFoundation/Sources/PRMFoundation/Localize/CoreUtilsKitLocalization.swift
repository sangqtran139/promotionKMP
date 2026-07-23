//
//  CoreUtilsKitLocalization.swift
//  CoreUtilsKit
//
//  Created by Natariannn on 11/11/20.
//  Copyright © 2020 ViettelPay App Team. All rights reserved.
//

import Foundation

private class BundleFinder {}

public enum CoreUtilsKitLocalization: String {
    case currency_unit
    case currency_thousand
    case currency_million
    case currency_billion
    case point_unit
    case required_touch_id
    case required_face_id
    
    public var localized: String {
        let bundle: Bundle
        #if SWIFT_PACKAGE
        bundle = Bundle.module
        #else
        bundle = Bundle(for: BundleFinder.self)
        #endif
        return NSLocalizedString(rawValue, tableName: "CoreUtilsKit", bundle: bundle, comment: "")
    }
}
